package com.zsxj.pda.ui.client;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;

import com.zsxj.pda.provider.DbContracts.PositionTable;
import com.zsxj.pda.provider.DbHelper;
import com.zsxj.pda.provider.ProviderContract.Positions;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.ConstParams.PrefKeys;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTUpdate;
import com.zsxj.pda.wdt.WDTUpdate.UpdateCallBack;
import com.zsxj.pdaonphone.R;

public class NewPositonActivity extends Activity implements UpdateCallBack{

	private EditText mSearchEdit;
	private RelativeLayout mProgressLayout;
	private ListView mPositionsLv;
	
	private int mWarehouseId;
	private int mSpecId;
		
	private DbHelper mDbHelper;
	private SimpleCursorAdapter mAdapter;
	
	private int mWhichPosition;
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HandlerCases.NO_CONN:
				Util.toast(getApplicationContext(), R.string.no_conn);
				break;
			case HandlerCases.PREPARE_UPDATE_FAIL:
				Util.toast(getApplicationContext(), R.string.new_position_fail);
				break;
			case HandlerCases.UPDATE_FAIL:
				Util.toast(getApplicationContext(), R.string.new_position_fail);
				break;
			case HandlerCases.UPDATE_SUCCESS:
				Util.toast(getApplicationContext(), R.string.update_success);
				finish();
				break;
			default:
				break;
			}
		}
	};
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_position_activity);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.new_position);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mSearchEdit = (EditText) findViewById(R.id.search_edit);
		mSearchEdit.addTextChangedListener(new TextWatcher() {	
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				new FillListTask().execute(s.toString());
//				fillList(s.toString());
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		
		mProgressLayout = (RelativeLayout) findViewById(R.id.progress_layout);
		
		mPositionsLv = (ListView) findViewById(R.id.positions_lv);
		String[] from = {
			Positions.COLUMN_NAME_POSITION_NAME
		};
		int[] to = {
			android.R.id.text1
		};
		mAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, 
				from, to, 0);
		mPositionsLv.setAdapter(mAdapter);
		mPositionsLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mWhichPosition = position;
				confirmUpdate();
			}
		});
		
		SharedPreferences prefs = getSharedPreferences(Globals.getUserPrefsName(), MODE_PRIVATE);
		mWarehouseId = prefs.getInt(PrefKeys.WAREHOUSE_ID, -1);
		mSpecId = getIntent().getIntExtra(Extras.SPEC_ID, -1);
		
		mDbHelper = new DbHelper(this, Globals.getDbName());
		new FillListTask().execute("");
//		fillList("");
	}
	
	
	private void confirmUpdate() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.confirm_new_position)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Cursor c = (Cursor) mPositionsLv.getItemAtPosition(mWhichPosition);
					int positionId = c.getInt(
							c.getColumnIndex(Positions.COLUMN_NAME_POSITION_ID));
					new WDTUpdate().newPosition(mWarehouseId, mSpecId,
							positionId, NewPositonActivity.this);					
				}
			}).show();
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
	
	@Override    
	protected void onDestroy() {          
	    super.onDestroy();
	    if (mAdapter != null && mAdapter.getCursor() != null) {    
	        mAdapter.getCursor().close();    
	    }
	    if (mDbHelper != null) {
	    	mDbHelper.close();
	    }
	}  
	
	@Override
	public void onUpdateSuccess() {
		Intent data = new Intent();
		Cursor c = (Cursor) mPositionsLv.getItemAtPosition(mWhichPosition);
		int positionId = c.getInt(
				c.getColumnIndex(Positions.COLUMN_NAME_POSITION_ID));
		String positionName = c.getString(
				c.getColumnIndex(Positions.COLUMN_NAME_POSITION_NAME));
		data.putExtra(Extras.POSITION_ID, positionId);
		data.putExtra(Extras.POSITION_NAME, positionName);
		setResult(RESULT_OK, data);
		finish();
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
	
	private class FillListTask extends AsyncTask<String, Void, Cursor> {
		
		@Override
		protected void onPreExecute() {
			mProgressLayout.setVisibility(View.VISIBLE);
			mPositionsLv.setVisibility(View.GONE);
		}

		@Override
		protected Cursor doInBackground(String... params) {
			String[] projection = {
					Positions._ID,
					Positions.COLUMN_NAME_POSITION_ID,
					Positions.COLUMN_NAME_POSITION_NAME
			};
			String selection = 
					Positions.COLUMN_NAME_WAREHOUSE_ID + "=? AND "
					+ Positions.COLUMN_NAME_POSITION_NAME + " LIKE ?";
			String[] selectionArgs = {
					String.valueOf(mWarehouseId),
					"%" + params[0] + "%"
			};
			String orderBy = Positions.COLUMN_NAME_POSITION_NAME;
			SQLiteDatabase db = mDbHelper.getReadableDatabase();
			Cursor cursor = db.query(PositionTable.TABLE_NAME, projection, selection, selectionArgs, 
					null, null, orderBy, "100");
			return cursor;
		}
		
		@Override
		protected void onPostExecute(Cursor result) {
			mAdapter.changeCursor(result);
			mProgressLayout.setVisibility(View.GONE);
			mPositionsLv.setVisibility(View.VISIBLE);
		}
	}
}
