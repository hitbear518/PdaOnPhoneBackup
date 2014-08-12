package com.zsxj.pda.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.zsxj.pda.ui.client.ScanAndListActivity;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.ScanType;

public class ViewUtils {

	public static void showSearchDialog(final Context context) {
		showSearchDialog(context, 0);
	}
	
	public static void showSearchDialog(final Context context, final int flags) {
		final EditText searchTermEdit = new EditText(context);
		searchTermEdit.setHint("货品名称、货品编号或商家编码");
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		searchTermEdit.setLayoutParams(params);
		AlertDialog.Builder buidler = new AlertDialog.Builder(context);
		AlertDialog dialog = buidler.setTitle("搜索货品")
			.setView(searchTermEdit)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String searchTerm = searchTermEdit.getText().toString();
					
					if (TextUtils.isEmpty(searchTerm)) return;
					
					search(context, searchTerm, flags);
				}
			})
			.create();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.show();
	}
	
	private static void search(Context context, String searchTerm, int flags) {
		Intent intent = new Intent(context, ScanAndListActivity.class);
		intent.setFlags(flags);
		intent.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_CASH_SALE_BY_TERM);
		intent.putExtra(Extras.SEARCH_TERM, searchTerm);
		context.startActivity(intent);
	}
}
