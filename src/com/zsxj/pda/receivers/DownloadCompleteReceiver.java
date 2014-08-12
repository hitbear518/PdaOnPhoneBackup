package com.zsxj.pda.receivers;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

import com.zsxj.pda.util.ConstParams.PrefKeys;

public class DownloadCompleteReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		DownloadManager.Query query = new DownloadManager.Query();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		long downloadId = prefs.getLong(PrefKeys.DOWNLOAD_ID, 0);
		query.setFilterById(downloadId);
		DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		Cursor c = dm.query(query);
		if (c.moveToNext()) {
			int statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
			int status = c.getInt(statusIdx);
			if (DownloadManager.STATUS_SUCCESSFUL == status) {
				Intent promptInstall = new Intent(Intent.ACTION_VIEW);
				promptInstall.setDataAndType(
						dm.getUriForDownloadedFile(downloadId), 
						"application/vnd.android.package-archive");
				promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(promptInstall);
			}
		}
	}
}
