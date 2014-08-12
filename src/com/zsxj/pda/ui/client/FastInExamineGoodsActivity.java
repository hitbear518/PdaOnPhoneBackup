package com.zsxj.pda.ui.client;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zsxj.pda.provider.ProviderContract.FastInExamineGoodsInfo;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.ConstParams.ScanType;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.wdt.Price;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTQuery;
import com.zsxj.pda.wdt.WDTQuery.QueryCallBack;
import com.zsxj.pdaonphone.R;

public class FastInExamineGoodsActivity extends Activity implements QueryCallBack {
	
	private RelativeLayout mProgressLayout;
	private TextView mSpecBarcodeTv;
	private TextView mGoodsNumTv;
	private TextView mGoodsNameTv;
	private TextView mSpecCodeTv;
	private TextView mSpecNameTv;
	private EditText mCountEdit;
	private EditText mUnitPriceEdit;
	private EditText mDiscountEdit;
	private TextView mMoneyTv;
	private Button mOkBtn;
	
	private Price mPrice;
	private String mUnitPriceStr;
	
	private boolean mFromListClick = false;
	
	private String mBarcode;
	
	private int mSpecId;
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void dispatchMessage(Message msg) {
			mProgressLayout.setVisibility(View.GONE);
			switch (msg.what) {
			case HandlerCases.NO_CONN:
				Util.toast(getApplicationContext(), R.string.no_conn);
				break;
			case HandlerCases.EMPTY_RESULT:
				Util.toast(getApplicationContext(), R.string.empty_result);
				break;
			case HandlerCases.PREPARE_QUERY_ERROR:
				Util.toast(getApplicationContext(), R.string.query_price_fail);
				break;
			case HandlerCases.QUERY_FAIL:
				Util.toast(getApplicationContext(), R.string.query_price_fail);
				break;
			case HandlerCases.QUERY_SUCCESS:
				dealData();
				break;
			case HandlerCases.SCAN_RESULT:
				Intent startScan = 
					new Intent(getApplicationContext(), ScanAndListActivity.class);
				startScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startScan.putExtra(Extras.BARCODE, mBarcode);
				startScan.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_FAST_IN_EXAMINE_GOODS);
				startActivity(startScan);
				finish();
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fast_in_examine_goods_activity);
		
		final ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.fast_in_examine_goods);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		final View rootView = findViewById(R.id.root);
		rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			
			@Override
			public void onGlobalLayout() {
				int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
				if (heightDiff > 300) {
					getActionBar().hide();
				} else {
					getActionBar().show();
				}
			}
		});
		
		mProgressLayout = (RelativeLayout) findViewById(R.id.progress_layout);
		
		mSpecBarcodeTv = (TextView) findViewById(R.id.spec_barcode_tv);
		String specBarcode = getIntent().getStringExtra(Extras.SPEC_BARCODE);
		mSpecBarcodeTv.setText(specBarcode);
		
		mGoodsNumTv = (TextView) findViewById(R.id.goods_num_tv);
		String goodsNum = getIntent().getStringExtra(Extras.GOODS_NUM);
		mGoodsNumTv.setText(goodsNum);
		
		mGoodsNameTv = (TextView) findViewById(R.id.goods_name_tv);
		String goodsName = getIntent().getStringExtra(Extras.GOODS_NAME);
		mGoodsNameTv.setText(goodsName);
		
		mSpecCodeTv = (TextView) findViewById(R.id.spec_code_tv);
		String specCode = getIntent().getStringExtra(Extras.SPEC_CODE);
		mSpecCodeTv.setText(specCode);
		
		mSpecNameTv = (TextView) findViewById(R.id.spec_name_tv);
		String specName = getIntent().getStringExtra(Extras.SPEC_NAME);
		mSpecNameTv.setText(specName);
		
		mCountEdit = (EditText) findViewById(R.id.count_edit);
		String countStr = getIntent().getStringExtra(Extras.COUNT);
		if (TextUtils.isEmpty(countStr))
			mCountEdit.setText("1");
		else {
			mCountEdit.setText(countStr);
			mFromListClick = true;
		}
		mCountEdit.setSelection(mCountEdit.getText().length());
		mUnitPriceEdit = (EditText) findViewById(R.id.unit_price_edit);
		String unitPriceStr = getIntent().getStringExtra(Extras.UNIT_PRICE);
		if (!TextUtils.isEmpty(unitPriceStr))
			mUnitPriceEdit.setText(unitPriceStr);
		mDiscountEdit = (EditText) findViewById(R.id.discount_edit);
		String discountStr = getIntent().getStringExtra(Extras.DISCOUNT);
		if (TextUtils.isEmpty(discountStr))
			mDiscountEdit.setText("1.0");
		else 
			mDiscountEdit.setText(discountStr);
		mMoneyTv = (TextView) findViewById(R.id.money_tv);
		mCountEdit.addTextChangedListener(new MyTextWatcher());
		mUnitPriceEdit.addTextChangedListener(new MyTextWatcher());
		mDiscountEdit.addTextChangedListener(new MyTextWatcher());
		countMoney();
		
		mOkBtn = (Button) findViewById(R.id.ok_btn);
		mOkBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				writeEntry();
			}
		});
		
		mSpecId = getIntent().getIntExtra(Extras.SPEC_ID, -1);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (!mFromListClick)
			WDTQuery.getinstance().queryPrice(this, this, mSpecId, Globals.getSupplierId());
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	
	private void writeEntry() {
		String specBarcode = getIntent().getStringExtra(Extras.SPEC_BARCODE);
		String goodsNum = getIntent().getStringExtra(Extras.GOODS_NUM);
		String goodsName = getIntent().getStringExtra(Extras.GOODS_NAME);
		String specCode = getIntent().getStringExtra(Extras.SPEC_CODE);
		String specName = getIntent().getStringExtra(Extras.SPEC_NAME);
		
		String countStr = mCountEdit.getText().toString();
		if (!checkCount(countStr)) {
			Toast.makeText(this, 
				R.string.please_reenter_goods_count, 
				Toast.LENGTH_SHORT).show();
			return;
		}
			
		String unitPriceStr = mUnitPriceEdit.getText().toString();
		if (TextUtils.isEmpty(unitPriceStr) || unitPriceStr.equals(".")) {
			unitPriceStr = "0";
		}
		String discountStr = mDiscountEdit.getText().toString();
		if (TextUtils.isEmpty(discountStr) || discountStr.equals(".")) {
			discountStr = "0";
		}
		String selection = FastInExamineGoodsInfo.COLUMN_NAME_SPEC_ID + "=?";
		String[] selectionArgs = {
			mSpecId + ""
		};
		
		Cursor cursor = getContentResolver().query(FastInExamineGoodsInfo.CONTENT_URI, 
			null, selection, selectionArgs, null);
		ContentValues values = new ContentValues();
		values.put(FastInExamineGoodsInfo.COLUMN_NAME_SPEC_ID, mSpecId);
		values.put(FastInExamineGoodsInfo.COLUMN_NAME_SPEC_BARCODE, specBarcode);
		values.put(FastInExamineGoodsInfo.COLUMN_NAME_GOODS_NUM, goodsNum);
		values.put(FastInExamineGoodsInfo.COLUMN_NAME_GOODS_NAME, goodsName);
		values.put(FastInExamineGoodsInfo.COLUMN_NAME_SPEC_CODE, specCode);
		values.put(FastInExamineGoodsInfo.COLUMN_NAME_SPEC_NAME, specName);
		values.put(FastInExamineGoodsInfo.COLUMN_NAME_COUNT, countStr);
		values.put(FastInExamineGoodsInfo.COLUMN_NAME_UNIT_PRICE, unitPriceStr);
		values.put(FastInExamineGoodsInfo.COLUMN_NAME_DISCOUNT, discountStr);
		if (0 == cursor.getCount()) {
			getContentResolver().insert(FastInExamineGoodsInfo.CONTENT_URI, 
				values);
		} else {
			getContentResolver().update(FastInExamineGoodsInfo.CONTENT_URI, 
				values, selection, selectionArgs);
		}
		
		Intent intent = new Intent(this, FastInExamineGoodsListActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
	
	private boolean checkCount(String countStr) {
		if (TextUtils.isEmpty(countStr))
			return false;
		
		Double count = Double.parseDouble(countStr);
		if (count.equals(Double.valueOf(0))) {
			return false;
		}
		
		return true;
	}
	
	private void dealData() {
		switch (Globals.getWhichPrice()) {
		case 0:
			mUnitPriceStr = mPrice.suppliyPrice;
			break;
		case 1:
			mUnitPriceStr = mPrice.retailPrice;
			break;
		case 2:
			mUnitPriceStr = mPrice.wholesalePrice;
			break;
		case 3:
			mUnitPriceStr = mPrice.memberPrice;
			break;
		case 4:
			mUnitPriceStr = mPrice.purchasePrice;
			break;
		default:
			break;
		}
		mUnitPriceEdit.setText(mUnitPriceStr);
		countMoney();
	}
	
	private void countMoney() {
		String quantityStr = mCountEdit.getText().toString();
		String unitPriceStr = mUnitPriceEdit.getText().toString();
		String discountStr = mDiscountEdit.getText().toString();
		if (TextUtils.isEmpty(quantityStr) ||
			TextUtils.isEmpty(unitPriceStr) || unitPriceStr.equals(".") ||
			TextUtils.isEmpty(discountStr) || discountStr.equals(".")) {
			
			mMoneyTv.setText("0.0000");
			return;
		}
		Double quantity = Double.parseDouble(quantityStr);
		Double unitPrice = Double.parseDouble(unitPriceStr);
		Double discount = Double.parseDouble(discountStr);
		Double money = quantity * unitPrice * discount;
		mMoneyTv.setText(String.format("%.4f", money));
	}

	@Override
	public void onQuerySuccess(Object qr) {
		if (null == qr) {
			mHandler.sendEmptyMessage(HandlerCases.EMPTY_RESULT);
			return;
		}
		
		if (!Price.class.isInstance(qr)) {
			throw new ClassCastException("Wrong query result type");
		}
		
		mPrice = (Price) qr;
		mHandler.sendEmptyMessage(HandlerCases.QUERY_SUCCESS);
	}

	@Override
	public void onQueryFail(int type, WDTException wdtEx) {
		if (null == wdtEx) {
			mHandler.sendEmptyMessage(type);
			return;
		}
		switch (wdtEx.getStatus()) {
		case 1064:
			mHandler.sendEmptyMessage(HandlerCases.UPDATE_FAIL);
			break;
		default:
			break;
		}
	}
	
	private class MyTextWatcher implements TextWatcher {
		

		@Override
		public void afterTextChanged(Editable s) {
			countMoney();
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			
		}
	}
}
