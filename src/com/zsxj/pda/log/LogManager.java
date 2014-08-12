package com.zsxj.pda.log;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import android.content.Context;
import android.os.Environment;


public final class LogManager {
	
	public static String LOG_FILE_DIR = null;//System.getenv("EXTERNAL_STORAGE") +  File.separator + "jiayuan";

	public static int MAX_FILE_SIZE = 1024 * 1024 * 5;
	
	public static int NUM_FILES = 5;
	
	public final static String LOG_FILE_NAME = "datareport%g.txt";
	
	public final static LogManager manager = new LogManager();
	
	private Handler mFileHandler;
	
	private Logger mRootLogger = Logger.global;
	
	private volatile boolean mDebugOutput;
	
	private LogManager() {
	}
	
	public boolean switchLog()
	{
		boolean successful;
		
		if (mDebugOutput)
		{
			mDebugOutput = false;
			//disable log cat
			mRootLogger.setLevel(Level.OFF);
			//disable log file output
			closeFileOutput();
			
			successful = true;
		}
		else
		{
			mDebugOutput = true;
			//Enable log cat
			mRootLogger.setLevel(Level.ALL);
			//Enable log file output
			successful = openFileOutput();
		}
		
		return successful;
	}
	
	private void closeFileOutput()
	{
		if (mFileHandler == null)
		{
			return;
		}
		
		mFileHandler.flush();
		mFileHandler.close();
		
		mRootLogger.removeHandler(mFileHandler);
		
		mFileHandler = null;
			
	}
	
	protected String getSDPath()
	{
		 File sdDir = null;
		 boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED); //�ж�sd���Ƿ����
		 if (sdCardExist)
		 {
			 sdDir = Environment.getExternalStorageDirectory();
		 }
		 return sdDir.toString(); 
	}
	
	private boolean openFileOutput()
	{
		
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
		{
			return false;
		}

		boolean successful = false;

		try {
			LOG_FILE_DIR = getSDPath() + File.separator + "datareport" + File.separator + "log";
			File dir = new File(LOG_FILE_DIR);

			if (!dir.exists())
			{
				if (!dir.isDirectory())
				{
					dir.delete();
				}
				
				dir.mkdirs();
			}
			
			File f = new File(LOG_FILE_DIR);
			if(!f.exists())
				f.mkdirs();
			mFileHandler = new FileHandler(LOG_FILE_DIR + File.separator + LOG_FILE_NAME, MAX_FILE_SIZE, NUM_FILES, true);
			mFileHandler.setEncoding("UTF-8");
			mFileHandler.setFormatter(new Formatter() {

				/*
				 * (non-Javadoc)
				 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
				 */
				public String format(LogRecord r) {
					
					StringBuilder sb = new StringBuilder();
			        sb.append(MessageFormat.format("{0,date,MM-dd HH:mm:ss} ", 
			                new Object[] { new Date(r.getMillis()) }));
			        sb.append(r.getSourceClassName().substring(r.getSourceClassName().lastIndexOf("."))).append("#"); 
			        sb.append(r.getSourceMethodName()).append(" ");
			        sb.append(r.getLevel().getName()).append(": ");
			        sb.append(formatMessage(r)).append("\n");
			        if (null != r.getThrown()) {
			            sb.append("Throwable occurred: ");
			            Throwable t = r.getThrown();
			            PrintWriter pw = null;
			            try {
			                StringWriter sw = new StringWriter();
			                pw = new PrintWriter(sw);
			                t.printStackTrace(pw);
			                sb.append(sw.toString());
			            } finally {
			                if(pw != null){
			                    try {
			                        pw.close();
			                    } catch (Exception e) {
			                        pw = null;
			                    }
			                }
			            }
			        }
					return sb.toString();
				}
			});

			mRootLogger.addHandler(mFileHandler);
			
			successful = true;
			
		} catch (Throwable t) {
			successful = false;
			closeFileOutput();
			android.util.Log.e(LogManager.class.getSimpleName(), "Log Switch on failed.", t);
		}

		return successful;
	}
	
	public void switchOFFBySDUnmounted()
	{
		if (mDebugOutput)
		{
			closeFileOutput();
		}
	}
	
	public void switchONBySDMounted()
	{
		if (mDebugOutput)
		{
			openFileOutput();
		}
	}
	
	public void init(Context ctx)
	{
		//Switch Log ON.
		switchLog();
	}
	
	public boolean isLogging()
	{
		return mDebugOutput;
	}
	
	public Logger createLogger(String name)
	{
		Logger ret = Logger.getLogger(name);
		ret.setParent(mRootLogger);
		return ret;
	}
}
