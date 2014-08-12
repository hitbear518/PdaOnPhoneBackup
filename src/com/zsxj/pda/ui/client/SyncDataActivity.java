package com.zsxj.pda.ui.client;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.zsxj.pda.service.SyncPositionsService;
import com.zsxj.pda.util.ConstParams.Actions;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.PrefKeys;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pdaonphone.R;

public class SyncDataActivity extends ListActivity {
	
	private MyListAdapter mAdapter;
	private int mWarehouseId;
	private String[] mLabelText;
	private String[] mValueText;
	private ProgressDialog mProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sync_data_activity);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle("数据同步");
		
		mAdapter = new MyListAdapter();
		SharedPreferences prefs = 
				getSharedPreferences(Globals.getUserPrefsName(), MODE_PRIVATE);
		mWarehouseId = prefs.getInt(PrefKeys.WAREHOUSE_ID, -1);
		prefs = getSharedPreferences(Globals.getWarehousePrefsName(mWarehouseId), MODE_PRIVATE);
		mLabelText = new String[] {
				"同步货位："
		};
		mValueText = new String[] {
				prefs.getString(PrefKeys.SYNC_POSITION_TIME, "初次同步")
		};
		setListAdapter(mAdapter);
		
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setTitle("请等待同步完成");
		mProgressDialog.setCancelable(false);
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Actions.POSITION_SYNC_RESPONSE_PERCENT_ACTION);
		intentFilter.addAction(Actions.POSITION_SYNC_RESPONSE_COMPLETION_ACTION);
		intentFilter.addAction(Actions.POSITION_SYNC_RESPONSE_INTERRUPT_ACTION);
		LocalBroadcastManager.getInstance(this)
			.registerReceiver(new SyncReceiver(), intentFilter);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		switch (position) {
		case 0:
			Intent intent = new Intent(this, SyncPositionsService.class);
			startService(intent);
			mProgressDialog.show();
			break;
		default:
			break;
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private class SyncReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Actions.POSITION_SYNC_RESPONSE_PERCENT_ACTION)) {
				String str = intent.getStringExtra(Extras.POSITION_SYNC_RESPONSE_PERCENT);
				mProgressDialog.setMessage(str);
			} else if (action.equals(Actions.POSITION_SYNC_RESPONSE_COMPLETION_ACTION)) {
				mValueText[0] =intent.getStringExtra(Extras.POSITION_SYNC_RESPONSE_TIME);
				mProgressDialog.setMessage("");
				mProgressDialog.dismiss();
				mAdapter.notifyDataSetChanged();
			} else if (action.equals(Actions.POSITION_SYNC_RESPONSE_INTERRUPT_ACTION)) {
				mProgressDialog.dismiss();
				mProgressDialog.setMessage("");
				String errorMsg = intent.getStringExtra(Extras.SYNC_ERROR_MSG);
				Util.toast(getApplicationContext(), errorMsg);
			}
		}
		
	}
	
	private class MyListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return 1;
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
			ViewHolder holder;
			if (null == convertView) {
				convertView = getLayoutInflater().inflate(R.layout.lable_value_row, null);
				holder = new ViewHolder();
				holder.labelTv = (TextView) convertView.findViewById(R.id.label_tv);
				holder.valueTv = (TextView) convertView.findViewById(R.id.value_tv);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.labelTv.setText(mLabelText[position]);
			holder.valueTv.setText(mValueText[position]);
			return convertView;
		}
		
		private class ViewHolder {
			TextView labelTv;
			TextView valueTv;
		}
	}
}
