package com.zsxj.pda.ui.client;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.TextView;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;
import com.zsxj.pda.service.GetPushNotificationService;
import com.zsxj.pda.service.ServicePool;
import com.zsxj.pda.service.StartServicePoolService;
import com.zsxj.pda.util.ServiceCfgFileCopy;
import com.zsxj.pdaonphone.R;

public class SplashActivity extends Activity {
	
	protected Log l = LogFactory.getLog(SplashActivity.class);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setUpViews();
		setUpListeners();
		initData();
	}

	private void setUpViews() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.splash_activity);
		
		TextView versionTv = (TextView) findViewById(R.id.version_tv);
		String appVersion = getString(R.string.app_version_name);
		versionTv.setText(getString(R.string.version_prompt, appVersion));
	}

	private void setUpListeners() {
	}

	private void initData() {
	}
	
	protected void goToMain() {
		
		// Start main activty 
		Intent intent = null;
		intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		synchronized (this) {
			try {
				wait(1000);
			} catch (InterruptedException e) {
			}
		}
		
		startActivity(intent);
		finish();
	}
	
	private void goToLogin() {
		// Start login activity
		Intent intent = null;
		intent = new Intent(this, LoginActivity.class);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		startActivity(intent);
		finish();
	}
	
	class InitManager implements Runnable {
		
		public void run() {
			
			synchronized (this) {
				try {
					wait(500);
				} catch (InterruptedException e) {
				}
			}
			
			synchronized (this) {
				
				Looper.prepare();
				
				ServiceCfgFileCopy scfc = new ServiceCfgFileCopy();
				try {
					scfc.copyFile(SplashActivity.this);
				} catch (IOException e) {
					l.error("error on copy service config file" + e);
					finish();
				}

				// Start service pool service 
				startService(new Intent(SplashActivity.this, StartServicePoolService.class));
				if(!ServicePool.getinstance().isInitReady()) {
					StartServicePoolService.check2InitServicePool(SplashActivity.this.getApplicationContext());
				}
				
				// Start push service, get push notification regularly. The push notification may is an ad, or some recommended information.
				Intent service = new Intent(SplashActivity.this, GetPushNotificationService.class);
				startService(service);
				
				goToLogin();
			}
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		
		super.onResume();
		
		new Thread(new InitManager()).start();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		
		if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			return true;
		}
		return false;
	}
}
