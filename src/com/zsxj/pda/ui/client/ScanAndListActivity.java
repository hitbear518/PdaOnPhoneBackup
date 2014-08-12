package com.zsxj.pda.ui.client;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zsxj.pda.provider.ProviderContract.PdInfo;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.ConstParams.ScanType;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.util.ViewUtils;
import com.zsxj.pda.wdt.CashSaleSpec;
import com.zsxj.pda.wdt.MergedSpec;
import com.zsxj.pda.wdt.Spec;
import com.zsxj.pda.wdt.TradeInfo;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTQuery;
import com.zsxj.pda.wdt.WDTQuery.QueryCallBack;
import com.zsxj.pdaonphone.R;

public class ScanAndListActivity extends Activity implements QueryCallBack {
	
	private TextView mPromptTv;
	private RelativeLayout mProgressLayout;
	private ListView mSpecsLv;
	
	
	private int mScanType;
	private String mBarcode;
	private int mPdId;
	
	private Spec[] mSpecs;
	private TradeInfo mTradeInfo;
	private Cursor mCursor;
	private MergedSpec[] mMergedSpecs;
	private int mWhichSpec;
	private MyListAdapter mAdapter;
	
	private CashSaleSpec[] mCashSaleSpecs;
	
	private String mSearchTerm;
	
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			mProgressLayout.setVisibility(View.GONE);
			
			switch (msg.what) {
			case HandlerCases.NO_CONN:
				mPromptTv.setVisibility(View.VISIBLE);
				Util.toast(getApplicationContext(), R.string.no_conn);
				break;
			case HandlerCases.PREPARE_QUERY_ERROR:
				mPromptTv.setVisibility(View.VISIBLE);
				Util.toast(getApplicationContext(), R.string.query_specs_failed);
				break;
			case HandlerCases.QUERY_FAIL:
				Util.toast(getApplicationContext(), R.string.query_specs_failed);
				break;
			case HandlerCases.OUT_EXAMINE_MUTI_ROWS:
				Util.toast(getApplicationContext(), "物流单号对应系统内两个或以上订单,请扫描系统内订单编号");
				mPromptTv.setVisibility(View.VISIBLE);
				break;
			case HandlerCases.EMPTY_RESULT:
				mPromptTv.setVisibility(View.VISIBLE);
				mSpecsLv.setVisibility(View.GONE);
				if (ScanType.TYPE_OUT_EXAMINE_GOODS == mScanType || 
					ScanType.TYPE_PICK_GOODS == mScanType) {
					Util.toast(getApplicationContext(), "无此订单或订单状态异常");
				} else {
					Util.toast(getApplicationContext(), R.string.query_no_result_promt);
				}
				break;
			case HandlerCases.QUERY_SUCCESS:
				dealData();
				break;
			case HandlerCases.SCAN_RESULT:
				tryQuery();
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scan_and_list_activity);
		
		mScanType = getIntent().getIntExtra(Extras.SCAN_TYPE, -1);
		
		mPromptTv = (TextView) findViewById(R.id.promt_tv);
		mProgressLayout = (RelativeLayout) findViewById(R.id.progress_layout);
		mSpecsLv = (ListView) findViewById(R.id.specs_lv);
		
		final ActionBar actionBar = getActionBar();
		switch (mScanType) {
		case ScanType.TYPE_PD:
			actionBar.setTitle(R.string.pd);
			break;
		case ScanType.TYPE_FAST_PD:
			actionBar.setTitle(R.string.fast_pd);
			mPromptTv.setText(R.string.scan_goods_prompt);
			break;
		case ScanType.TYPE_QUERY_SPECS:
			actionBar.setTitle(R.string.query_goods);
			mPromptTv.setText(R.string.scan_goods_prompt);
			break;
		case ScanType.TYPE_FAST_IN_EXAMINE_GOODS:
			actionBar.setTitle(R.string.fast_in_examine_goods);
			mPromptTv.setText(R.string.scan_goods_prompt); 
			break;
		case ScanType.TYPE_OUT_EXAMINE_GOODS:
			actionBar.setTitle(R.string.out_examine_goods);
			mPromptTv.setText(R.string.scan_trade_or_logistics_prompt);
			break;
		case ScanType.TYPE_PICK_GOODS:
			actionBar.setTitle(R.string.pick_goods);
			mPromptTv.setText(R.string.scan_trade_or_logistics_prompt);
			break;
		case ScanType.TYPE_CASH_SALE:
		case ScanType.TYPE_CASH_SALE_BY_TERM:
			actionBar.setTitle(R.string.cash_sale);
			mPromptTv.setText(R.string.scan_goods_prompt);
			break;
		default:
			break;
		}
		mPromptTv.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				mBarcode = "JY201405230001";
				mHandler.sendEmptyMessage(HandlerCases.SCAN_RESULT);
			}
		});
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mAdapter = new MyListAdapter(this); 
		mSpecsLv.setAdapter(mAdapter);
		mSpecsLv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mWhichSpec = position;
				if (ScanType.TYPE_CASH_SALE == mScanType || mScanType == ScanType.TYPE_CASH_SALE_BY_TERM) 
					showCashSaleSpec(mCashSaleSpecs[mWhichSpec]);
				else
					showMergedSpec(mMergedSpecs[mWhichSpec]);
			}
		});
		
		mBarcode = getIntent().getStringExtra(Extras.BARCODE);
		mSearchTerm = getIntent().getStringExtra(Extras.SEARCH_TERM);
		mPdId = getIntent().getIntExtra(Extras.PD_ID, -1);
		mWhichSpec = getIntent().getIntExtra(Extras.WHICH_SPEC, -1);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(
			R.menu.scan_and_list_actions, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.search_action:
			ViewUtils.showSearchDialog(this, Intent.FLAG_ACTIVITY_CLEAR_TOP);
			break;
		default:
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == RESULT_OK) {
			mBarcode = data.getStringExtra("result");
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d("Scan Process", "ScanAndListActivity, onKeyDown");
		if (KeyEvent.KEYCODE_F2 == keyCode) {
			return true;
		} else if (KeyEvent.KEYCODE_BACK == keyCode) {
			onBackPressed();
		}
		return false;
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_F2 == keyCode) {
			return true;
		}
		return false;
	}
	
	@Override
	public void onQuerySuccess(Object qr) {
		if (qr != null) {
			if (Spec[].class.isInstance(qr)) {
				mSpecs = (Spec[]) qr;
			} else if (TradeInfo.class.isInstance(qr)) {
				mTradeInfo = (TradeInfo) qr;
			} else if (CashSaleSpec[].class.isInstance(qr)) {
				mCashSaleSpecs = (CashSaleSpec[]) qr;
			} else {
				throw new ClassCastException("Wrong query result type");
			}
			
			mHandler.sendEmptyMessage(HandlerCases.QUERY_SUCCESS);	
		} else {
			mHandler.sendEmptyMessage(HandlerCases.EMPTY_RESULT);
		}
	}

	@Override
	public void onQueryFail(int type, WDTException wdtEx) {
		if (null == wdtEx) {
			mHandler.sendEmptyMessage(type);
			return;
		}
		switch (wdtEx.getStatus()) {
		case 1064:
			mHandler.sendEmptyMessage(HandlerCases.QUERY_FAIL);
			break;
		case 9999:
			mHandler.sendEmptyMessage(HandlerCases.OUT_EXAMINE_MUTI_ROWS);
		default:
			break;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		tryQuery();
	}

	private void tryQuery() {
		if (!TextUtils.isEmpty(mBarcode) || !TextUtils.isEmpty(mSearchTerm)) {
			Log.i("In try Query", "Before query");
			mProgressLayout.setVisibility(View.VISIBLE);
			mPromptTv.setVisibility(View.GONE);
			switch (mScanType) {
			case ScanType.TYPE_PD:
				queryPdSpecsLocally();
				break;
			case ScanType.TYPE_FAST_PD:
			case ScanType.TYPE_QUERY_SPECS:
				WDTQuery.getinstance().querySpecs(this, this, 
						Globals.getWarehouseId(getApplicationContext()), mBarcode);
				break;
			case ScanType.TYPE_FAST_IN_EXAMINE_GOODS:
				WDTQuery.getinstance().queryInExamSpecs(this, this, mBarcode);
				break;
			case ScanType.TYPE_OUT_EXAMINE_GOODS:
			case ScanType.TYPE_PICK_GOODS:
				WDTQuery.getinstance().queryTradeInfo(this, this, mBarcode);
				break;
			case ScanType.TYPE_CASH_SALE:
				WDTQuery.getinstance().queryCashSaleSpecs(this, this, Globals.getModuleUseWarehouseId(), mBarcode);
				break;
			case ScanType.TYPE_CASH_SALE_BY_TERM:
				WDTQuery.getinstance().queryCashSaleSpecsByTerm(this, Globals.getModuleUseWarehouseId(), mSearchTerm);
				break;
			default:
				break;
			}
		}
	}
	
	private void queryPdSpecsLocally() {
		String selection = PdInfo.COLUMN_NAME_PD_ID + "=? AND " 
				+ PdInfo.COLUMN_NAME_BARCODE + "=?";
		String[] selectionArgs = {
				mPdId + "",
				mBarcode
		};
		mCursor = getContentResolver().query(PdInfo.CONTENT_URI, null, 
			selection, selectionArgs, null);
		if (0 == mCursor.getCount()) 
			mHandler.sendEmptyMessage(HandlerCases.EMPTY_RESULT);
		else
			mHandler.sendEmptyMessage(HandlerCases.QUERY_SUCCESS);
	}

	private void dealData() {
		if (ScanType.TYPE_PD == mScanType) {
			mMergedSpecs = MergedSpec.mergePdSpecs(mCursor);
		} else if (ScanType.TYPE_OUT_EXAMINE_GOODS == mScanType) {
			goToOutExamine();
			return;
		} else if (ScanType.TYPE_PICK_GOODS == mScanType) {
			goToPickGoods();
			return;
		} else if (ScanType.TYPE_FAST_IN_EXAMINE_GOODS == mScanType) {
			mMergedSpecs = MergedSpec.noMerge(mSpecs);
		} else if (ScanType.TYPE_CASH_SALE == mScanType || mScanType == ScanType.TYPE_CASH_SALE_BY_TERM) {
			dealCashSaleSpec();
			return;
		} else {
			mMergedSpecs = MergedSpec.mergeSpecs(mSpecs);
		}
			
		mAdapter.notifyDataSetChanged();
		
		if (mWhichSpec != -1) {
			showMergedSpec(mMergedSpecs[mWhichSpec]);
			return;
		}
		
		if (mMergedSpecs.length > 1) {
			mSpecsLv.setVisibility(View.VISIBLE);
		} else {
			mWhichSpec = 0;
			showMergedSpec(mMergedSpecs[mWhichSpec]);
			if (ScanType.TYPE_FAST_IN_EXAMINE_GOODS == mScanType) {
				finish();
			}
		}
	}
	
	private void goToOutExamine() {
		mBarcode = null;
		mPromptTv.setVisibility(View.VISIBLE);
		if (mTradeInfo.tradeStatus != 5) {
			switch (mTradeInfo.tradeStatus) {
			case 2:
				Util.toast(this, "订单尚未通过审核");
				break;
			case 4:
				Util.toast(this, "订单尚未通过财务审核");
				break;
			case 8:
			case 11:
				Util.toast(this, "订单已发货");
				break;
			default:
				Util.toast(this, "订单状态有误 ");
				break;
			}
			return;
		}
		if (mTradeInfo.bStockOut > 0) {
			Util.toast(this, "订单已出库");
			return;
		}
		if (mTradeInfo.bFreezed > 0) {
			Util.toast(this, "订单已冻结");
			return;
		}
		if (mTradeInfo.bStockOut > 0) {
			Util.toast(this, "订单已退款或用户已申请退款");
			return;
		}
		Intent intent = new Intent(this, OutExamineGoodsActivity.class);
		intent.putExtra(Extras.TRADE_ID, mTradeInfo.tradeId);
		intent.putExtra(Extras.PICKER_ID, mTradeInfo.pickerId);
		intent.putExtra(Extras.POST_ID, mTradeInfo.postId);
		intent.putExtra(Extras.WAREHOUSE_ID, mTradeInfo.warehouseId);
		
		startActivity(intent);
	}
	
	private void goToPickGoods() {
		mBarcode = null;
		mPromptTv.setVisibility(View.VISIBLE);
		if (mTradeInfo.tradeStatus != 5) {
			switch (mTradeInfo.tradeStatus) {
			case 2:
				Util.toast(this, "订单尚未通过审核");
				break;
			case 4:
				Util.toast(this, "订单尚未通过财务审核");
				break;
			case 8:
			case 11:
				Util.toast(this, "订单已发货");
				break;
			default:
				Util.toast(this, "订单状态有误 ");
				break;
			}
			return;
		}
		if (mTradeInfo.bStockOut > 0) {
			Util.toast(this, "订单已出库");
			return;
		}
		if (mTradeInfo.bFreezed > 0) {
			Util.toast(this, "订单已冻结");
			return;
		}
		if (mTradeInfo.bStockOut > 0) {
			Util.toast(this, "订单已退款或用户已申请退款");
			return;
		}
		
		Intent intent = new Intent(this, PickGoodsActivity.class);
		intent.putExtra(Extras.TRADE_ID, mTradeInfo.tradeId);
		intent.putExtra(Extras.PICKER_ID, mTradeInfo.pickerId);
		intent.putExtra(Extras.WAREHOUSE_ID, mTradeInfo.warehouseId);
		
		startActivity(intent);
	}
	
	private void clearData() {
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
		}
		
		if (ScanType.TYPE_CASH_SALE == mScanType || mScanType == ScanType.TYPE_CASH_SALE_BY_TERM) {
			if (1 == mCashSaleSpecs.length) {
				mBarcode = null;
				mSearchTerm = null;
			}
			mCashSaleSpecs = null;
		} else {
			mSpecs = null;
			if (1 == mMergedSpecs.length) {
				// Only keep barcode for specs list
				mBarcode = null;
			}
			mMergedSpecs = null;
		}
		mWhichSpec = -1;
	}
	
	private void showMergedSpec(final MergedSpec mergedSpec) {
		Intent showIntent = new Intent();
		switch (mScanType) {
		case ScanType.TYPE_PD:
			showIntent.setClass(this, PdActivity.class);
			showIntent.putExtra(Extras.PD_ID, mPdId);
			showIntent.putExtra(Extras.RECORD_IDS, mMergedSpecs[mWhichSpec].recIds);
			showIntent.putExtra(Extras.STOCKS_OLD, mMergedSpecs[mWhichSpec].stockOlds);
			showIntent.putExtra(Extras.STOCKS_PD, mMergedSpecs[mWhichSpec].stockPds);
			break;
		case ScanType.TYPE_FAST_PD:
			showIntent.setClass(this, FastPdActivity.class);
			showIntent.putExtra(Extras.STOCKS, mMergedSpecs[mWhichSpec].stocks);
			break;
		case ScanType.TYPE_QUERY_SPECS:
			showIntent.setClass(this, QuerySpecsActivity.class);
			showIntent.putExtra(Extras.STOCKS, mMergedSpecs[mWhichSpec].stocks);
			break;
		case ScanType.TYPE_FAST_IN_EXAMINE_GOODS:
			showIntent.setClass(this, FastInExamineGoodsActivity.class);
			showIntent.putExtra(Extras.STOCKS, mMergedSpecs[mWhichSpec].stocks);
			showIntent.putExtra(Extras.SCAN_TYPE, mScanType);
			break;
		default:
			break;
		}
		showIntent.putExtra(Extras.SPEC_ID, mMergedSpecs[mWhichSpec].specId);
		showIntent.putExtra(Extras.SPEC_BARCODE, mMergedSpecs[mWhichSpec].specBarcode);
		showIntent.putExtra(Extras.GOODS_NUM, mMergedSpecs[mWhichSpec].goodsNum);
		showIntent.putExtra(Extras.GOODS_NAME, mMergedSpecs[mWhichSpec].goodsName);
		showIntent.putExtra(Extras.BARCODE, mBarcode);
		showIntent.putExtra(Extras.SPEC_CODE, mMergedSpecs[mWhichSpec].specCode);
		showIntent.putExtra(Extras.SPEC_NAME, mMergedSpecs[mWhichSpec].specName);
		showIntent.putExtra(Extras.POSITION_IDS, mMergedSpecs[mWhichSpec].positionIds);
		showIntent.putExtra(Extras.POSITION_NAMES, mMergedSpecs[mWhichSpec].positionNames);
		showIntent.putExtra(Extras.WHICH_SPEC, mWhichSpec);
		startActivity(showIntent);
		if (ScanType.TYPE_FAST_IN_EXAMINE_GOODS != mScanType) {
			mPromptTv.setVisibility(View.VISIBLE);
			mSpecsLv.setVisibility(View.GONE);
			mProgressLayout.setVisibility(View.GONE);
		}
		if (ScanType.TYPE_PD == mScanType && 1 == mMergedSpecs.length)
			finish();
		clearData();
	}
	
	private void showCashSaleSpec(CashSaleSpec spec) {
		Intent showIntent = new Intent(this, CashSaleGoodsActivity.class);
		showIntent.putExtra(Extras.SPEC_ID, spec.specId);
		showIntent.putExtra(Extras.SPEC_BARCODE, spec.specBarcode);
		showIntent.putExtra(Extras.GOODS_NUM, spec.goodsNum);
		showIntent.putExtra(Extras.GOODS_NAME, spec.goodsName);
		showIntent.putExtra(Extras.SPEC_CODE, spec.specCode);
		showIntent.putExtra(Extras.SPEC_NAME, spec.specName);
		showIntent.putExtra(Extras.RETAIL_PRICE, spec.retailPrice);
		showIntent.putExtra(Extras.WHOLESALE_PRICE, spec.wholesalePrice);
		showIntent.putExtra(Extras.MEMBER_PRICE, spec.memberPrice);
		showIntent.putExtra(Extras.PURCHASE_PRICE, spec.purchasePrice);
		showIntent.putExtra(Extras.PRICE_1, spec.price1);
		showIntent.putExtra(Extras.PRICE_2, spec.price2);
		showIntent.putExtra(Extras.PRICE_3, spec.price3);
		showIntent.putExtra(Extras.CASH_SALE_STOCK, spec.stock);
		if (mBarcode == null) {
			mBarcode = "search by term: " + mSearchTerm;
		}
		showIntent.putExtra(Extras.BARCODE, mBarcode);
		
		clearData();
		startActivity(showIntent);
	}
	
	private void dealCashSaleSpec() {
		
		mAdapter.notifyDataSetChanged();
		
		if (mWhichSpec != -1) {
			showCashSaleSpec(mCashSaleSpecs[mWhichSpec]);
			return;
		}
		
		if (mCashSaleSpecs.length > 1) {
			mSpecsLv.setVisibility(View.VISIBLE);
		} else {
			mWhichSpec = 0;
			showCashSaleSpec(mCashSaleSpecs[mWhichSpec]);
			finish();
		}
	}

	private class MyListAdapter extends BaseAdapter {
		
		private final LayoutInflater mInflater;

		public MyListAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}
		
		@Override
		public int getCount() {
			if (ScanType.TYPE_CASH_SALE == mScanType || mScanType == ScanType.TYPE_CASH_SALE_BY_TERM)
				return null == mCashSaleSpecs ? 0 : mCashSaleSpecs.length;
			else
				return null ==  mMergedSpecs ? 0 : mMergedSpecs.length;  
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {			
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.specs_list_item, parent, false);
				holder = new ViewHolder();
				holder.goodsName = (TextView) convertView.findViewById(R.id.goods_name_tv);
				holder.specCode = (TextView) convertView.findViewById(R.id.spec_code_tv);
				holder.specName = (TextView) convertView.findViewById(R.id.spec_name_tv);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			if (ScanType.TYPE_CASH_SALE == mScanType || mScanType == ScanType.TYPE_CASH_SALE_BY_TERM) {
                holder.goodsName.setText(mCashSaleSpecs[position].goodsName);
                holder.specCode.setText(mCashSaleSpecs[position].specCode);
                holder.specName.setText(mCashSaleSpecs[position].specName);
			} else {
                holder.goodsName.setText(mMergedSpecs[position].goodsName);
                holder.specCode.setText(mMergedSpecs[position].specCode);
                holder.specName.setText(mMergedSpecs[position].specName);
			}
			
			return convertView;
		}
		
		class ViewHolder {
			TextView goodsName;
			TextView specCode;
			TextView specName;
		}
	}
}
