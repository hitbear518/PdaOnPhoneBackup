package com.zsxj.pda.service;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class StartServicePoolService extends Service {

	protected Log l = LogFactory.getLog(StartServicePoolService.class);
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		
		super.onCreate();
		
		l.debug("StartServicePoolService onCreate ...................");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		
		super.onStart(intent, startId);
		
		l.debug("StartServicePoolService onStart ...................");
	}
	
	public static synchronized void check2InitServicePool(final Context ctx) {
		
		if(!ServicePool.getinstance().isInitReady()) {
			
			try {
				ServicePool.getinstance().init(ctx);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}
	
	@Override
	public synchronized int onStartCommand(Intent intent, int flags, int startId) {

		l.debug("StartServicePoolService onStartCommand ...................");
		
		check2InitServicePool(getApplicationContext());
		return START_STICKY;
	}

	@Override
	public void onLowMemory() {
		
		l.debug("StartServicePoolService onLowMemory ...................");
	}
}