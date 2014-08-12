package com.zsxj.pda.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;
import com.zsxj.pda.receivers.NetworkListener;
import com.zsxj.pda.service.config.ConfigAccess;
import com.zsxj.pda.service.event.EventListener;
import com.zsxj.pda.ui.client.LoginActivity;
import com.zsxj.pda.util.ConstParams.Events;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.PrefKeys;
import com.zsxj.pda.wdt.Account;
import com.zsxj.pda.wdt.User;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTLogin;
import com.zsxj.pda.wdt.WDTLogin.LoginCallBack;

public class SocketService extends Service implements EventListener, LoginCallBack {
	
	private static Socket client = null;
	public static OutputStream ops = null;
	public static InputStream ips = null;
	private Thread myClientThread = null;
		
	public static final int UPLIMIT_OF_RETRY_FOR_SINGLE_IP = 3;
	private int retryTimes;
	
	private boolean autoLoginBg = false;
	private boolean needReLogin = false;
	
	protected Log l = LogFactory.getLog(SocketService.class);
	
	private String remote_ip;
	private int remote_port;
//	private String remote_ip = "121.199.38.85"; //duoduotest
//	private String remote_ip = "121.196.128.4"; //ylbb
//	private String remote_ip = "121.196.130.204"; //duoduoyun
//	private int remote_port = 30000;

	public static String SCK_CONN_SUCC = "socket_conn_successful";
	public static String SCK_CONN_FAILED = "socket_conn_failed";
	
	private String mErrorMessage = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		if (ServicePool.getinstance().getConfig() == null) {
			try {
				ServicePool.getinstance().init(this);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		remote_ip = ServicePool.getinstance().getConfig()
				.getConfig(ConfigAccess.SOCKET_HOST);
		String portStr = ServicePool.getinstance().getConfig()
				.getConfig(ConfigAccess.SOCKET_HOST_PORT);
		remote_port = Integer.parseInt(portStr);
		System.out.println("SocketService onCreate ...................");
	}
	
	@Override
	public synchronized int onStartCommand(Intent intent, int flags, int startId) {
		
		System.out.println("SocketService onStartCommand ...................");
		System.out.println("SocketService onStartCommand intent = " + intent);
		
		if (null == intent) {
			android.util.Log.d("socket service", "System restart service");
			autoLoginBg = true;
		}
		
		initSocketService();
		
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		System.out.println("SocketService onDestroy() ...................");
		
		destroySocketThread();
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public synchronized void initSocketService() {
		
		// Update thread 
		if (null != myClientThread) {
			destroySocketThread();
		}
		myClientThread = new Thread(new MyClientSocketRunnable());
		myClientThread.start();
	}

	public class MyClientSocketRunnable implements Runnable {
		
		public void run() {
			
			// Try to connect using socket
			retryTimes = 1;
			boolean	bTry = trySocketConnection(retryTimes);
			
			while (false == bTry && retryTimes < UPLIMIT_OF_RETRY_FOR_SINGLE_IP) {
				retryTimes++;
				bTry = trySocketConnection(retryTimes);
			}
			
			// Init socket, if try successful 
			if (bTry) {
				// Connected successfully. Init the socket connection.
				boolean bInit = initSocketConnection();
			} else {
				// Fault to connect with current ip
				// Destroy socket thread 
				destroySocketThread();
				connFailed();
				return;
			}
		}
	}
	
	private boolean trySocketConnection(int reTryCount) {
		
		System.out.println("trySocketConnection");
		
		try {
			System.out.println("SocketService: try to connet to " + remote_ip + ":" + remote_port + "..." + reTryCount);
			client = new Socket();
            SocketAddress sAddr = new InetSocketAddress(remote_ip, remote_port);
            client.connect(sAddr, 1000 * 5); 
            System.out.println("SocketService: conneted to " + remote_ip + ":" + remote_port + " successfully!");
			
		} catch (UnknownHostException e) {
			System.out.println("SocketService: the host name could not be resolved into an IP address");
			return false;
		} catch (IOException e) {
			System.out.println("SocketService: an error occurs while creating the socket");
			return false;
		}
		
		return true;
	}
	
	private boolean initSocketConnection() {
		
		System.out.println("initSocketConnection");

		// Register network listener
		ServicePool.getinstance().getEventCenter().registerListener(this, NetworkListener.NETWORK_CONNECTION_ON);
		ServicePool.getinstance().getEventCenter().registerListener(this, NetworkListener.NETWORK_CONNECTION_OFF);
		ServicePool.getinstance().getEventCenter().registerListener(this, Events.TIME_OUT);
		ServicePool.getinstance().getEventCenter().registerListener(this, Events.DBE_INVALID_PACKET);
		
		// Set this socket's read timeout in milliseconds 
		try {
			client.setSoTimeout(10000);
		} catch (SocketException e) {
			System.out.println("SocketService: an error occurs while setSoTimeout()");
			connFailed();
			return false;
		}
		
		// Set the inputStream and the outputStream 
		try {
			ops = client.getOutputStream();
			ips = client.getInputStream();
			
			connSucc();
			
		} catch (UnsupportedEncodingException e) {
			System.out.println("SocketService: the encoding specified by UTF-8 cannot be found");
			connFailed();
			return false;
		} catch (IOException e) {
			System.out.println("SocketService: an error occurs while creating the input/output stream or the socket is in an invalid state");
			connFailed();
			return false;
		}
		
		return true;
	}
	
	private void destroySocketThread() {
		
		// Remove network listener
		ServicePool.getinstance().getEventCenter().removeListener(this, NetworkListener.NETWORK_CONNECTION_ON);
		ServicePool.getinstance().getEventCenter().removeListener(this, NetworkListener.NETWORK_CONNECTION_OFF);
		ServicePool.getinstance().getEventCenter().removeListener(this, Events.TIME_OUT);
		ServicePool.getinstance().getEventCenter().removeListener(this, Events.DBE_INVALID_PACKET);
		
		try {
			if (null != ips) {
				ips.close();
				ips = null;
			}
	
			if (null != ops) {
				ops.close();
				ops = null;
			}
			
			if (null != client && client.isConnected()) {
				client.close();
				client = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (null != myClientThread) {
			myClientThread.interrupt();
			myClientThread = null;
		}
	}
	
	public static boolean isConnected() {
		
		if (client == null) {
			return false;
		}
		
		try {
			client.sendUrgentData(0xFF);
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}
	
	private void connSucc() {

		ServicePool.getinstance().getEventCenter().fireEvent(this, SocketService.SCK_CONN_SUCC);
		
		if (autoLoginBg) {
			autoLoginBackground();
			autoLoginBg = false;
		} else if (needReLogin) {
			needReLogin = false;
			reLogin();
		}
	}
	
	private void connFailed() {

		ServicePool.getinstance().getEventCenter().fireEvent(this, SocketService.SCK_CONN_FAILED);
	}
	
	public void onEvent(Object source, String event) {

		System.out.println("SocketService onEvent");
		
		if (NetworkListener.NETWORK_CONNECTION_ON == event) {
			System.out.println("SocketService NETWORK_CONNECTION_ON");
			autoLoginBg = true;
			initSocketService();
		} else if (NetworkListener.NETWORK_CONNECTION_OFF == event) {
			System.out.println("SocketService NETWORK_CONNECTION_OFF");
		} else if (Events.TIME_OUT == event) {
			System.out.println("SocketService QUERY_TIME_OUT");
			mErrorMessage = "授权已超时，请重新登录";
			needReLogin = true;
			initSocketService();
		} else if (Events.DBE_INVALID_PACKET == event) {
			System.out.println("SocketService DBE_INVALID_PACKET");
			needReLogin = true;
			mErrorMessage = "数据库异常， 请检查卖家帐号并重新登录";
			initSocketService();
		}
	}
	
	/*
	 * ************************************************
	 * Auto login in background
	 * ************************************************
	 */
	private void autoLoginBackground() {
		
		// Read login info 
		SharedPreferences loginPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String sellerNick = loginPrefs.getString(PrefKeys.SELLER_NICK, "");
		String userName = loginPrefs.getString(PrefKeys.USER_NAME, "");
		String password = loginPrefs.getString(PrefKeys.PASSWORD, "");
		System.out.println("sellerNick=" + sellerNick + ", userName=" + userName + ", password=" + password);
		
		if (!sellerNick.equalsIgnoreCase("") && !userName.equalsIgnoreCase("") && !password.equalsIgnoreCase("")) {
			// Login in background 
			WDTLogin.getinstance().login(this, 
					sellerNick, 
					userName, 
					password,
					this);
		} else {
			// Switch to login activity 
			reLogin();
		}
	}
	
	private void reLogin() {
		Intent intent = new Intent(this, LoginActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.putExtra(Extras.ERROR_MESSAGE, mErrorMessage);
		startActivity(intent);
	}

	@Override
	public void onLoginSuccess(User user, Account[] accounts) {
		System.out.println("SocketService onLoginSuccess");
	}

	@Override
	public void onLoginFail(int type, WDTException ex) {
		System.out.println("SocketService onLoginFail");
	}
	
}
