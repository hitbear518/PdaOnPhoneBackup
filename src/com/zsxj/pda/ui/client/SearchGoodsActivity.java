package com.zsxj.pda.ui.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.ScanType;
import com.zsxj.pdaonphone.R;

public class SearchGoodsActivity extends Activity{
	
	private EditText mSearchGoodsEdit;
	private Button mSearchBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_goods_activity);
		
		mSearchGoodsEdit = (EditText) findViewById(R.id.search_goods_edit);
		mSearchBtn = (Button) findViewById(R.id.search_btn);
		
		mSearchBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				search();
			}
		});
	}
	
	private void search() {
		String searchTerm = mSearchGoodsEdit.getText().toString();
		
		if (TextUtils.isEmpty(searchTerm)) return;
		
		Intent intent = new Intent(this, ScanAndListActivity.class);
		intent.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_CASH_SALE_BY_TERM);
		intent.putExtra(Extras.SEARCH_TERM, searchTerm);
		startActivity(intent);
	}
}
