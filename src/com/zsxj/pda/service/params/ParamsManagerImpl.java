package com.zsxj.pda.service.params;

import android.content.Context;

import com.zsxj.pda.ui.client.MainActivity;

public class ParamsManagerImpl implements ParamsManager {
	
	protected Context mContext = null;
	protected MainActivity mainActivity = null;
	protected int loginStatus = -1;
	protected String taobaoUserNick = "";
	//access_token is the seesionKey of taobaoUserNick
	protected String access_token = "";
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see UserManager#init()
	 */
	public void init(Context context) {
		
		mContext = context;
		loginStatus = -1;
		taobaoUserNick = null;
	}
	
	public int getLoginStatus() {
		return loginStatus;
	}

	public void setLoginStatus(int status) {
		loginStatus = status;
	}

	public String getTaobaoUserNick() {
		return taobaoUserNick;
	}

	public void setTaobaoUserNick(String nick) {
		taobaoUserNick = nick;
	}

	public String getAccess_token() {
		return access_token;
	}

	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}

	@Override
	public MainActivity getMainActivity() {
		return mainActivity;
	}

	@Override
	public void SetMainActivity(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
	}

}