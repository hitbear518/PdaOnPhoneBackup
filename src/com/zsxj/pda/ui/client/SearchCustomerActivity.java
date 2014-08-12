package com.zsxj.pda.ui.client;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.wdt.Customer;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTQuery;
import com.zsxj.pda.wdt.WDTQuery.QueryCallBack;
import com.zsxj.pdaonphone.R;

public class SearchCustomerActivity extends Activity implements QueryCallBack {
	
	private EditText mSearchTermEdit;
	private Button mSearchBtn;
	private RelativeLayout mProgressLayout;
	private ListView mCustomerList;
	private MyAdapter mAdapter;
	
	private Customer[] mCustomers;
	
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void dispatchMessage(Message msg) {
			mProgressLayout.setVisibility(View.GONE);
			switch (msg.what) {
			case HandlerCases.NO_CONN:
				Util.toast(getApplicationContext(), R.string.no_conn);
				break;
			case HandlerCases.PREPARE_QUERY_ERROR:
			case HandlerCases.QUERY_FAIL:
				Util.toast(getApplicationContext(), "查询客户失败");
				setListEmpty();
				break;
			case HandlerCases.EMPTY_RESULT:
				Util.toast(getApplicationContext(), "没有匹配的客户");
				setListEmpty();
				break;
			case HandlerCases.QUERY_SUCCESS:
				dealData();
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_customer_activity);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.cash_sale);
		
		mSearchTermEdit = (EditText) findViewById(R.id.search_term_edit);
		mSearchBtn = (Button) findViewById(R.id.search_btn);
		mProgressLayout = (RelativeLayout) findViewById(R.id.progress_layout);
		mCustomerList = (ListView) findViewById(R.id.customer_list);
		
		mSearchBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				searchCustomers();
			}
		});
		
		mAdapter = new MyAdapter();
		mCustomerList.setAdapter(mAdapter);
		mCustomerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				setCustomerAndReturn(position);
			}
		});
	}
	
	private void setCustomerAndReturn(int position) {
		Intent data = new Intent();
		data.putExtra(Extras.CUSTOMER_NAME, mCustomers[position].customerName);
		data.putExtra(Extras.TEL, mCustomers[position].tel);
		data.putExtra(Extras.NICK_NAME, mCustomers[position].nickName);
		data.putExtra(Extras.ZIP_CODE, mCustomers[position].zip);
		data.putExtra(Extras.PROVINCE, mCustomers[position].province);
		data.putExtra(Extras.CITY, mCustomers[position].city);
		data.putExtra(Extras.DISTRICT, mCustomers[position].district);
		data.putExtra(Extras.STREET_ADDRESS, mCustomers[position].address);
		data.putExtra(Extras.EMAIL, mCustomers[position].email);
		setResult(RESULT_OK, data);
		finish();
	}
	
	private void searchCustomers() {
		String tel = mSearchTermEdit.getText().toString();
		if (TextUtils.isEmpty(tel)) {
			Util.toast(getApplicationContext(), "请输入关键字");
			return;
		}
		
		WDTQuery.getinstance().queryCustomers(this, tel);
		mProgressLayout.setVisibility(View.VISIBLE);
	}
	
	private void setListEmpty() {
		mCustomers = null;
		mAdapter.notifyDataSetChanged();
	}
	
	private void dealData() {
		mAdapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			break;
		}
		return false;
	}
	
	@Override
	public void onQuerySuccess(Object qr) {
		if (qr == null) {
			mHandler.sendEmptyMessage(HandlerCases.EMPTY_RESULT);
			return;
		}
		
		if (qr instanceof Customer[]) {
			mCustomers = (Customer[]) qr;
			mHandler.sendEmptyMessage(HandlerCases.QUERY_SUCCESS);
		} else {
			throw new ClassCastException("Wrong query result type");
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
		default:
			break;
		}
	}
	
	class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mCustomers == null ? 0 : mCustomers.length;
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.simple_list_item_3, parent, false);
				holder = new ViewHolder();
				holder.nickNameTv = (TextView) convertView.findViewById(R.id.text1);
				holder.customerNameTv = (TextView) convertView.findViewById(R.id.text2);
				holder.telTv = (TextView) convertView.findViewById(R.id.text3);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.nickNameTv.setText(mCustomers[position].nickName);
			holder.customerNameTv.setText(mCustomers[position].customerName);
			holder.telTv.setText(mCustomers[position].tel);
			
			return convertView;
		}
		
		class ViewHolder {
			TextView nickNameTv;
			TextView customerNameTv;
			TextView telTv;
		}
	}
}
