package com.zsxj.pda.receivers;

import com.zsxj.pda.service.GetPushNotificationService;
import com.zsxj.pda.service.ServicePool;
import com.zsxj.pda.service.StartServicePoolService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		// Start StartServicePoolService 
		context.startService(new Intent(context, StartServicePoolService.class));
		if(!ServicePool.getinstance().isInitReady()) {
			StartServicePoolService.check2InitServicePool(context.getApplicationContext());
		}
		
		// Start push service, get push notification regularly. The push notification may is an ad, or some recommended information.
		Intent service = new Intent(context, GetPushNotificationService.class);
		context.startService(service);
	}
}
