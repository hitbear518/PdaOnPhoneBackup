package com.zsxj.pda.service.network;

import java.util.HashSet;
import java.util.Set;

import com.zsxj.pda.service.ServicePool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class NetStatusWatcher extends BroadcastReceiver {
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
	protected void parseNetworkStatus(NetworkInfo ni)
	{
		if(ni == null)
			throw new RuntimeException("ni can not be null");
		
		{	// parse connection type
			if(ni.getType() == ConnectivityManager.TYPE_WIFI)
				connecitonType = NETWORK_CONNECTION_TYPE_WIFI;
			else
				connecitonType = NETWORK_CONNECTION_TYPE_GPRS;
			// in Android 2.1 there is no other TYPE_ defines, like: TYPE_DUN
		}
		
		{	// parese connection speed
			if(highSpeed.contains(ni.getSubtype()) || (connecitonType == NETWORK_CONNECTION_TYPE_WIFI))
				connecitonSpeed = CONNECTION_SPEED_HEIGHT;
			else
				connecitonSpeed = CONNECTION_SPEED_LOW;
		}
		
		{
			String ei = ni.getExtraInfo();
			if(ei != null)
			{
				if(ei.contains("wap") || ei.contains("WAP"))
					connecitonApnType = CONNECTION_APN_TYPE_WAP;
				else
					connecitonApnType = CONNECTION_APN_TYPE_NO_WAP;
			}
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		checkConnectionStatus(context, true);
	}
	
	/**
	 * check current connection status
	 * @param context
	 */
	protected void checkConnectionStatus(Context context, boolean fireEvents)
	{
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
		if (activeNetInfo != null)
		{
			parseNetworkStatus(activeNetInfo);
			connecitonStatus = NETWORK_CONNECTION_ON;
			if(fireEvents)
				ServicePool.getinstance().getEventCenter().fireEvent(this, NETWORK_CONNECTION_ON);
		}
		else
		{
			connecitonType = null;
			connecitonSpeed = null;
			connecitonStatus = NETWORK_CONNECTION_OFF;
			if(fireEvents)
				ServicePool.getinstance().getEventCenter().fireEvent(this, NETWORK_CONNECTION_OFF);
		}
	}
	
	/**
	 * initial
	 * @param ctx
	 */
	public void init(Context ctx)
	{
		checkConnectionStatus(ctx, false);
		IntentFilter intentFilter = new  IntentFilter();//"android.net.conn.CONNECTIVITY_CHANGE");
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		ctx.registerReceiver(this, intentFilter);
	}
	
	/**
	 * self clear
	 * @param ctx
	 */
	public void clear(Context ctx)
	{
		ctx.unregisterReceiver(this);
	}

	public String getConnecitonType()
	{
		return connecitonType;
	}

	public String getConnecitonSpeed()
	{
		return connecitonSpeed;
	}

	/**
	 * this works only when GPRS connected, otherwise return value is undefined
	 * @return
	 */
	public String getConnecitonApnType()
	{
		return connecitonApnType;
	}

	public String getConnecitonStatus()
	{
		return connecitonStatus;
	}
}