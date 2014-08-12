package com.zsxj.pda.ui.client;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.ConstParams.Interface;
import com.zsxj.pda.util.ConstParams.PrefKeys;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.wdt.CashSaleSpec;
import com.zsxj.pda.wdt.Customer;
import com.zsxj.pda.wdt.Shop;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTQuery;
import com.zsxj.pda.wdt.WDTQuery.QueryCallBack;
import com.zsxj.pda.wdt.Warehouse;
import com.zsxj.pdaonphone.R;

public class CashSaleSetActivity extends Activity implements QueryCallBack {
	
	private static final int REQUEST_CODE_CUSTOMER = 0;
	private static final int REQUEST_CODE_ADDRESS = 1;
	
	private RelativeLayout mProgressLayout;
	private LinearLayout mContentLayout;
	private Spinner mShopSp;
	private Spinner mWarehouseSp;
	private Spinner mPriceSp;
	private Button mOkBtn;
	
	private CheckBox mNotRecordCustomerCheck;
	private Button mSearchCustomerBtn;
	private GridLayout mCustomerInfoLayout;
	private EditText mCustomerNameEdit;
	private EditText mNickNameEdit;
	private EditText mTelEdit;
	private EditText mZipCodeEdit;
	private Button mToDistrictAddressBtn;
	private EditText mStreetAddressEdit;
	private EditText mEmailEdit;
	
	private String mProvince;
	private String mCity;
	private String mDistrict;
	
	private int mWhichShop;
	private int mWhichWarehouse; 
	
	private Shop[] mShops;
	private Warehouse[] mWarehouses;
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
                @Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HandlerCases.NO_CONN:
				Util.toast(getApplicationContext(), R.string.no_conn);
				mProgressLayout.setVisibility(View.GONE);
				break;
			case HandlerCases.PREPARE_QUERY_ERROR:
				Log.e("CashSaleSetActivity", 
						getString(R.string.prepare_query_error));
				Util.toast(getApplicationContext(), R.string.prepare_query_error);
				mProgressLayout.setVisibility(View.GONE);
				break;
			case HandlerCases.QUERY_FAIL:
				Util.toast(getApplicationContext(), R.string.query_suppliers_fail);
				mProgressLayout.setVisibility(View.GONE);
				break;
			case HandlerCases.EMPTY_RESULT:
				Util.toast(getApplicationContext(), "empty result ");
				mProgressLayout.setVisibility(View.GONE);
				break;
			case HandlerCases.QUERY_SUCCESS:
				onQueryShopsSuccess();
				break;
			case HandlerCases.QUERY_2_FAIL:
				Util.toast(getApplicationContext(), R.string.query_warehouse_fail);
				mProgressLayout.setVisibility(View.GONE);
				break;
			case HandlerCases.QUERY_2_SUCCESS:
				onQueryWarehousesSuccess();
				break;
			default:
				break;
			}
		}
		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cash_sale_set_activity);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.cash_sale);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mProgressLayout = (RelativeLayout) findViewById(R.id.progress_layout);
		mContentLayout = (LinearLayout) findViewById(R.id.content_layout);
		mShopSp = (Spinner) findViewById(R.id.shop_sp);
		mWarehouseSp = (Spinner) findViewById(R.id.warehouse_sp);
		mPriceSp = (Spinner) findViewById(R.id.price_sp);
		mNotRecordCustomerCheck = (CheckBox)  findViewById(R.id.not_record_customer_check);
		mSearchCustomerBtn = (Button) findViewById(R.id.search_customer_btn);
		mCustomerInfoLayout = (GridLayout) findViewById(R.id.customer_info_layout);
		mCustomerNameEdit = (EditText) findViewById(R.id.customer_name_edit);
		mNickNameEdit = (EditText) findViewById(R.id.nick_name_edit);
		mTelEdit = (EditText) findViewById(R.id.tel_edit);
		mZipCodeEdit = (EditText) findViewById(R.id.zip_code_edit);
		
		mToDistrictAddressBtn = (Button) findViewById(R.id.to_district_address_btn);
		mToDistrictAddressBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(CashSaleSetActivity.this, AddressSelectActivity.class);
				startActivityForResult(intent, REQUEST_CODE_ADDRESS);
			}
		});
		mStreetAddressEdit = (EditText) findViewById(R.id.street_address_edit);
		
		mEmailEdit = (EditText) findViewById(R.id.email_edit);
		
		mNotRecordCustomerCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				showCustomerInfo(!isChecked);
			}
		});
		
		mSearchCustomerBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(CashSaleSetActivity.this, SearchCustomerActivity.class);
				startActivityForResult(intent, REQUEST_CODE_CUSTOMER);
			}
		});
		
		Util.initSpinner(this, mPriceSp, R.layout.simple_spinner_dropdown_item, 
			CashSaleSpec.getPrices());
		final SharedPreferences sharedPref = getSharedPreferences(Globals.getUserPrefsName(), Context.MODE_PRIVATE);
		int defaultPrice = sharedPref.getInt(PrefKeys.DEFAULT_PRICE, 0);
		mPriceSp.setSelection(defaultPrice);
		Globals.setWhichPrice(defaultPrice);
		mPriceSp.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				Globals.setWhichPrice(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		if (Globals.getWhichPrice() == -1) {
			Globals.setWhichPrice(0);
		}
		mOkBtn = (Button) findViewById(R.id.ok_btn);
		
		WDTQuery.getinstance().queryShops(this, this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mPriceSp.setSelection(Globals.getWhichPrice());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.cash_sale_set_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.set_default_shop_action:
			setDefaultShop();
			return true;
		case R.id.set_default_price_action:
			setDefaultPrice();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void setDefaultShop() {
		if (mShops == null) return;
		Context context = CashSaleSetActivity.this;
        final SharedPreferences sharedPref = context.getSharedPreferences(
    		Globals.getUserPrefsName(), Context.MODE_PRIVATE);
		int defaultShop = sharedPref.getInt(PrefKeys.DEFAULT_SHOP, 0);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.default_shop)
			.setSingleChoiceItems(getShopNames(), defaultShop, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
	                int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
	                SharedPreferences.Editor editor = sharedPref.edit();
	                editor.putInt(PrefKeys.DEFAULT_SHOP, selectedPosition);
	                editor.commit();
				}
			})
			.show();
	}
	
	private void setDefaultPrice() {
		Context context = CashSaleSetActivity.this;
        final SharedPreferences sharedPref = context.getSharedPreferences(
    		Globals.getUserPrefsName(), Context.MODE_PRIVATE);
		int defaultPrice = sharedPref.getInt(PrefKeys.DEFAULT_PRICE, 0);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.default_price)
			.setSingleChoiceItems(CashSaleSpec.getPrices(), defaultPrice, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
	                int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
	                SharedPreferences.Editor editor = sharedPref.edit();
	                editor.putInt(PrefKeys.DEFAULT_PRICE, selectedPosition);
	                editor.commit();
				}
			})
			.show();
	}
	
	private String[] getShopNames() {
		String[] shopNames = new String[mShops.length];
		for (int i = 0; i < mShops.length; i++) {
			shopNames[i] = mShops[i].shopName;
		}
		return shopNames;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (REQUEST_CODE_CUSTOMER == requestCode && RESULT_OK == resultCode) {
			String customerName = data.getStringExtra(Extras.CUSTOMER_NAME);
			String tel = data.getStringExtra(Extras.TEL);
			String nickName = data.getStringExtra(Extras.NICK_NAME);
			String zipCode = data.getStringExtra(Extras.ZIP_CODE);
			mProvince = data.getStringExtra(Extras.PROVINCE);
			if (null == mProvince) {
				mProvince = "";
			}
			mCity = data.getStringExtra(Extras.CITY);
			if (null == mCity) {
				mCity = "";
			}
			mDistrict = data.getStringExtra(Extras.DISTRICT);
			if (null == mDistrict) {
				mDistrict = "";
			}
			String streetAddress = data.getStringExtra(Extras.STREET_ADDRESS);
			String email = data.getStringExtra(Extras.EMAIL);
			
			mCustomerNameEdit.setText(customerName);
			mTelEdit.setText(tel);
			mNickNameEdit.setText(nickName);
			mZipCodeEdit.setText(zipCode);
			mToDistrictAddressBtn.setText(mProvince + " " + mCity + " " + mDistrict); 
			mStreetAddressEdit.setText(streetAddress);
			mEmailEdit.setText(email);
			
		} else if (REQUEST_CODE_ADDRESS == requestCode && RESULT_OK == resultCode) {
			mProvince = data.getStringExtra(Extras.PROVINCE);
			mCity = data.getStringExtra(Extras.CITY);
			mDistrict = data.getStringExtra(Extras.DISTRICT);
			
			mToDistrictAddressBtn.setText(mProvince + mCity + mDistrict);
		}
	}
	
	private void showCustomerInfo(boolean show) {
		if (show) {
			mSearchCustomerBtn.setVisibility(View.VISIBLE);
			mCustomerInfoLayout.setVisibility(View.VISIBLE);
		} else {
			mSearchCustomerBtn.setVisibility(View.GONE);
			mCustomerInfoLayout.setVisibility(View.GONE);
		}
	}
	
	private void onQueryShopsSuccess() {
		ArrayAdapter<Shop> adapter = new ArrayAdapter<Shop>(this, 
				R.layout.simple_spinner_dropdown_item, mShops);
		mShopSp.setAdapter(adapter);
		final SharedPreferences sharedPref = getSharedPreferences(Globals.getUserPrefsName(), Context.MODE_PRIVATE);
		int defaultShop = sharedPref.getInt(PrefKeys.DEFAULT_SHOP, 0);
		mShopSp.setSelection(defaultShop, false);
		mWhichShop = defaultShop;
		mShopSp.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				mWhichShop = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		WDTQuery.getinstance().queryInterfaceWarehouses(this, this, Interface.getInterfaceId());
	}
	
	private void onQueryWarehousesSuccess() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
				R.layout.simple_spinner_dropdown_item, Warehouse.getNames(mWarehouses));
		mWarehouseSp.setAdapter(adapter);
		mWarehouseSp.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				mWhichWarehouse = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		mOkBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				goToList();
			}
		});
		
		mProgressLayout.setVisibility(View.GONE);
		mContentLayout.setVisibility(View.VISIBLE);
	}
	
	private void goToList() {
		Globals.setWhichShop(mWhichShop);
		Globals.setShopName(mShops[mWhichShop].shopName);
		Globals.setWhichWarehouse(mWhichWarehouse);
		Globals.setModuleUseWarehouseId(mWarehouses[mWhichWarehouse].warehouseId);
		Globals.setCashSaleWarehouseNO(mWarehouses[mWhichWarehouse].warehouseNO);
		Intent intent = new Intent(getBaseContext(), 
			CashSaleGoodsListActivity.class);
		
		if (!mNotRecordCustomerCheck.isChecked()) {
			String customerName = mCustomerNameEdit.getText().toString();
			String tel = mTelEdit.getText().toString();
			String nickName = mNickNameEdit.getText().toString();
			if (TextUtils.isEmpty(customerName) || TextUtils.isEmpty(tel) || TextUtils.isEmpty(nickName)) {
				Util.toast(getApplicationContext(), R.string.customer_field_empty_toast);
				return;
			}
			String zipCode = mZipCodeEdit.getText().toString();
			String streetAddress = mStreetAddressEdit.getText().toString();
			String email = mEmailEdit.getText().toString();
			Customer customer = new Customer(-1, nickName, customerName, tel, 
				zipCode, mProvince, mCity, mDistrict, streetAddress, email);
			Globals.setCustomer(customer);
		}
		
		startActivity(intent);
	}

	@Override
	public void onQuerySuccess(Object qr) {
		if (null == qr) {
			mHandler.sendEmptyMessage(HandlerCases.EMPTY_RESULT);
			return;
		}
		
		if (Shop[].class.isInstance(qr)) {
			mShops = (Shop[]) qr;
			mHandler.sendEmptyMessage(HandlerCases.QUERY_SUCCESS);
		} else if (Warehouse[].class.isInstance(qr)) {
			mWarehouses = (Warehouse[]) qr;
			mHandler.sendEmptyMessage(HandlerCases.QUERY_2_SUCCESS);
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
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Globals.setWhichShop(-1);
		Globals.setModuleUseWarehouseId(-1);
		Globals.setWhichPrice(-1);
		Globals.setCustomer(null);
		
//		getContentResolver().delete(CashSaleGoods.CONTENT_URI, null, null);
	}
}
