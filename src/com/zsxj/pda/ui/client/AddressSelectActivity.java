package com.zsxj.pda.ui.client;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.zsxj.pda.provider.AddressDatabase;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pdaonphone.R;

public class AddressSelectActivity extends Activity {

	private TextView mProvinceTv;
	private TextView mCityTv;
	private ListView mAddressList;

	private AddressDatabase mAddressDb;
	private Cursor mProvinceCursor;
	private Cursor mCityCursor;
	private Cursor mDistrictCursor;

	private static final int LEVEL_PROVINCE = 0;
	private static final int LEVEL_CITY = 1;
	private static final int LEVEL_DISTRICT = 2;
	private int mCurrentLevel = LEVEL_PROVINCE;
	
	private String mProvince;
	private String mCity;
	private String mDistrict;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.address_select_activity);
		
		mAddressList = (ListView) findViewById(R.id.address_list);
		mProvinceTv = (TextView) findViewById(R.id.province_tv);
		mCityTv = (TextView) findViewById(R.id.city_tv);
		mAddressDb = new AddressDatabase(this);
		
		// fill list with provinces and set level to province
		mProvinceCursor = mAddressDb.getProvinces();
		fillList(mProvinceCursor, AddressDatabase.PROVINCE_NAME);
		mCurrentLevel = LEVEL_PROVINCE;
		
		mAddressList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				switch (mCurrentLevel) {
				case LEVEL_PROVINCE:
					mProvinceCursor.moveToPosition(position);
					
					// show province text view
					mProvince = mProvinceCursor.getString(2);
					mProvinceTv.setText(mProvince);
					mProvinceTv.setVisibility(View.VISIBLE);
					
					// fill list with cities
					int provinceId = mProvinceCursor.getInt(1);
					mCityCursor = mAddressDb.getCities(provinceId);
					fillList(mCityCursor, AddressDatabase.CITY_NAME);
					
					// down to city level
					mCurrentLevel = LEVEL_CITY;
					break;
				case LEVEL_CITY:
					mCityCursor.moveToPosition(position);
					
					// show city text view
					mCity = mCityCursor.getString(2);
					mCityTv.setText(mCity);
					mCityTv.setVisibility(View.VISIBLE);
					
					int cityId = mCityCursor.getInt(1);
					mDistrictCursor = mAddressDb.getDistricts(cityId);

					// if no available districts, set result and finish;
					if (mDistrictCursor.getCount() == 0) {
						mDistrict = "";
						setResultAndFinish();
						break;
					}
					
					// fill list with districts
					fillList(mDistrictCursor, AddressDatabase.DISTRICT_NAME);
					
					// down to district level
					mCurrentLevel = LEVEL_DISTRICT;
					break;
				case LEVEL_DISTRICT:
					mDistrictCursor.moveToPosition(position);
					
					mDistrict = mDistrictCursor.getString(2);
					
					// All three level selected
					setResultAndFinish();
					break;
				default:
					break;
				}
			}
		});
	}

	
	private void fillList(Cursor cursor, String colName) {
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(
			AddressSelectActivity.this, 
			android.R.layout.simple_list_item_1, 
			cursor, 
			new String[] {colName}, 
			new int[] {android.R.id.text1}, 
			0
		);
		mAddressList.setAdapter(adapter);
	}
	
	private void setResultAndFinish() {
		mAddressList.setVisibility(View.GONE);
		Intent data = new Intent();
		data.putExtra(Extras.PROVINCE, mProvince);
		data.putExtra(Extras.CITY, mCity);
		data.putExtra(Extras.DISTRICT, mDistrict);
		setResult(RESULT_OK, data);
		finish();
	}
	
	@Override
	public void onBackPressed() {
		switch (mCurrentLevel) {
		case LEVEL_CITY:
			// hide province and city text view
			mCityTv.setVisibility(View.GONE);
			mProvinceTv.setVisibility(View.GONE);
			
			// fill list with provinces
			mProvinceCursor = mAddressDb.getProvinces();
			fillList(mProvinceCursor, AddressDatabase.PROVINCE_NAME);

			// up to province level
			mCurrentLevel = LEVEL_PROVINCE;
			break;
		case LEVEL_DISTRICT:
			// hide city text view
			mCityTv.setVisibility(View.GONE);
			
			// fill list with cities
			fillList(mCityCursor, AddressDatabase.CITY_NAME);
			
			// up to city level
			mCurrentLevel = LEVEL_CITY;
			break;
		default:
			super.onBackPressed();
			break;
		}
		
	}
}
