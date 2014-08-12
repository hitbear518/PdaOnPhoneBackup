package com.zsxj.pda.ui.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.PrefKeys;
import com.zsxj.pda.util.Globals;
import com.zsxj.pdaonphone.R;

public class SettingsActivity extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_activity);

		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.settings);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		String[] from = {
				"label",
				"value"
		};
		int[] to = { 
				R.id.label_tv,
				R.id.value_tv
		};
		
		SharedPreferences prefs = getSharedPreferences(Globals.getUserPrefsName(), MODE_PRIVATE);
		String warehouseName = prefs.getString(PrefKeys.WAREHOUSE_NAME, null);
		List<Map<String, String>> fillMaps = new ArrayList<Map<String, String>>();
		Map<String, String> map = new HashMap<String, String>();
		map.put("label", "当前仓库：");
		map.put("value", warehouseName);
		fillMaps.add(map);
		map = new HashMap<String, String>();
		map.put("label", "数据同步");
		fillMaps.add(map);
		map = new HashMap<String, String>();
		map.put("label", "当前版本：");
		map.put("value", getString(R.string.app_version_name));
		fillMaps.add(map);
		map = new HashMap<String, String>();
		map.put("label", getString(R.string.logout));
		fillMaps.add(map);
		SimpleAdapter adapter = new SimpleAdapter(this, fillMaps, R.layout.lable_value_row, from, to);
		setListAdapter(adapter);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		switch (position) {
		case 0:
			Intent selectWarehouse = new Intent(this, SelectWarehouseActivity.class);
			selectWarehouse.putExtra(Extras.RESET_WAREHOUSE, true);
			startActivity(selectWarehouse);
			break;
		case 1:
			Intent startSync = new Intent(this, SyncDataActivity.class);
			startActivity(startSync);
			break;
		case 2:
			break;
		case 3:
			promptLogout();
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
	
	private void promptLogout() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		View view = inflater.inflate(R.layout.custom_alert_dialog_view, null);
		TextView message = (TextView) view.findViewById(R.id.message);
		message.setText(R.string.confirm_logout);
		
		builder.setView(view);
		final AlertDialog dialog = builder.create();
		
		Button btn_positive = (Button) view.findViewById(R.id.btn_positive);
		Button btn_negative = (Button) view.findViewById(R.id.btn_negative);
		btn_positive.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				logout();
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
	
	private void logout() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PrefKeys.SELLER_NICK, "");
		editor.putString(PrefKeys.USER_NAME, "");
		editor.putString(PrefKeys.PASSWORD, "");
		editor.commit();

		// Go to login activity
		Intent intent = new Intent(this, LoginActivity.class);
		startActivity(intent);

		this.finish();
		MainActivity.getInstance().finish();
	}
}
