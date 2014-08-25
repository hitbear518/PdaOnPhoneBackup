package com.zsxj.pda.ui.client;

import java.math.RoundingMode;
import java.text.DecimalFormat;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.zsxj.pda.provider.ProviderContract.CashSaleGoods;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.ScanType;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.util.ViewUtils;
import com.zsxj.pda.wdt.CashSaleSpec;
import com.zsxj.pdaonphone.R;

public class CashSaleGoodsListActivity extends ListActivity {
	
	private TextView mTotalTv;
	private TextView mGoodsTotalTv;
	private Spinner mPriceSp;
	
	private Cursor mCursor = null;
	
	private double mNoDiscountTotal;
	private double mDiscountTotal;
	
	private String mBarcode;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cash_sale_goods_list_activity);
		
		final ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.cash_sale);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mTotalTv = (TextView) findViewById(R.id.total_tv);
		mGoodsTotalTv = (TextView) findViewById(R.id.goods_total_tv);
		mPriceSp = (Spinner) findViewById(R.id.price_sp);
				
		registerForContextMenu(getListView());
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		mCursor.moveToPosition(position);
			
		int specId = mCursor.getInt(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_SPEC_ID));
		String specBarcode = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_SPEC_BARCODE));
		String goodsNum = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_GOODS_NUM));
		String goodsName = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_GOODS_NAME));
		String specCode = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_SPEC_CODE));
		String specName = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_SPEC_NAME));
		String countStr = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_COUNT));
		String retailPrice = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_RETAILE_PRICE));
		String wholesalePrice = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_WHOLESALE_PRICE));
		String memberPrice = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_MEMBER_PRICE));
		String purchasePrice = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_PURCHASE_PRICE));
		String price1 = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_PRICE_1));
		String price2 = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_PRICE_2));
		String price3 = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_PRICE_3));
		String discountStr = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_DISCOUNT));
		String stock = mCursor.getString(mCursor.getColumnIndex(CashSaleGoods.COLUMN_NAME_CASH_SALE_STOCK));
		String barcode = mCursor.getString(mCursor.getColumnIndex(
			CashSaleGoods.COLUMN_NAME_BARCODE));
		
		Intent intent = new Intent(this,CashSaleGoodsActivity.class);
		intent.putExtra(Extras.SPEC_ID, specId);
		intent.putExtra(Extras.SPEC_BARCODE, specBarcode);
		intent.putExtra(Extras.GOODS_NUM, goodsNum);
		intent.putExtra(Extras.GOODS_NAME, goodsName);
		intent.putExtra(Extras.SPEC_CODE, specCode);
		intent.putExtra(Extras.SPEC_NAME, specName);
		intent.putExtra(Extras.COUNT, countStr);
		intent.putExtra(Extras.RETAIL_PRICE, retailPrice);
		intent.putExtra(Extras.WHOLESALE_PRICE, wholesalePrice);
		intent.putExtra(Extras.MEMBER_PRICE, memberPrice);
		intent.putExtra(Extras.PURCHASE_PRICE, purchasePrice);
		intent.putExtra(Extras.PRICE_1, price1);
		intent.putExtra(Extras.PRICE_2, price2);
		intent.putExtra(Extras.PRICE_3, price3);
		intent.putExtra(Extras.DISCOUNT, discountStr);
		intent.putExtra(Extras.CASH_SALE_STOCK, stock);
		intent.putExtra(Extras.BARCODE, barcode);
		startActivity(intent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(
			R.menu.cash_sale_list_menu, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mPriceSp = (Spinner) menu.findItem(R.id.price_sp_menu).getActionView();
        Util.initSpinner(this, mPriceSp, R.layout.simple_spinner_dropdown_item_in_action_bar, 
    		CashSaleSpec.getPrices());
		mPriceSp.setSelection(Globals.getWhichPrice());
		mPriceSp.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				Globals.setWhichPrice(position);
				requery();
				countInfo();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.search_action:
			ViewUtils.showSearchDialog(this, ScanType.TYPE_CASH_SALE_BY_TERM);
			break;
		case R.id.submit_action:
			startSubmit();
			break;
		case R.id.clear_order_action:
			new AlertDialog.Builder(this)
				.setMessage("确定清除订单吗？")
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						clearOrder();
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
			
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	private void showSearchDialog(Context context) {
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == RESULT_OK) {
			mBarcode = data.getStringExtra("result");
			scanAndList();
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.delete_action_context_menu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo(); 
		switch (item.getItemId()) {
		case R.id.action_delete:
			deleteItem(info.id);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	private void clearOrder() {
		String selection = CashSaleGoods.COLUMN_NAME_WAREHOUSE_ID + "=?";
		String[] selectionArgs = {
				Globals.getModuleUseWarehouseId() + ""
		};
		getContentResolver().delete(CashSaleGoods.CONTENT_URI, 
			selection, selectionArgs);
		requery();
	}
	
	private void deleteItem(long id) {
		getContentResolver().delete(
			ContentUris.withAppendedId(CashSaleGoods.CONTENT_URI, id), 
			null, null);
		String selection = CashSaleGoods.COLUMN_NAME_WAREHOUSE_ID + "=?";
		String[] selectionArgs = {
				Globals.getModuleUseWarehouseId() + ""
		};
		if (null != mCursor)
			mCursor.close();
		mCursor = getContentResolver().query(CashSaleGoods.CONTENT_URI, 
				null, selection, selectionArgs, null);
		CursorAdapter adapter = new MyListAdapter(this, mCursor, 0);
		setListAdapter(adapter);
		
		countInfo();
	}
	
	private void startSubmit() {
		if (getListAdapter().getCount() == 0) {
			Util.toast(getApplicationContext(), "没有可提交的条目");
			return;
		}
		Intent intent = new Intent(this, CashSaleSubmitActivity.class);
		intent.putExtra(Extras.NO_DISCOUNT_TOTAL, mNoDiscountTotal);
		intent.putExtra(Extras.DISCOUNT_TOTAL, mDiscountTotal);
		startActivity(intent);
	}
	
	private void countInfo() {
		mCursor.moveToPosition(-1);
		int total = 0;
		double noDiscountTotal = 0d;
		double discountTotal = 0d;
		while (mCursor.moveToNext()) {
			String countStr = mCursor.getString(mCursor.getColumnIndex(
				CashSaleGoods.COLUMN_NAME_COUNT));
			int count = Integer.parseInt(countStr);
			total += count;
			String unitPriceStr = null;
			switch (Globals.getWhichPrice()) {
			case 0:
				unitPriceStr = mCursor.getString(mCursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_RETAILE_PRICE));
				break;
			case 1:
				unitPriceStr = mCursor.getString(mCursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_WHOLESALE_PRICE));
				break;
			case 2:
				unitPriceStr = mCursor.getString(mCursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_MEMBER_PRICE));
				break;
			case 3:
				unitPriceStr = mCursor.getString(mCursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_PURCHASE_PRICE));
				break;
			case 4:
				unitPriceStr = mCursor.getString(mCursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_PRICE_1));
				break;
			case 5:
				unitPriceStr = mCursor.getString(mCursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_PRICE_2));
				break;
			case 6:
				unitPriceStr = mCursor.getString(mCursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_PRICE_3));
				break;
			default:
				break;
			}
			String discountStr = mCursor.getString(mCursor.getColumnIndex(
				CashSaleGoods.COLUMN_NAME_DISCOUNT));
			double unitPrice = Double.parseDouble(unitPriceStr);
			double discount = Double.parseDouble(discountStr);
			
			double noDiscount = unitPrice * count;
			noDiscountTotal += noDiscount;
			double discountMoney = noDiscount * (1 - discount);
			discountTotal += discountMoney;
		}
		mCursor.moveToPosition(-1);
		
		mTotalTv.setText(String.valueOf(total));

		mNoDiscountTotal = noDiscountTotal;
		mDiscountTotal = discountTotal;
		double goodsTotal = noDiscountTotal - discountTotal;
		mGoodsTotalTv.setText(String.format("%.2f 元", goodsTotal));
	}
	
	@Override
	protected void onResume() {
		super.onResume();	
		requery();
		countInfo();
	}
	
	private void requery() {
		String selection = CashSaleGoods.COLUMN_NAME_WAREHOUSE_ID + "=?";
		String[] selectionArgs = {
				Globals.getModuleUseWarehouseId() + ""
		};
		if (null != mCursor)
			mCursor.close();
		mCursor = getContentResolver().query(CashSaleGoods.CONTENT_URI, 
				null, selection, selectionArgs, null);
		CursorAdapter adapter = new MyListAdapter(this, mCursor, 0);
		setListAdapter(adapter);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mCursor.close();
	}
	
	private void scanAndList() {
		Intent intent = new Intent(getApplicationContext(), 
			ScanAndListActivity.class);
		intent.putExtra(Extras.BARCODE, mBarcode);
		intent.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_CASH_SALE);
		startActivity(intent);
	}
	
	private class MyListAdapter extends CursorAdapter {
		
		private LayoutInflater mInflater;

		public MyListAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
			mInflater = LayoutInflater.from(context);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView specBarcodeTv = (TextView) 
				view.findViewById(R.id.spec_barcode_tv);
			specBarcodeTv.setText(cursor.getString(
				cursor.getColumnIndex(
				CashSaleGoods.COLUMN_NAME_SPEC_BARCODE)));
			
			TextView goodsNumTv = (TextView) 
				view.findViewById(R.id.goods_num_tv);
			goodsNumTv.setText(cursor.getString(
				cursor.getColumnIndex(
				CashSaleGoods.COLUMN_NAME_GOODS_NUM)));
			
			TextView goodsNameTv = (TextView)
				view.findViewById(R.id.goods_name_tv);
			goodsNameTv.setText(cursor.getString(
				cursor.getColumnIndex(
				CashSaleGoods.COLUMN_NAME_GOODS_NAME)));
			
			TextView specCodeTv = (TextView)
				view.findViewById(R.id.spec_code_tv);
			specCodeTv.setText(cursor.getString(
				cursor.getColumnIndex(
				CashSaleGoods.COLUMN_NAME_SPEC_CODE)));
			
			TextView specNameTv = (TextView)
				view.findViewById(R.id.spec_name_tv);
			specNameTv.setText(cursor.getString(
				cursor.getColumnIndex(
				CashSaleGoods.COLUMN_NAME_SPEC_NAME)));
			
			TextView stockTv = (TextView) view.findViewById(R.id.stock_can_order_btn);
			stockTv.setText(cursor.getString(cursor.getColumnIndex(CashSaleGoods.COLUMN_NAME_CASH_SALE_STOCK)));
			
			String countStr = cursor.getString(
				cursor.getColumnIndex(
				CashSaleGoods.COLUMN_NAME_COUNT));
			TextView countTv = (TextView)
				view.findViewById(R.id.count_tv);
			countTv.setText(countStr);
			
			DecimalFormat df = new DecimalFormat("#.####");
			df.setRoundingMode(RoundingMode.HALF_UP);

			String unitPriceStr = null;
			switch (Globals.getWhichPrice()) {
			case 0:
				unitPriceStr = cursor.getString(cursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_RETAILE_PRICE));
				break;
			case 1:
				unitPriceStr = cursor.getString(cursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_WHOLESALE_PRICE));
				break;
			case 2:
				unitPriceStr = cursor.getString(cursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_MEMBER_PRICE));
				break;
			case 3:
				unitPriceStr = cursor.getString(cursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_PURCHASE_PRICE));
				break;
			case 4:
				unitPriceStr = cursor.getString(cursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_PRICE_1));
				break;
			case 5:
				unitPriceStr = cursor.getString(cursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_PRICE_2));
				break;
			case 6:
				unitPriceStr = cursor.getString(cursor.getColumnIndex(
					CashSaleGoods.COLUMN_NAME_PRICE_3));
				break;
			default:
				break;
			}
		
			TextView unitPriceTv = (TextView)
				view.findViewById(R.id.unit_price_tv);
			
			String discountStr = cursor.getString(
				cursor.getColumnIndex(
				CashSaleGoods.COLUMN_NAME_DISCOUNT));
			TextView discountTv = (TextView)
				view.findViewById(R.id.discount_tv);
			
			int count = Integer.parseInt(countStr);
			Double unitPrice = Double.parseDouble(unitPriceStr);
			Double discount = Double.parseDouble(discountStr);
			Double price = count * unitPrice * discount;
			TextView priceTv = (TextView)
				view.findViewById(R.id.price_tv);
			
			unitPriceTv.setText(df.format(unitPrice));
			discountTv.setText(df.format(discount));
			priceTv.setText(df.format(price) + " 元");
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			 View view = mInflater.inflate(
				 R.layout.cash_sale_goods_list_item, parent, false);
             bindView(view, context, cursor);
             return view;
		}
	}
}
