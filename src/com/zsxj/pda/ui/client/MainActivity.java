package com.zsxj.pda.ui.client;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.ScanType;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.wdt.WDTQuery;
import com.zsxj.pdaonphone.R;


public class MainActivity extends Activity {
	
	private static final Integer TAG_EMPTY = 0;
	private static final Integer TAG_TITLE = 1;
	
	private String[] TITLES;
	
	private ListView titlesLv;
	
	private static Activity instance;
	
	private static int[] modules = Globals.getModules(Globals.getSellerNick()); 
	
	public static Activity getInstance() {
		return instance;
	};
	
	WDTQuery wdtQuery = new WDTQuery();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.function_list);
		
		TITLES = getResources().getStringArray(R.array.titles);
		
		instance = this;
		setContentView(R.layout.main_activity);
		
		titlesLv = (ListView) findViewById(R.id.titles_lv);
		titlesLv.setAdapter(new MyListAdapter(this));
		int warehouseId = getIntent().getIntExtra(Extras.WAREHOUSE_ID, -1);
		final Intent intent = new Intent();
		intent.putExtra(Extras.WAREHOUSE_ID, warehouseId);
		titlesLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				switch (modules[position]) {
				case 0:
					intent.setClass(getApplicationContext(), ScanAndListActivity.class);
					intent.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_FAST_PD);
					break;
				case 1:
					intent.setClass(getApplicationContext(), ScanAndListActivity.class);
					intent.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_QUERY_SPECS);
					break;
				case 2:
					intent.setClass(getApplicationContext(), 
						FastInExamineGoodsSetActivity.class);
					break;
				case 3:
					intent.setClass(getApplicationContext(), ScanAndListActivity.class);
					intent.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_PICK_GOODS);
					break;
				case 4:
					intent.setClass(getApplicationContext(), ScanAndListActivity.class);
					intent.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_OUT_EXAMINE_GOODS);
					break;
				case 5:
					intent.setClass(getApplicationContext(), CashSaleSetActivity.class);
					break;
				default:
					intent.setClass(getBaseContext(), SettingsActivity.class);
					break;
				}
				startActivity(intent);
			}
		});
	}
	
	@Override
	public void onBackPressed() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		View view = inflater.inflate(R.layout.custom_alert_dialog_view, null);
		TextView message = (TextView) view.findViewById(R.id.message);
		message.setText(R.string.check_quit);
		
		builder.setView(view);
		final AlertDialog dialog = builder.create();
		
		Button btn_positive = (Button) view.findViewById(R.id.btn_positive);
		Button btn_negative = (Button) view.findViewById(R.id.btn_negative);
		btn_positive.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				MainActivity.super.onBackPressed();
			}
		});
		btn_negative.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dialog.cancel();
			}
		});
		
		dialog.show();
	}
	
	private class MyListAdapter extends BaseAdapter {
		
		private final Context mContext;

		public MyListAdapter(Context context) {
			mContext = context;
		}
		
		@Override
		public int getCount() {
			return modules.length;
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
			if (convertView == null || convertView.getTag() == TAG_EMPTY) {
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.simple_list_item_1, parent, false);
				convertView.setTag(TAG_TITLE);
			}
			
			TextView titleTv = (TextView) convertView.findViewById(R.id.item_tv);
			if (99 == modules[position]) {
				titleTv.setText(R.string.settings);
			} else {
				titleTv.setText(TITLES[modules[position]]);	
			}
			return convertView;
		}
		
	}
}
