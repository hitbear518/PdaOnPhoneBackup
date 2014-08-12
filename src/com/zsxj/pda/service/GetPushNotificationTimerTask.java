package com.zsxj.pda.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;
import com.zsxj.pda.service.config.ConfigAccess;
import com.zsxj.pda.service.config.ConfigAccessImpl;
import com.zsxj.pda.service.http.HttpRequesterIntf;
import com.zsxj.pda.service.http.HttpServiceImpl;
import com.zsxj.pda.service.http.HttpServiceIntf;
import com.zsxj.pda.util.Util;
//import com.zsxj.pda.ui.ProductListActivity;
//import com.zsxj.pda.ui.PushNotificationURLActivity;

public class GetPushNotificationTimerTask  extends TimerTask implements HttpRequesterIntf {
	
	private Object dataId = null;
	private Context mContext = null;

	protected Log l = LogFactory.getLog(GetPushNotificationTimerTask.class);
	
	public GetPushNotificationTimerTask(Context context) {
		this.mContext = context;
	}
	
	@Override
	public void run() {
		
		l.debug("GetPushNotificationService run");
		
//		createAndSendRequest();
	}
	
	private void createAndSendRequest() {
		
		try {
			ConfigAccess ca = ServicePool.getinstance().getConfig();
			if (null == ca) {
				ConfigAccessImpl cai = new ConfigAccessImpl();
				cai.init(mContext);
				ca = cai;
			}
			
			HttpServiceIntf hsi = ServicePool.getinstance().getHttpService();
			if (null == hsi) {
				HttpServiceImpl httpImpl = new HttpServiceImpl();
				httpImpl.init(mContext);
				httpImpl.start();
				hsi = httpImpl;
			}
			
			String requestUrl = "item.message.get.php?";
			String requestParam = null;
			try {
				JSONObject params = new JSONObject();
				params.put("nick", 			ca.getConfig(ConfigAccess.SELLER_NICK));
				// 
				params.put("imei", 			Util.getDeviceIMEI(mContext));
				params.put("client_agent", 	ca.getConfig(ConfigAccess.CLIENT_AGENT));
				params.put("channelid", 	ca.getConfig(ConfigAccess.CHANNEL_ID));
				params.put("softver", 		ca.getConfig(ConfigAccess.SOFTWARE_VERSION));
				params.put("sysver", 		Util.getSysVer());
				// 
				requestParam = Util.getRequestParams(params);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			dataId = hsi.addRequest(this, new String[]{requestUrl, requestParam}, HttpServiceImpl.REQUEST_TYPE_DATA, HttpServiceImpl.SERVER_TYPE_NORMAL);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onReponse(Object idd, InputStream is) throws IOException {
		
		if (null != dataId && dataId.toString().equals(idd.toString())) {
			
			try {
				
				String rtnData = new String(Util.readDataFromIS(is));
				if (null == rtnData || rtnData.equals("")) {
					createAndSendRequest();
					return;
				}
				// Test 
//				JSONObject result = new JSONObject("{\"status\":200,\"data\":[{\"type\":1,\"description\":\"推送了一个自定义分类\",\"data\":\"315706026\",\"id\":1},{\"type\":2,\"description\":\"推送了一个url\",\"data\":\"http://www.baidu.com\",\"id\":2}]}");
				JSONObject result = new JSONObject(rtnData);
				// End 
				
				JSONArray pns = result.getJSONArray("data");
				for(int i = 0; i < pns.length(); i++) {

					JSONObject pn = pns.getJSONObject(i);
					int id 		= pn.getInt("id");
					// int type 	= pn.getInt("type");
					// String data = pn.getString("data");
					// String desc = pn.getString("description");
					
					if (null == getPushNotify(id)) {
						pn.put("is_alerted", false);
						putPushNotify(pn);
					}
				}
				showAlert();
				
			} catch (JSONException e) {
				System.out.println("split json data error : " + e);
			}
		}
	}

	@Override
	public void onFileDownloaded(Object id, InputStream data) throws IOException {
		
	}
	
	private JSONObject getPushNotify(int id) {
		
		SharedPreferences spfs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String pns = spfs.getString("pushNotifies", "[]");
		try {
			JSONArray pnArr = new JSONArray(pns);
			// Check already has 
			for (int i = 0; i < pnArr.length(); i++) {
				JSONObject pn = (JSONObject)pnArr.get(i);
				if (pn.getInt("id") == id)
					return pn;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void putPushNotify(JSONObject apn) {

		SharedPreferences spfs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String pns = spfs.getString("pushNotifies", "[]");
		try {
			JSONArray pnArr = new JSONArray(pns);
			// Check already has 
			boolean hasPN = false;
			for (int i = 0; i < pnArr.length(); i++) {
				JSONObject pn = (JSONObject)pnArr.get(i);
				if (pn.getInt("id") == apn.getInt("id"))
					hasPN = true;
			}
			
			// Add to shared preferences 
			if (!hasPN) {
				pnArr.put(apn);
				SharedPreferences.Editor editor = spfs.edit();
				editor.putString("pushNotifies", pnArr.toString());
				editor.commit();
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private void showAlert() {

		SharedPreferences spfs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String pns = spfs.getString("pushNotifies", "[]");
//		try {
//			JSONArray pnArr = new JSONArray(pns);
//			// Check already alert 
//			for (int i = 0; i < pnArr.length(); i++) {
//				
//				JSONObject pn = (JSONObject)pnArr.get(i);
//				if (pn.getBoolean("is_alerted"))
//					continue;
//				int id 		= pn.getInt("id");
//				int type 	= pn.getInt("type");
//				String data = pn.getString("data");
//				String desc = pn.getString("description");
//				
//				// Init notification 
//				Intent notiIntent = null;
//				switch(type) {
//				case 1:
//					notiIntent = new Intent(mContext, ProductListActivity.class);
//					notiIntent.putExtra("preActivity", "GetPushNotificationService");
//					notiIntent.putExtra("cid", data);
//					notiIntent.putExtra("title", desc);
//					notiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//					break;
//				case 2:
//					notiIntent = new Intent(mContext, PushNotificationURLActivity.class);
//					notiIntent.putExtra("url", data);
//					notiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//					break;
//				default:
//					break;	
//				}
//				// Set notification 
//				PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, notiIntent, 0);
//				Notification ntf = new Notification(R.drawable.logo, desc, System.currentTimeMillis());
//				ntf.setLatestEventInfo(mContext, mContext.getString(R.string.push_title), desc, pendingIntent); 
//				ntf.flags |= Notification.FLAG_AUTO_CANCEL;
//				// Show notification 
//				NotificationManager ntfMgr = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
//				ntfMgr.notify((1000 + id), ntf);
//				
//				// Update pn in share preferences 
//				pn.put("is_alerted", true);
//			}
//			
//			// Update pn in share preferences 
//			SharedPreferences.Editor editor = spfs.edit();
//			editor.putString("pushNotifies", pnArr.toString());
//			editor.commit();
//			
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
	}
}
