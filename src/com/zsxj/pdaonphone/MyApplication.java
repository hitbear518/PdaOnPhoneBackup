package com.zsxj.pdaonphone;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

public class MyApplication extends Application {
	
	private static final String LOG_DIR = Environment
		.getExternalStorageDirectory().getAbsolutePath() + "/pda/log";
	private static final String LOG_NAME = getCurrentDateString() + ".txt";
	
	@Override
	public void onCreate() {
		super.onCreate();
		Thread.setDefaultUncaughtExceptionHandler(handler);
	}
	
	UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {
		
		private UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
		
		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			writeErrorLog(ex);
			
			defaultUEH.uncaughtException(thread, ex);
		}
	};
	
	protected void writeErrorLog(Throwable ex) {
		String info = null;
		ByteArrayOutputStream baos = null;
		PrintStream printStream = null;
		try {
			baos = new ByteArrayOutputStream();
			printStream = new PrintStream(baos);
			ex.printStackTrace(printStream);
			byte[] data = baos.toByteArray();
			info = new String(data);
			data = null;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (printStream != null) {
					printStream.close();
				}
				if (baos != null) {
					baos.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Log.d("pda", "崩溃信息\n" + info);
		File dir = new File(LOG_DIR);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File file = new File(dir, LOG_NAME);
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(file, true);
			fileOutputStream.write(info.getBytes());
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String getCurrentDateString() {
		String result = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
		Date nowDate = new Date();
		result = sdf.format(nowDate);
		return result;
	}

	
}
