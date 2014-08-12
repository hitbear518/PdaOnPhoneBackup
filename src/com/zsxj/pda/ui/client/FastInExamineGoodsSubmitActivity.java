package com.zsxj.pda.ui.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.zsxj.pda.provider.ProviderContract.FastInExamineGoodsInfo;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.wdt.Account;
import com.zsxj.pda.wdt.Logistics;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTQuery;
import com.zsxj.pda.wdt.WDTQuery.QueryCallBack;
import com.zsxj.pda.wdt.WDTUpdate;
import com.zsxj.pda.wdt.WDTUpdate.UpdateCallBack;
import com.zsxj.pdaonphone.R;

@SuppressLint("HandlerLeak")
public class FastInExamineGoodsSubmitActivity extends Activity implements QueryCallBack, UpdateCallBack{
	
	private RelativeLayout mProgressLayout;
	private TextView mGoodsTotalTv;
	private EditText mOtherFeeEdit;
	private TextView mAllTotalTv;
	private EditText mCashFeeEdit;
	private Spinner mAccountsSp;
	private TextView mMoneyOnCreditTv;
	private Spinner mLogisticsSp;
	private TextView mPostageFeeTv;
	private EditText mLogisticNumberTv;
	private Button mOkBtn;
	
	private Cursor mCursor;
	
	private int mWhichAccount;
	private int mWhichLogistic;
	private Logistics[] mLogistics;
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			mProgressLayout.setVisibility(View.GONE);
			switch (msg.what) {
			case HandlerCases.NO_CONN:
				Util.toast(getApplicationContext(), R.string.no_conn);
				break;
			case HandlerCases.PREPARE_QUERY_ERROR:
				Log.e("Submit Examine Goods", 
						getString(R.string.prepare_query_error));
				Util.toast(getApplicationContext(), R.string.query_logistics_fail);
				break;
			case HandlerCases.QUERY_FAIL:
				Util.toast(getApplicationContext(), R.string.query_logistics_fail);
				break;
			case HandlerCases.QUERY_SUCCESS:
				onQueryLogisticsSuccess();
				break;
			case HandlerCases.PREPARE_UPDATE_FAIL:
				Util.toast(getApplicationContext(), R.string.submit_fail);
				break;
			case HandlerCases.UPDATE_FAIL:
				Util.toast(getApplicationContext(), R.string.submit_fail);
				break;
			case HandlerCases.UPDATE_SUCCESS:
				Util.toast(getApplicationContext(), R.string.submit_success);
				deleteEntries();
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
		setContentView(R.layout.fast_examine_goods_submit_activity);
		
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
		mGoodsTotalTv = (TextView) findViewById(R.id.price_tv);
		mOtherFeeEdit = (EditText) findViewById(R.id.other_fee_edit);
		mAllTotalTv = (TextView) findViewById(R.id.all_total_tv);
		mCashFeeEdit = (EditText) findViewById(R.id.cash_fee_edit);
		mAccountsSp = (Spinner) findViewById(R.id.accounts_sp);
		mMoneyOnCreditTv = (TextView) findViewById(R.id.money_on_credit_tv);
		mLogisticsSp = (Spinner) findViewById(R.id.logistics_sp);
		mPostageFeeTv = (TextView) findViewById(R.id.postage_fee_edit);
		mLogisticNumberTv = (EditText) findViewById(R.id.logistic_number_edit);
		mOkBtn = (Button) findViewById(R.id.ok_btn);
		
		mCursor = getContentResolver().query(FastInExamineGoodsInfo.CONTENT_URI, 
			null, null, null, null);
		
		//GoodsTotal
		String goodsTotal = getGoodsTotal(mCursor);
		mGoodsTotalTv.setText(goodsTotal);
		
		mOtherFeeEdit.setText("0");
		mOtherFeeEdit.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				countAllTotal();
				countMoneyOnCredit();
			}
		});
		mAllTotalTv.setText(goodsTotal);
		
		
		mCashFeeEdit.setText("0");
		countAllTotal();
		String alltotal = mAllTotalTv.getText().toString();
		mMoneyOnCreditTv.setText(alltotal);
		mCashFeeEdit.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				countMoneyOnCredit();
			}
		});
		
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, 
			R.layout.simple_spinner_dropdown_item, 
			Account.getAccountNames(Globals.getAccounts()));
		mAccountsSp.setAdapter(arrayAdapter);
		mAccountsSp.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				mWhichAccount = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		mLogisticsSp.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				mWhichLogistic = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		mPostageFeeTv.setText("0");
		
		WDTQuery.getinstance().queryLogistics(this, this);
		
		mOkBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				submit();
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mCursor.close();
	}
	
	private String getGoodsTotal(Cursor cursor) {
		Double goodsTotal = 0.0;
		while (cursor.moveToNext()) {
			String countStr = cursor.getString(cursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_COUNT));
			String unitPriceStr = cursor.getString(cursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_UNIT_PRICE));
			String discountStr = cursor.getString(cursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_DISCOUNT));
			Double count = Double.parseDouble(countStr);
			Double unitPrice = Double.parseDouble(unitPriceStr);
			Double discount = Double.parseDouble(discountStr);
			Double price = count * unitPrice * discount;
			goodsTotal += price;
		}
		cursor.moveToPosition(-1);
		return String.format(Locale.US, "%.4f", goodsTotal);
	}
	
	private void submit() {
		String otherFee = mOtherFeeEdit.getText().toString();
		if (TextUtils.isEmpty(otherFee) || otherFee.equals(".")) {
			Util.toast(this, "请输入正确的其他费用");
			return;
		}
		String cashFee = mCashFeeEdit.getText().toString();
		if (TextUtils.isEmpty(cashFee) || cashFee.equals(".")) {
			Util.toast(this, "请输入正确的现付金额");
			return;
		}
		String postageFee = mPostageFeeTv.getText().toString();
		if (TextUtils.isEmpty(postageFee) || postageFee.equals(".")) {
			Util.toast(this, "请输入正确的运费");
			return;
		}
		
		int userId = Globals.getUserId();
		int warehouseId = Globals.getModuleUseWarehouseId();
		int supplierId = Globals.getSupplierId();
		int accountId = Globals.getAccounts()[mWhichAccount].accountId;
		int logisticId = mLogistics[mWhichLogistic].logisticId;
		String goodsTotal = mGoodsTotalTv.getText().toString();
		
		List<String> infoStrList = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		while (mCursor.moveToNext()) {
			sb.append(mCursor.getInt(mCursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_SPEC_ID)))
				.append(',')
				.append(mCursor.getString(mCursor.getColumnIndex(
					FastInExamineGoodsInfo.COLUMN_NAME_COUNT)))
				.append(',')
				.append(mCursor.getString(mCursor.getColumnIndex(
					FastInExamineGoodsInfo.COLUMN_NAME_UNIT_PRICE)))
				.append(',')
				.append(mCursor.getString(mCursor.getColumnIndex(
					FastInExamineGoodsInfo.COLUMN_NAME_DISCOUNT)))
				.append(',');
			if (sb.length() > 0) {
				infoStrList.add(sb.toString());
				sb = new StringBuilder();
			} else
				sb.append(",");
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
			infoStrList.add(sb.toString());
		}
		
		String postId = mLogisticNumberTv.getText().toString();
		if (null == postId) 
			postId = "";
		
		new WDTUpdate().fastInExamineGoodsSubmit(userId, warehouseId, supplierId, 
			logisticId, accountId, otherFee, goodsTotal, postageFee, cashFee, 
			postId, infoStrList, this);
	}
	
	private void deleteEntries() {
		getContentResolver().delete(FastInExamineGoodsInfo.CONTENT_URI, 
			null, null);
	}

	private void countAllTotal() {
		String goodsTotalStr = mGoodsTotalTv.getText().toString();
		String otherFeeStr = mOtherFeeEdit.getText().toString();
		
		if (TextUtils.isEmpty(otherFeeStr) || otherFeeStr.equals(".")) {
			mAllTotalTv.setText(goodsTotalStr);
			return;
		}
		
		double goodsTotal = Double.parseDouble(goodsTotalStr);
		double otherFee = Double.parseDouble(otherFeeStr);
		double allTotal = goodsTotal + otherFee;
		mAllTotalTv.setText(String.format(Locale.US, "%.4f", allTotal));
	}
	
	private void countMoneyOnCredit() {
		String allTotalStr = mAllTotalTv.getText().toString();
		String mCashFeeStr = mCashFeeEdit.getText().toString();
		
		if (TextUtils.isEmpty(mCashFeeStr) || mCashFeeStr.equals(".")) {
			mMoneyOnCreditTv.setText(allTotalStr);
			return;
		}
		
		Double allTotal = Double.parseDouble(allTotalStr);
		Double cashFee = Double.parseDouble(mCashFeeStr);
		Double moneyOnCredit = allTotal - cashFee;
		mMoneyOnCreditTv.setText(String.format(Locale.US, "%.4f", moneyOnCredit));
	}
	
	private void onQueryLogisticsSuccess() {
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, 
			R.layout.simple_spinner_dropdown_item, Logistics.getNames(mLogistics));
		mLogisticsSp.setAdapter(arrayAdapter);
	}

	@Override
	public void onUpdateSuccess() {
		mHandler.sendEmptyMessage(HandlerCases.UPDATE_SUCCESS);
	}

	@Override
	public void onUpdateFail(int type, WDTException wdtEx) {
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

	@Override
	public void onQuerySuccess(Object qr) {
		if (null == qr) {
			mHandler.sendEmptyMessage(HandlerCases.EMPTY_RESULT);
			return;
		}
		
		if (!Logistics[].class.isInstance(qr))
			throw new ClassCastException("Wrong query result type");
		
		mLogistics = (Logistics[]) qr;
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
			mHandler.sendEmptyMessage(HandlerCases.QUERY_FAIL);
			break;
		default:
			break;
		}
	}
}
