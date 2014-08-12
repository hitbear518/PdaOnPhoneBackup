package com.zsxj.pda.service;

import java.util.Timer;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class GetPushNotificationService extends Service {
	
	private Thread pnsThread = null;
	private Timer pnsTimer = null;
	private final long period = 1000 * 60 * 60;
//	private final long period = 1000 * 10 * 6;	// Test 

	protected Log l = LogFactory.getLog(GetPushNotificationService.class);
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		
		super.onCreate();

		l.debug("GetPushNotificationService onCreate ...................");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		
		super.onStart(intent, startId);
		
		l.debug("GetPushNotificationService onStart ...................");
	}

	public synchronized void initSyncCheckingService(final Intent intent) {
		
		// Update thread 
		if (null != pnsThread) {
			pnsThread.interrupt();
			pnsThread = null;
		}
		pnsThread = new Thread(new SyncCheckingRunnable());
		pnsThread.start();
	}
	 
	public class SyncCheckingRunnable implements Runnable {
		
		public void run() {
			
			// Push notification  
			if (null != pnsTimer) {
				pnsTimer.cancel();
			}
			pnsTimer = new Timer();
			pnsTimer.schedule(new GetPushNotificationTimerTask(GetPushNotificationService.this), /*date*/ 1, period);
		}
	}
	
	@Override
	public synchronized int onStartCommand(Intent intent, int flags, int startId) {

		l.debug("GetPushNotificationService onStartCommand ...................");

		initSyncCheckingService(intent);
		return START_STICKY;
	}

	@Override
	public void onLowMemory() {
		
		l.debug("GetPushNotificationService onLowMemory ...................");
	}
	
	@Override
	public void onDestroy() {
		
		// Cancel timer 
		if (null != pnsTimer) {
			pnsTimer.cancel();
			pnsTimer = null;
		}
		
		// Stop thread 
		if (null != pnsThread) {
			pnsThread.interrupt();
			pnsThread = null;
		}
		
		super.onDestroy();
	}
}
