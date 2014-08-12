package com.zsxj.pda.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

/**
 * common HTTP utils
 * @author Hill
 *
 */
public class HttpUtils
{
	/**
	 * to check if there is HTTP proxy. In china it mainly for WAP
	 */
    public static void setupProxy()
    {
        final String proxyHost = android.net.Proxy.getDefaultHost();
        final int proxyPort = android.net.Proxy.getDefaultPort();
        Properties systemProperties = System.getProperties(); 
        if (!TextUtils.isEmpty(proxyHost))
        {
    		systemProperties.setProperty("http.proxyHost", proxyHost); 
    		systemProperties.setProperty("http.proxyPort", String.valueOf(proxyPort));
        }
        else
        {
        	if(systemProperties.contains("http.proxyHost"))
        	{
        		systemProperties.remove("http.proxyHost");
        		systemProperties.remove("http.proxyPort");
        	}
        }
    }
    
	/**
     * open URL connection with proxy check
     * 
     * At Android 2.3 for CMWAP HttpURLConnection conn = (HttpURLConnection) URL.openConnection(proxy); doesn't work so
     * implement as this. keep return value in case one day it will work again.
     * @param ctx
     * @param url
     * @return
     * @throws IOException
     */
    public static HttpURLConnection openConnection(Context ctx, URL url)throws IOException
	{
		ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
		
//		Proxy proxy = null;
		if((activeNetInfo != null) && !(activeNetInfo.getType()== ConnectivityManager.TYPE_WIFI) && (activeNetInfo.getExtraInfo().contains("wap") || activeNetInfo.getExtraInfo().contains("WAP")))
			setupProxy();
//		if(proxy != null)
//			return (HttpURLConnection) url.openConnection(proxy);
//		else
			return (HttpURLConnection) url.openConnection();
	}
    
    public static boolean isCurrentWAPConnected()
    {
    	// TODO to return APN type
    	return true;
    }
   
}
