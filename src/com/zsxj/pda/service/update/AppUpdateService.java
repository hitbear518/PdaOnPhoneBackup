package com.zsxj.pda.service.update;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;
import com.zsxj.pda.service.ServicePool;
import com.zsxj.pda.ui.client.MainActivity;
import com.zsxj.pda.util.HttpUtils;
import com.zsxj.pda.util.Util;
import com.zsxj.pdaonphone.R;

public class AppUpdateService {
	
	NotificationManager notifMgr = null;
	Notification notification = null;
	boolean needUpdate = false;
	protected Context ctx;
	protected UpdateRecord ur;
	protected final String RECORD_FILE_NAME = "AppUpdateService.class";
	protected boolean inProcess;	// marks if the upgrade in process
	protected boolean stoped;
	
	public static final int APP_UPGRADE_NOTIFICATION_ID = 0;
	
	protected Log l = LogFactory.getLog(AppUpdateService.class);
	
	public void init(Context ctx) {
		
		this.ctx = ctx;
		ur = readStore();
		if (null != ur) {
			File f = ctx.getFileStreamPath(ur.getFileName());
			if (null != f) {
				if (Util.getAppVersionName(ctx).equals(ur.ver)) {
					ctx.deleteFile(ur.getFileName());
					ServicePool.getinstance().getFileStore().retireFile(RECORD_FILE_NAME);
					ur = null;
				}
			}
		}
		
		notifMgr = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	/**
	 * to stop upgrade
	 */
	public void stop() {
		
		notifMgr.cancel(APP_UPGRADE_NOTIFICATION_ID);
		stoped = true;
	}
	
	/**
	 * generate UpdateRecord
	 * @param fileName
	 * @param ver
	 * @param len	Length 
	 * @return	instance or 'null' if failed
	 */
	protected UpdateRecord genUpateRecord(String url, String ver, long len) {
		
		UpdateRecord val = new UpdateRecord();	// descrips the given file
		val.ver = ver;
		val.fileLength = len;
		if (null != url) {
			try {
				val.url = new URL(url);
			} catch (MalformedURLException e) {
				l.error("", e);
				return null;
			}
		}
		
		return val;
	}
	
	
	/**
	 * to get new ver apk file to install
	 * If the specified new apk file has been downloaded then only to notify user to install
	 * @param url
	 * @param type
	 * @param length	Expected length of the file
	 * @param ver
	 */
	public void update(String url, int type, String ver, long length) {
		
		if (inProcess)
			return;
		
		inProcess = true;
		
		UpdateRecord tmp = genUpateRecord(url, ver, length);
		UpdateRecord newUr = null;
		if (null != ur) {
			if (tmp.isNewerThen(ur))
				newUr = tmp;
		} else {
			newUr = tmp;
		}
		
		if (null != newUr) {
			if (null != ur)
				ctx.deleteFile(ur.getFileName());
			ur = newUr;
			storeRecord(ur);
		}
		
		onUpdateApp(ur);
	}
	
	/**
	 * read update record store 
	 * @return
	 */
	protected UpdateRecord readStore() {
		
		File fs = ctx.getFileStreamPath(RECORD_FILE_NAME);
		
		InputStream is = null;
		try {
			is = new FileInputStream(fs);
		} catch (FileNotFoundException e1) {
		}
		
		UpdateRecord rec = null;
		if (null != is) {
			try {
				ObjectInputStream ois = new ObjectInputStream(is);
				rec = (UpdateRecord)ois.readObject();
			} catch (Exception e) {
				
			} finally {
				Util.close(is);
			}
		}
		
		return rec;
	}
	
	/**
	 * store update record
	 * @param src
	 */
	protected void storeRecord(UpdateRecord src) {
		
		FileOutputStream os = null;
		ObjectOutputStream writer = null;
		
		try {
			ctx.deleteFile(RECORD_FILE_NAME);
			os = ctx.openFileOutput(RECORD_FILE_NAME, Context.MODE_WORLD_WRITEABLE);
			writer = new ObjectOutputStream(os);
			writer.writeObject(src);
		} catch (IOException e) {
			
		} finally {
			Util.close(os);
			Util.close(writer);
		}
	}

	private void updateNotification(int percent) {
		
		int temp = 0;
		if (temp < percent) {
			
			notification.contentView.setProgressBar(R.id.progress, 100, percent, false);
			String str = String.format("已下载%1$d%%", percent);
			notification.setLatestEventInfo(ctx, "旺店通数据统计", str, null);
			notifMgr.notify(APP_UPGRADE_NOTIFICATION_ID, notification);
		}
		temp = percent;
	}

	/**
	 * Check & download specified file
	 * @param ur
	 */
	private void onUpdateApp(UpdateRecord ur) {
		
		notification = new Notification(android.R.drawable.stat_sys_download,
										"旺店通数据统计", 
										System.currentTimeMillis());
		notification.contentView = new RemoteViews(ctx.getPackageName(), R.layout.ntf_progress);
		
		FileDownloadTask fileDownload = new FileDownloadTask();
		fileDownload.execute(ur);
	}

	private void onInstallApp(File file) {
		
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
		ctx.startActivity(intent);
	}
	
	private void onDownloadFailed() {
		Toast.makeText(ctx, "下载新版本失败", Toast.LENGTH_LONG).show();
	}

	public class FileDownloadTask extends AsyncTask<UpdateRecord, Integer, File> {
		
		@Override
		protected File doInBackground(UpdateRecord... params) {
			File f = saveFileToSDCard(params[0]);
			return f;
		}

		/**
		 * write a given data to the end of file
		 * @param data
		 * @param len
		 * @param file
		 * 
		 * @throws FileNotFoundException, IOException
		 */
		protected void appendToFile(byte[] data, int len, String file)throws FileNotFoundException, IOException {
			
			FileOutputStream fos = ctx.openFileOutput(file, Context.MODE_WORLD_READABLE | Context.MODE_APPEND);
			try {
				fos.write(data, 0, len);
			} finally {
				Util.close(fos);
			}
		}
		
		/**
		 * Get the length of file in local
		 * @return
		 */
		protected long getSavedFilelength() {
			
			File f = ctx.getFileStreamPath(ur.getFileName());
			if(f.exists()) {
				return f.length();
			} else {
				return 0;
			}
		}
		
		private File saveFileToSDCard(UpdateRecord ur) {
			
			long downloaded = getSavedFilelength();
			
			if (downloaded >= ur.fileLength) {
				return ctx.getFileStreamPath(ur.getFileName());
			}
			
			BufferedInputStream bis = null;
			try {
				HttpURLConnection conn = (HttpURLConnection) HttpUtils.openConnection(ctx.getApplicationContext(), ur.url);
				conn.setRequestProperty("Range", "bytes=" + downloaded + "-");
				bis = new BufferedInputStream(conn.getInputStream());
				byte[] buffer = new byte[1024 * 4];
				int size = 0;
				int tempProgress = 0;
				String fileName = ur.getFileName();
				while ((size = bis.read(buffer)) != -1) {
					appendToFile(buffer, size, fileName);
					downloaded += size;
					int percent = (int) ((float) downloaded * 100 / ur.fileLength);
					if (tempProgress < percent) {
						tempProgress = percent;
						publishProgress(percent);
					}
					
					if(stoped)
						break;
				}
			} catch (IOException e) {
				l.error("error happened when downloading upgrade file: " + ur.url.toString(), e);
				return null;
			} finally {
				Util.close(bis);
			}
			
			if(stoped)
				return null;
			else
				return ctx.getFileStreamPath(ur.getFileName());
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			updateNotification(progress[0]);
		}

		@Override
		protected void onPostExecute(File file) {
			
			if (null != file) {
				notifyDownloadDone();
				onInstallApp(file);
			} else {
				if(!stoped)
					onDownloadFailed();
			}
			
			inProcess = false;
		}
	}

	private void notifyDownloadDone() {
		
		notification.icon = android.R.drawable.stat_sys_download_done;
		notifMgr.notify(APP_UPGRADE_NOTIFICATION_ID, notification);
		stop();
	}
	
	/**
	 * update intent
	 * @param n
	 */
	protected void updateIntent(Notification n) {
		
		n.contentIntent = PendingIntent.getActivity(ctx, 
													0, 
													new Intent(ctx, MainActivity.class),
													PendingIntent.FLAG_CANCEL_CURRENT);
	}
	
	/**
	 * check if app upgrade in process
	 * @return
	 */
	public boolean isProcess() {
		
		return inProcess;
	}

}
