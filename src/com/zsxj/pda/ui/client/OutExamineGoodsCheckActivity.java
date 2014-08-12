package com.zsxj.pda.ui.client;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.zsxj.pda.provider.ProviderContract.TradeGoods;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.ScanType;
import com.zsxj.pda.util.Util;
import com.zsxj.pdaonphone.R;

public class OutExamineGoodsCheckActivity extends Activity {
	
	private TextView mBarcodeTv;
	private TextView mGoodsNoTv;
	private TextView mGoodsNameTv;
	private TextView mSpecCodeTv;
	private TextView mSpecNameTv;
	private TextView mPositionNameTv;
	private TextView mSellCountTv;
	private EditText mCountCheckEdit;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.out_examine_goods_check_activity);
		
		int scanType = getIntent().getIntExtra(Extras.SCAN_TYPE, -1);
		
		final ActionBar actionBar = getActionBar();
		if (ScanType.TYPE_PICK_GOODS == scanType) {
			actionBar.setTitle(R.string.pick_goods);
		} else {
			actionBar.setTitle(R.string.out_examine_goods);
		}
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mBarcodeTv = (TextView) findViewById(R.id.barcode_tv);
		mGoodsNoTv = (TextView) findViewById(R.id.goods_num_tv);
		mGoodsNameTv = (TextView) findViewById(R.id.goods_name_tv);
		mSpecCodeTv = (TextView) findViewById(R.id.spec_code_tv);
		mSpecNameTv = (TextView) findViewById(R.id.spec_name_tv);
		mSellCountTv = (TextView) findViewById(R.id.sell_count_tv);
		mCountCheckEdit = (EditText) findViewById(R.id.count_check_edit);
		
		String barcode = getIntent().getStringExtra(Extras.BARCODE);
		mBarcodeTv.setText(barcode);
		String goodsNo = getIntent().getStringExtra(Extras.GOODS_NUM);
		mGoodsNoTv.setText(goodsNo);
		String goodsName = getIntent().getStringExtra(Extras.GOODS_NAME);
		mGoodsNameTv.setText(goodsName);
		String specCode = getIntent().getStringExtra(Extras.SPEC_CODE);
		mSpecCodeTv.setText(specCode);
		String specName = getIntent().getStringExtra(Extras.SPEC_NAME);
		mSpecNameTv.setText(specName);
		
		double sellCount = getIntent().getDoubleExtra(Extras.SELL_COUNT, -1);
		mSellCountTv.setText(String.valueOf(sellCount));
		double countCheck = getIntent().getDoubleExtra(Extras.COUNT_CHECK, -1);
		mCountCheckEdit.setText(String.valueOf((int) countCheck));
		if (ScanType.TYPE_PICK_GOODS == scanType) {
			TextView countCheckTv = (TextView) findViewById(R.id.count_check_tv);
			countCheckTv.setText(R.string.pick_count);
		}
		mCountCheckEdit.setSelection(mCountCheckEdit.getText().length());
		mCountCheckEdit.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE ||
					(EditorInfo.IME_NULL == actionId && 
					KeyEvent.ACTION_DOWN == event.getAction())) {
					countCheck();
					return true;
				}
				return false;
			}
		});
		
		if (ScanType.TYPE_PICK_GOODS == scanType) {
			mPositionNameTv = (TextView) findViewById(R.id.position_name_tv);
			String positionName = getIntent().getStringExtra(Extras.POSITION_NAME);
			mPositionNameTv.setText(positionName);
			
			findViewById(R.id.extra_separator_view).setVisibility(View.VISIBLE);
			findViewById(R.id.position_name_row).setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.ok_action, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.ok_action:
			countCheck();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void countCheck() {
		if (TextUtils.isEmpty(mCountCheckEdit.getText())) {
			Util.toast(this, "校验量不能为空");
			return;
		}
		double countCheck = Double.parseDouble(mCountCheckEdit.getText().toString());
		long id = getIntent().getLongExtra(Extras._ID, -1);
		ContentValues values = new ContentValues();
		values.put(TradeGoods.COLUMN_NAME_COUNT_CHECK, countCheck);
		getContentResolver().update(ContentUris.withAppendedId(
			TradeGoods.CONTENT_URI, id), values, null, null);
		finish();
	}
}
