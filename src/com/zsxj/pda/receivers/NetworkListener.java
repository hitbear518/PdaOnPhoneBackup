package com.zsxj.pda.receivers;

import java.util.HashSet;
import java.util.Set;

import com.zsxj.pda.service.ServicePool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class NetworkListener extends BroadcastReceiver {
	
	// event names for those events to fire 
	public static final String NETWORK_CONNECTION_ON = "NETWORK_CONNECTION_ON";
	public static final String NETWORK_CONNECTION_OFF = "NETWORK_CONNECTION_OFF";
	protected String connecitonStatus;	// marks current connection status
	
	
	// connection type define
	public static final String NETWORK_CONNECTION_TYPE_GPRS = "NETWORK_CONNECTION_TYPE_GPRS";
	public static final String NETWORK_CONNECTION_TYPE_WIFI = "NETWORK_CONNECTION_TYPE_WIFI";
	protected String connecitonType;	// marks how this device access network currently
	
	// GPRS connection type, eg: 3g, edge...
	public static final String CONNECTION_SPEED_LOW = "MOBILE_CONNECTION_TYPE_LOW_SPEED";
	public static final String CONNECTION_SPEED_HEIGHT = "MOBILE_CONNECTION_TYPE_HIGH_SPEED";
	protected String connecitonSpeed;	// marks current connection speed
	
	// GPRS connection type, eg: 3g, edge...
	public static final String CONNECTION_APN_TYPE_WAP = "CONNECTION_APN_TYPE_WAP";
	public static final String CONNECTION_APN_TYPE_NO_WAP = "CONNECTION_APN_TYPE_NO_WAP";
	protected String connecitonApnType;	// current APN type
		
	/**
	 * parse current network status 
	 */
	protected Set<Integer> highSpeed = new HashSet<Integer>();
	{
		highSpeed.add(TelephonyManager.NETWORK_TYPE_UMTS);
		highSpeed.add(TelephonyManager.NETWORK_TYPE_1xRTT);
		highSpeed.add(TelephonyManager.NETWORK_TYPE_EDGE);
		highSpeed.add(TelephonyManager.NETWORK_TYPE_EVDO_0);
		highSpeed.add(TelephonyManager.NETWORK_TYPE_EVDO_A);
		highSpeed.add(TelephonyManager.NETWORK_TYPE_HSDPA);
		highSpeed.add(TelephonyManager.NETWORK_TYPE_HSPA);
		highSpeed.add(TelephonyManager.NETWORK_TYPE_HSUPA);
	}
	
	protected void parseNetworkStatus(NetworkInfo ni) {
		
		if (null == ni)
			throw new RuntimeException("ni can not be null");
		
		{	// parse connection type
			if (ni.getType() == ConnectivityManager.TYPE_WIFI)
				connecitonType = NETWORK_CONNECTION_TYPE_WIFI;
			else
				connecitonType = NETWORK_CONNECTION_TYPE_GPRS;
			// in Android 2.1 there is no other TYPE_ defines, like: TYPE_DUN
		}
		
		{	// parese connection speed
			if (highSpeed.contains(ni.getSubtype()) || (connecitonType == NETWORK_CONNECTION_TYPE_WIFI))
				connecitonSpeed = CONNECTION_SPEED_HEIGHT;
			else
				connecitonSpeed = CONNECTION_SPEED_LOW;
		}
		
		{
			String ei = ni.getExtraInfo();
			if (ei != null)
			{
				if (ei.contains("wap") || ei.contains("WAP"))
					connecitonApnType = CONNECTION_APN_TYPE_WAP;
				else
					connecitonApnType = CONNECTION_APN_TYPE_NO_WAP;
			}
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		checkConnectionStatus(context, true);
	}
	
	/**
	 * check current connection status
	 * @param context
	 */
	public void checkConnectionStatus(Context ctx, boolean fireEvents) {
		
		System.out.println("NetworkListener checkConnectionStatus... ...");
		ConnectivityManager connectivityManager = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
		
		if (null != activeNetInfo) {

			System.out.println("null != activeNetInfo");
			parseNetworkStatus(activeNetInfo);
			connecitonStatus = NETWORK_CONNECTION_ON;
			
			if(fireEvents) {
				System.out.println("fireEvents");
				if (null == ServicePool.getinstance().getEventCenter()) {
					System.out.println("null == ServicePool.getinstance().getEventCenter()");
				} else {
					System.out.println("null != ServicePool.getinstance().getEventCenter()");
				}
				ServicePool.getinstance().getEventCenter().fireEvent(this, NETWORK_CONNECTION_ON);
					
//				// Start sync checking service 
//				Intent intent = new Intent(ctx, SyncCheckingService.class);
//				intent.putExtra("intent", "NetworkListener");
//				intent.putExtra("status", connecitonStatus);
//				intent.putExtra("speed", connecitonSpeed);
//				ctx.startService(intent);
			}
			
		} else {

			System.out.println("null == activeNetInfo");
			connecitonType = null;
			connecitonSpeed = null;
			connecitonStatus = NETWORK_CONNECTION_OFF;
			
			if(fireEvents) {
				System.out.println("fireEvents");
				if (null == ServicePool.getinstance().getEventCenter()) {
					System.out.println("null == ServicePool.getinstance().getEventCenter()");
				} else {
					System.out.println("null != ServicePool.getinstance().getEventCenter()");
				}
				ServicePool.getinstance().getEventCenter().fireEvent(this, NETWORK_CONNECTION_OFF);
				
//				// Start sync checking service 
//				Intent intent = new Intent(ctx, SyncCheckingService.class);
//				intent.putExtra("intent", "NetworkListener");
//				intent.putExtra("status", connecitonStatus);
//				intent.putExtra("speed", connecitonSpeed);
//				ctx.startService(intent);
			}
		}
	}
}