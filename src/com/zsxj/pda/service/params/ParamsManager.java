package com.zsxj.pda.service.params;

import com.zsxj.pda.ui.client.MainActivity;

import android.content.Context;

public interface ParamsManager {

	/**
	 * Instance initial. THIS MUST THE VERY FIRST INVOCATION.
	 * @throws Exception
	 */
	public void init(Context context);
	
	public int getLoginStatus();
	public void setLoginStatus(int status);
	
	public String getTaobaoUserNick();
	public void setTaobaoUserNick(String nick);

	public String getAccess_token();
	public void setAccess_token(String access_token);
	
	public MainActivity getMainActivity();
	public void SetMainActivity(MainActivity mainActivity);
	
}
