package com.zsxj.pda.ui.client;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.wdt.PdEntry;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTQuery;
import com.zsxj.pda.wdt.WDTQuery.QueryCallBack;
import com.zsxj.pdaonphone.R;

public class PdEntriesActivity extends Activity implements QueryCallBack{
	
	public static final int REQUEST_PD = 0;
	
	private ListView mEntriesLv;
	private MyListAdapter mAdapter;
	private PdEntry[] mEntries;
	private WDTQuery mQuery;
	
	private int mWhich;
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HandlerCases.NO_CONN:
				Util.toast(getApplicationContext(), R.string.no_conn);
				break;
			case HandlerCases.PREPARE_QUERY_ERROR:
				Util.toast(getApplicationContext(), R.string.get_pd_entries_fail);
				break;
			case HandlerCases.QUERY_FAIL:
				Util.toast(getApplicationContext(), R.string.get_pd_entries_fail);
				break;
			case HandlerCases.EMPTY_RESULT:
				Util.toast(getApplicationContext(), R.string.empty_result);
				break;
			case HandlerCases.QUERY_SUCCESS:
				mAdapter.notifyDataSetChanged();
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pd_entries_activity);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.select_pd_entries);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mEntriesLv = (ListView) findViewById(R.id.entries_lv);
		
		mAdapter = new MyListAdapter(this);
		mEntriesLv.setAdapter(mAdapter);
		
		int warehouseId = getIntent().getIntExtra(Extras.WAREHOUSE_ID, -1);
		
		mQuery = new WDTQuery();
		mQuery.queryPdEntries(this, this, warehouseId);
		
		mEntriesLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mWhich = position;
				Intent intent = new Intent(PdEntriesActivity.this, 
						PdDetailsActivity.class);
				intent.putExtra(Extras.PD_ID, mEntries[mWhich].pdId);
				intent.putExtra(Extras.WAREHOUSE_ID, 
						getIntent().getIntExtra(Extras.WAREHOUSE_ID, -1));
				startActivityForResult(intent, REQUEST_PD);
			}
		});  
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (REQUEST_PD == requestCode && RESULT_OK == resultCode) {
			WDTQuery.getinstance().queryPdEntries(this, this, 
					getIntent().getIntExtra(Extras.WAREHOUSE_ID, -1));
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			break;

		default:
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private class MyListAdapter extends BaseAdapter {
		
		private final Context mContext;

		public MyListAdapter(Context context) {
			mContext = context;
		}
		
		@Override
		public int getCount() {
			return null == mEntries ? 0 : mEntries.length;  
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {			
			ViewHolder holder = null;
			if (convertView != null) {
				holder = (ViewHolder) convertView.getTag();
				holder.number.setText(mEntries[position].pdNum);
				holder.creater.setText(mEntries[position].createUser + "");
				holder.time.setText(mEntries[position].createdTime);
			} else {
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.pd_entries_list_item, parent, false);
				holder = new ViewHolder();
				holder.number = (TextView) convertView.findViewById(R.id.number_tv);
				holder.creater = (TextView) convertView.findViewById(R.id.creater_tv);
				holder.time = (TextView) convertView.findViewById(R.id.time_tv);
				holder.number.setText(mEntries[position].pdNum);
				holder.creater.setText(mEntries[position].createUser + "");
				holder.time.setText(mEntries[position].createdTime);
				convertView.setTag(holder);
			}
			
			setBackground(convertView, position);
			return convertView;
		}
		
		class ViewHolder {
			TextView number;
			TextView creater;
			TextView time;
		}

		private void setBackground(View view, int position) {
			if (0 == position) {
				view.setBackgroundResource(R.drawable.section_bg_1);
			} else if (position < getCount() - 1) {
				view.setBackgroundResource(R.drawable.section_bg_2);
			} else {
				view.setBackgroundResource(R.drawable.section_bg_3);
			}
		}
	}

	@Override
	public void onQuerySuccess(Object qr) {
		if (qr != null) {
			if (!PdEntry[].class.isInstance(qr)) {
				throw new ClassCastException("Wrong query result type");
			}
			
			mEntries = (PdEntry[]) qr;
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
		default:
			break;
		}
	}
}
