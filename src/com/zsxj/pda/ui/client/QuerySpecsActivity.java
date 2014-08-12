package com.zsxj.pda.ui.client;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.ConstParams.ScanType;
import com.zsxj.pda.util.Globals;
import com.zsxj.pdaonphone.R;

public class QuerySpecsActivity extends Activity {
	
	private TextView mSpecBarcodeTv;
	private TextView mGoodsNumTv;
	private TextView mGoodsNameTv;
	private TextView mBarcodeTv;
	private TextView mSpecCodeTv;
	private TextView mSpecNameTv;
	private Spinner mPositionNamesSp;
	private TextView mStockTv;
	private Button mStockTransferBtn;
	
	private int mWhichPosition;
	
	private int mSpecId;
	private int[] mPositionIds;
	private String[] mPositionNames;
	private String[] mStocks;
	
	private String mBarcode;
	private int mWhichSpec;
	private boolean mStockTranfered;
	
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HandlerCases.SCAN_RESULT:
				Intent startScan = 
					new Intent(getApplicationContext(), ScanAndListActivity.class);
				startScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startScan.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_QUERY_SPECS);
				startScan.putExtra(Extras.BARCODE, mBarcode);
				startActivity(startScan);
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.query_specs_activity);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.query_goods);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		final View rootView = findViewById(R.id.root);
		rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			
			@Override
			public void onGlobalLayout() {
				int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
				if (heightDiff > 300) {
					getActionBar().hide();
				} else {
					getActionBar().show();
				}
			}
		});
		
		mSpecBarcodeTv = (TextView) findViewById(R.id.spec_barcode_tv);
		String specBarcode = getIntent().getStringExtra(Extras.SPEC_BARCODE);
		mSpecBarcodeTv.setText(specBarcode);
		
		mGoodsNumTv = (TextView) findViewById(R.id.goods_num_tv);
		String goodsNum = getIntent().getStringExtra(Extras.GOODS_NUM);
		mGoodsNumTv.setText(goodsNum);
		
		mGoodsNameTv = (TextView) findViewById(R.id.goods_name_tv);
		String goodsName = getIntent().getStringExtra(Extras.GOODS_NAME);
		mGoodsNameTv.setText(goodsName);
		
		mBarcodeTv = (TextView) findViewById(R.id.barcode_tv);
		String barcode = getIntent().getStringExtra(Extras.BARCODE);
		mBarcodeTv.setText(barcode);
		
		mSpecCodeTv = (TextView) findViewById(R.id.spec_code_tv);
		String specCode = getIntent().getStringExtra(Extras.SPEC_CODE);
		mSpecCodeTv.setText(specCode);
		
		mSpecNameTv = (TextView) findViewById(R.id.spec_name_tv);
		String specName = getIntent().getStringExtra(Extras.SPEC_NAME);
		mSpecNameTv.setText(specName);
		
		mPositionNamesSp = (Spinner) findViewById(R.id.position_names_sp);
		mPositionNames = getIntent().getStringArrayExtra(Extras.POSITION_NAMES);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
				R.layout.simple_spinner_dropdown_item, 
				mPositionNames);
		mPositionNamesSp.setAdapter(adapter);
		mStockTv = (TextView) findViewById(R.id.stock_can_order_btn);
		mStocks = getIntent().getStringArrayExtra(Extras.STOCKS);
		mPositionNamesSp.setOnItemSelectedListener(
				new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						mWhichPosition = position;
						mStockTv.setText(mStocks[mWhichPosition]);				
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
		});
		
		mStockTransferBtn = (Button) findViewById(R.id.stock_transfer_btn);
		mStockTransferBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stockTransfer();
			}
		});
		
		mSpecId = getIntent().getIntExtra(Extras.SPEC_ID, -1);
		mPositionIds = getIntent().getIntArrayExtra(Extras.POSITION_IDS);
		
		mWhichSpec = getIntent().getIntExtra(Extras.WHICH_SPEC, -1);
		mBarcode = getIntent().getStringExtra(Extras.BARCODE);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (mStockTranfered)
			refreshData();
	}
	

	private void stockTransfer() {
		Intent stockTransfer = new Intent(this, StockTransferActivity.class);
		stockTransfer.putExtra(Extras.WAREHOUSE_ID, Globals.getWarehouseId(this));
		stockTransfer.putExtra(Extras.SPEC_ID, mSpecId);
		stockTransfer.putExtra(Extras.POSITION_IDS, mPositionIds);
		stockTransfer.putExtra(Extras.POSITION_NAMES, mPositionNames);
		stockTransfer.putExtra(Extras.STOCKS, mStocks);
		stockTransfer.putExtra(Extras.BARCODE, mBarcode);
		startActivity(stockTransfer);
		mStockTranfered = true;
	}
	
	public void refreshData() {
		Intent startScan = 
			new Intent(getApplicationContext(), ScanAndListActivity.class);
		startScan.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_QUERY_SPECS);
		startScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startScan.putExtra(Extras.BARCODE, mBarcode);
		startScan.putExtra(Extras.WHICH_SPEC, mWhichSpec);
		startActivity(startScan);
	}
}
