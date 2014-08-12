package com.zsxj.pda.wdt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.util.Log;

import com.zsxj.pda.service.ServicePool;
import com.zsxj.pda.service.SocketService;
import com.zsxj.pda.service.config.ConfigAccess;
import com.zsxj.pda.util.ConstParams.Events;
import com.zsxj.pda.util.ConstParams.HandlerCases;

public class WDTLogin {
	
	static {
		System.loadLibrary("wdt_tdi");
	}
	
	public interface LoginCallBack {
		public void onLoginSuccess(User user, Account[] accounts);
		public void onLoginFail(int type, WDTException ex);
	}

	private native int initTDI();
	private native byte[] tdiProtSh1(String ip) throws WDTException;
	private native byte[] tdiProtSh2(byte[] buf, String sellerNick) throws WDTException;
	private native byte[] tdiProtSh3(byte[] buf, String userName, String password) throws WDTException;
	private native byte[] tdiProtSh4(byte[] buf) throws WDTException;
	private native byte[] prepareGetUserId(String userName) throws WDTException;
	private native int getUserId(byte[] buf) throws WDTException;
	private native byte[] preGetUser(String userNmae) throws WDTException;
	private native User getUser(byte[] buf) throws WDTException;
	private native byte[] prepareGetAccounts() throws WDTException;
	private native Account[] getAccounts(byte[] buf) throws WDTException;

	private native boolean isLogin();
	private native void logout();
	
	protected static WDTLogin instance = null;
	
	public static String LOGIN_SUCC = "wdt_login_succ";
	public static String LOGIN_FAILED = "wdt_login_failed";
	
	public static WDTLogin getinstance() {
		
		if(null == instance)
			instance = new WDTLogin();
		
		return instance;
	}

	public void login(Context ctx, String sellerNick, String userName, String password, LoginCallBack callBack) {
		
		// Test:duoduotest,admin,123456
		Thread loginThread = new Thread(new LoginRunnable(sellerNick, userName, password, callBack));
		loginThread.start();
	}

	public class LoginRunnable implements Runnable {
		
		private String mSellerNick;
		private String mUserName;
		private String mPassword;
		private LoginCallBack mCallBack;
		
		OutputStream ops = SocketService.ops;
		InputStream ips = SocketService.ips;
		int recvLen;
		byte[] recvBuf = new byte[8192];
		
		public LoginRunnable(String sellerNick, String userName, String password, LoginCallBack callBack) {
			mSellerNick = sellerNick;
			mUserName = userName;
			mPassword = password;
			mCallBack = callBack;
		}
		
		public void run() {
			if (!SocketService.isConnected()) {
				mCallBack.onLoginFail(HandlerCases.NO_CONN, null);
				return;
			}
			
			if (ops == null || ips == null) {
				ServicePool.getinstance().getEventCenter().fireEvent(this, Events.TIME_OUT);
				return;
			}
			
			try {						
				// Init s_tdi
				initTDI();
				
				// tdiProtSh1
				String remote_ip = ServicePool.getinstance().getConfig()
						.getConfig(ConfigAccess.SOCKET_HOST);
				byte[] bytes1 = tdiProtSh1(remote_ip);
				if (null == bytes1) {
					mCallBack.onLoginFail(HandlerCases.SHAKE_1_FAIL, null);
				}
				ops.write(bytes1);
				
				// tdiProtSh2
				recvLen = ips.read(recvBuf);
				byte[] bytes2 = tdiProtSh2(recvBuf, mSellerNick);
				if (null == bytes2) {
					mCallBack.onLoginFail(HandlerCases.SHAKE_2_FAIL, null);
					return;
				}
				ops.write(bytes2);
				
				// tdiProtSh3
				recvLen = ips.read(recvBuf);
				byte[] bytes3 = tdiProtSh3(recvBuf, mUserName, mPassword);
				if (null == bytes3) {
					mCallBack.onLoginFail(HandlerCases.SHAKE_3_FAIL, null);
					return;
				}
				ops.write(bytes3);
				
				// tdiProtSh4
				recvLen = ips.read(recvBuf);
				byte[] bytes4 = tdiProtSh4(recvBuf);
				if (null == bytes4) {
					mCallBack.onLoginFail(HandlerCases.SHAKE_4_FAIL, null);
					return;
				}
				
				byte[] bytes5 = preGetUser(mUserName);
				if (null == bytes5) {
					mCallBack.onLoginFail(HandlerCases.PREPARE_GET_USER_FAIL, null);
					return;
				}
				ops.write(bytes5);
				recvLen = ips.read(recvBuf);
				User user = getUser(recvBuf);
				if (user == null) {
					mCallBack.onLoginFail(HandlerCases.GET_USER_FAIL, null);
					return;
				}
				
				byte[] bytes6 = prepareGetAccounts();
				if (null == bytes6) {
					mCallBack.onLoginFail(HandlerCases.PREPARE_GET_ACCOUNTS_FAIL, null);
					return;
				}
				ops.write(bytes6);
				recvLen = ips.read(recvBuf);
				Account[] accounts = getAccounts(recvBuf);
				if (null == accounts) {
					mCallBack.onLoginFail(HandlerCases.GET_ACCOUNTS_FAIL, null);
					return;
				}
				
				mCallBack.onLoginSuccess(user, accounts);
				
			} catch (IOException e) {
				Log.d("LoginActivity", "IOException: " + e.toString());
				e.printStackTrace();
				mCallBack.onLoginFail(HandlerCases.TIME_OUT, null);
			} catch (WDTException wdtEx) {
				if (wdtEx.getStatus() > 20 || wdtEx.getStatus() < 0) {
					System.out.println("fireEvents");
					if (null == ServicePool.getinstance().getEventCenter()) {
						System.out.println("null == ServicePool.getinstance().getEventCenter()");
					} else {
						System.out.println("null != ServicePool.getinstance().getEventCenter()");
					}
					ServicePool.getinstance().getEventCenter().fireEvent(this, Events.DBE_INVALID_PACKET);
					return;
				}
				Log.e("WDTException", "status = " + wdtEx.getStatus() + ": " + 
					wdtEx.getMessage());
				mCallBack.onLoginFail(wdtEx.getStatus(), wdtEx);
			}
		}
	}
	
}
