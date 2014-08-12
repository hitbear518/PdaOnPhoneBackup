package com.zsxj.pda.ui.client;

import java.math.RoundingMode;
import java.text.DecimalFormat;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.zsxj.pda.provider.ProviderContract.FastInExamineGoodsInfo;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.ConstParams.ScanType;
import com.zsxj.pda.util.Util;
import com.zsxj.pdaonphone.R;

public class FastInExamineGoodsListActivity extends ListActivity {
	
	private TextView mTotalTv;
	private TextView mGoodsTotalTv;
	
	private Cursor mCursor = null;
	
	private String mBarcode;

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case HandlerCases.SCAN_RESULT:
				scanAndList();
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fast_in_examine_goods_list_activity);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.fast_in_examine_goods);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mTotalTv = (TextView) findViewById(R.id.total_tv);
		mGoodsTotalTv = (TextView) findViewById(R.id.goods_total_tv);
		
		registerForContextMenu(getListView());
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		mCursor.moveToPosition(position);
		
		int specId = mCursor.getInt(mCursor.getColumnIndex(
			FastInExamineGoodsInfo.COLUMN_NAME_SPEC_ID));
		String specBarcode = mCursor.getString(mCursor.getColumnIndex(
			FastInExamineGoodsInfo.COLUMN_NAME_SPEC_BARCODE));
		String goodsNum = mCursor.getString(mCursor.getColumnIndex(
			FastInExamineGoodsInfo.COLUMN_NAME_GOODS_NUM));
		String goodsName = mCursor.getString(mCursor.getColumnIndex(
			FastInExamineGoodsInfo.COLUMN_NAME_GOODS_NAME));
		String specCode = mCursor.getString(mCursor.getColumnIndex(
			FastInExamineGoodsInfo.COLUMN_NAME_SPEC_CODE));
		String specName = mCursor.getString(mCursor.getColumnIndex(
			FastInExamineGoodsInfo.COLUMN_NAME_SPEC_NAME));
		String countStr = mCursor.getString(mCursor.getColumnIndex(
			FastInExamineGoodsInfo.COLUMN_NAME_COUNT));
		String unitPriceStr = mCursor.getString(mCursor.getColumnIndex(
			FastInExamineGoodsInfo.COLUMN_NAME_UNIT_PRICE));
		String discountStr = mCursor.getString(mCursor.getColumnIndex(
			FastInExamineGoodsInfo.COLUMN_NAME_DISCOUNT));
		
		Intent intent = new Intent(this, FastInExamineGoodsActivity.class);
		intent.putExtra(Extras.SPEC_ID, specId);
		intent.putExtra(Extras.SPEC_BARCODE, specBarcode);
		intent.putExtra(Extras.GOODS_NUM, goodsNum);
		intent.putExtra(Extras.GOODS_NAME, goodsName);
		intent.putExtra(Extras.SPEC_CODE, specCode);
		intent.putExtra(Extras.SPEC_NAME, specName);
		intent.putExtra(Extras.COUNT, countStr);
		intent.putExtra(Extras.UNIT_PRICE, unitPriceStr);
		intent.putExtra(Extras.DISCOUNT, discountStr);
		startActivity(intent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(
			R.menu.fast_in_examine_goods_list_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.info_action:
			finish();
			break;
		case R.id.submit_action:
			startSubmit();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
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
	
	private void deleteItem(long id) {
		getContentResolver().delete(
			ContentUris.withAppendedId(FastInExamineGoodsInfo.CONTENT_URI, id), 
			null, null);
		if (null != mCursor)
			mCursor.close();
		mCursor = getContentResolver().query(FastInExamineGoodsInfo.CONTENT_URI, 
				null, null, null, null);
		CursorAdapter adapter = new MyListAdapter(this, mCursor, 0);
		setListAdapter(adapter);
		
		countInfo();
	}
	
	
	private void startSubmit() {
		if (getListAdapter().getCount() == 0) {
			Util.toast(getApplicationContext(), "没有可提交的条目");
			return;
		}
		Intent intent = new Intent(this, FastInExamineGoodsSubmitActivity.class);
		startActivity(intent);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (null != mCursor)
			mCursor.close();
		mCursor = getContentResolver().query(FastInExamineGoodsInfo.CONTENT_URI, 
				null, null, null, null);
		CursorAdapter adapter = new MyListAdapter(this, mCursor, 0);
		setListAdapter(adapter);
	
		countInfo();
	}
	
	private void countInfo() {
		mCursor.moveToPosition(-1);
		int total = 0;
		Double goodsTotal = 0d;
		while (mCursor.moveToNext()) {
			String countStr = mCursor.getString(mCursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_COUNT));
			total += Integer.parseInt(countStr);
			String unitPriceStr = mCursor.getString(mCursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_UNIT_PRICE));
			String discountStr = mCursor.getString(mCursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_DISCOUNT));
			Double unitPrice = Double.parseDouble(unitPriceStr);
			Double discount = Double.parseDouble(discountStr);
			Double price = total * unitPrice * discount;
			goodsTotal += price;
		}
		mCursor.moveToPosition(-1);
		
		mTotalTv.setText(String.valueOf(total));
		mGoodsTotalTv.setText(String.format("%.4f 元", goodsTotal));
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
		intent.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_FAST_IN_EXAMINE_GOODS);
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
				FastInExamineGoodsInfo.COLUMN_NAME_SPEC_BARCODE)));
			
			TextView goodsNumTv = (TextView) 
				view.findViewById(R.id.goods_num_tv);
			goodsNumTv.setText(cursor.getString(
				cursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_GOODS_NUM)));
			
			TextView goodsNameTv = (TextView)
				view.findViewById(R.id.goods_name_tv);
			goodsNameTv.setText(cursor.getString(
				cursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_GOODS_NAME)));
			
			TextView specCodeTv = (TextView)
				view.findViewById(R.id.spec_code_tv);
			specCodeTv.setText(cursor.getString(
				cursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_SPEC_CODE)));
			
			TextView specNameTv = (TextView)
				view.findViewById(R.id.spec_name_tv);
			specNameTv.setText(cursor.getString(
				cursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_SPEC_NAME)));
			
			String countStr = cursor.getString(
				cursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_COUNT));
			TextView countTv = (TextView)
				view.findViewById(R.id.count_tv);
			countTv.setText(countStr);
			
			DecimalFormat df = new DecimalFormat("#.####");
			df.setRoundingMode(RoundingMode.HALF_UP);
			String unitPriceStr = cursor.getString(
				cursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_UNIT_PRICE));
			TextView unitPriceTv = (TextView)
				view.findViewById(R.id.unit_price_tv);
			
			String discountStr = cursor.getString(
				cursor.getColumnIndex(
				FastInExamineGoodsInfo.COLUMN_NAME_DISCOUNT));
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
				 R.layout.fast_examine_goods_list_item, parent, false);
             bindView(view, context, cursor);
             return view;
		}
	}
}
