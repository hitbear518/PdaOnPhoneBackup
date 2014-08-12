package com.zsxj.pda.ui.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.zsxj.pda.service.ServicePool;
import com.zsxj.pda.service.SocketService;
import com.zsxj.pda.service.config.ConfigAccess;
import com.zsxj.pda.service.config.ConfigAccessImpl;
import com.zsxj.pda.service.event.EventListener;
import com.zsxj.pda.service.http.HttpRequesterIntf;
import com.zsxj.pda.service.http.HttpServiceImpl;
import com.zsxj.pda.service.http.HttpServiceIntf;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.ConstParams.PrefKeys;
import com.zsxj.pda.util.ConstParams.TdiErrorCode;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.wdt.Account;
import com.zsxj.pda.wdt.User;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTLogin;
import com.zsxj.pda.wdt.WDTLogin.LoginCallBack;
import com.zsxj.pdaonphone.R;

public class LoginActivity extends Activity implements EventListener, LoginCallBack, 
	HttpRequesterIntf {
	
	private final int INPUT_OK = 0;
	private final int SELLER_NICK_EMPTY = 1;
	private final int USER_NAME_EMPTY = 2;
	private final int PASSWORD_EMPTY = 3;
	
	private Button mLoginBtn;
	private EditText mSellerNickEdit;
	private EditText mUserNameEdit;
	private EditText mPasswordEdit;
	private CheckBox mMemPasswordCheck;
	private CheckBox mAutoLoginCheck;

	private ProgressDialog mProgressDialog;
	
	private WDTLogin mWdtLogin = null;
	
	private Object mUpdateDataId;
	
	private boolean mActivityRunning;
	
	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			int todo = msg.what;
			switch (todo) {
			case HandlerCases.NO_CONN:
				mProgressDialog.dismiss();
				Util.toast(getApplicationContext(), R.string.no_conn);
				break;
			case HandlerCases.SOCKET_CONN_FAIL:
				mProgressDialog.dismiss();
				Log.e("Login", getString(R.string.connection_fail));
				Util.toast(getApplicationContext(), R.string.connection_fail);
				break;
			case HandlerCases.SHAKE_1_FAIL:
				mProgressDialog.dismiss();
				Log.e("Login", getString(R.string.shake_1_fail));
				Util.toast(getApplicationContext(), R.string.login_fail);
				break;
			case HandlerCases.SHAKE_2_FAIL:
				mProgressDialog.dismiss();
				Log.e("Login", getString(R.string.shake_2_fail));
				Util.toast(getApplicationContext(), R.string.login_fail);
				break;
			case HandlerCases.SHAKE_3_FAIL:
				mProgressDialog.dismiss();
				Log.e("Login", getString(R.string.shake_3_fail));
				Util.toast(getApplicationContext(), R.string.login_fail);
				break;
			case HandlerCases.SHAKE_4_FAIL:
				mProgressDialog.dismiss();
				Log.e("Login", getString(R.string.shake_4_fail));
				Util.toast(getApplicationContext(), R.string.login_fail);
				break;
			case HandlerCases.INVALID_PWD:
				mProgressDialog.dismiss();
				Util.toast(getApplicationContext(), 
					R.string.seller_nick_user_name_or_password_invalid);
				break;
			case HandlerCases.PREPARE_GET_USER_ID_FAIL:
				mProgressDialog.dismiss();
				Log.e("Login", getString(R.string.prepare_get_user_id_fail));
				Util.toast(getApplicationContext(), R.string.login_fail_for_user_id);
				break;
			case HandlerCases.GET_USER_ID_FAIL:
				mProgressDialog.dismiss();
				Log.e("Login", getString(R.string.get_user_id_fail));
				Util.toast(getApplicationContext(), R.string.login_fail_for_user_id);
				break;
			case HandlerCases.PREPARE_GET_ACCOUNTS_FAIL:
				mProgressDialog.dismiss();
				Log.e("Login", getString(R.string.prepare_get_accounts_fail));
				Util.toast(getApplicationContext(), R.string.login_fail_for_accounts);
				break;
			case HandlerCases.GET_ACCOUNTS_FAIL:
				mProgressDialog.dismiss();
				Log.e("Login", getString(R.string.get_accounts_fail));
				Util.toast(getApplicationContext(), R.string.login_fail_for_accounts);
				break;
			case HandlerCases.LOGIN_SUCCESS:
				mProgressDialog.dismiss();
				saveLoginInfo();
				Intent startSelectWarehouse = new Intent(
						LoginActivity.this, SelectWarehouseActivity.class);
				startActivity(startSelectWarehouse);
				finish();
				break;
			case HandlerCases.UPDATE_FAIL_NO_CONN:
				Util.toast(getApplicationContext(), R.string.update_fail_no_conn);
				break;
			case HandlerCases.UPDATE_CHECK:
				if (mActivityRunning) {
					showUpdateInfo(
							msg.getData().getInt("update_type"), 
							msg.getData().getInt("update_method"), 
							msg.getData().getString("update_url"), 
							msg.getData().getString("update_ver"), 
							msg.getData().getLong("update_size"), 
							msg.getData().getString("update_msg")
							);
				}
				
				break;
			case HandlerCases.TIME_OUT:
				mProgressDialog.dismiss();
				Util.toast(getApplicationContext(), "网络连接不稳定，登录失败");
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setUpViews();
		setUpListeners();
		initData();
		sendCheckUpdateRequest();
		String errorMessage = getIntent().getStringExtra(Extras.ERROR_MESSAGE);
		if (!TextUtils.isEmpty(errorMessage)) {
			Util.toast(getApplicationContext(), errorMessage);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mActivityRunning = true;
	}
	
	protected void onPause() {
		super.onPause();
		mProgressDialog.dismiss();
		mActivityRunning = false;
	};
	
	private void download(String url) {
		DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		Uri downloadUri = Uri.parse(url);
		DownloadManager.Request request = new DownloadManager.Request(downloadUri);
		String fileName = url.substring(url.lastIndexOf("/") + 1);
		File downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		Log.d("debug", downloadDir.toString());
		File updateFile = new File(downloadDir, fileName);
		Log.d("debug", updateFile.toString());
		if (updateFile.exists()) {
			Intent promtInstall = new Intent(Intent.ACTION_VIEW);
			promtInstall.setDataAndType(Uri.fromFile(updateFile), 
					"application/vnd.android.package-archive");
			startActivity(promtInstall);
			finish();
		} else {
			request.setDestinationInExternalFilesDir(this, 
					Environment.DIRECTORY_DOWNLOADS, fileName);
			long downloadId = dm.enqueue(request);
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
			editor.putLong(PrefKeys.DOWNLOAD_ID, downloadId);
			editor.commit();
		}
	}
	
	private boolean downloading() {
		DownloadManager.Query query = new DownloadManager.Query();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		long downloadId = prefs.getLong(PrefKeys.DOWNLOAD_ID, 0);
		query.setFilterById(downloadId);
		DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		Cursor c = dm.query(query);
		if (c.moveToNext()) {
			int statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
			int status = c.getInt(statusIdx);
			if (DownloadManager.STATUS_RUNNING == status) {
				return true;
			}
		}
		return false;
	}

	private void setUpViews() {
		setContentView(R.layout.login_activity);

		mSellerNickEdit = (EditText) findViewById(R.id.edit_0);
		mUserNameEdit = (EditText) findViewById(R.id.edit_1);
		mPasswordEdit = (EditText) findViewById(R.id.edit_2);
		mMemPasswordCheck = (CheckBox) findViewById(R.id.check_0);
		mAutoLoginCheck = (CheckBox) findViewById(R.id.check_1);
		mLoginBtn = (Button) findViewById(R.id.btn_login);	
	}

	private void setUpListeners() {

		mLoginBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doLoginPrepare();
			}
		});
		
		mMemPasswordCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (!isChecked) {
					mAutoLoginCheck.setChecked(false);
				}
			}
		});
		
		mAutoLoginCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mMemPasswordCheck.setChecked(isChecked);
			}
		});
	}
	
	private void autoLoginPrepare() {
		mProgressDialog.show();
		
		ServicePool.getinstance().getEventCenter().registerListener(this, SocketService.SCK_CONN_SUCC);
		ServicePool.getinstance().getEventCenter().registerListener(this, SocketService.SCK_CONN_FAILED);

		Intent socketItn = new Intent(this, SocketService.class);
		this.startService(socketItn);
	}
	
	private void doLoginPrepare() {
		mProgressDialog.show();
		
		switch (checkInputStatus()) {
		case SELLER_NICK_EMPTY:
			mProgressDialog.dismiss();
			Util.toast(getApplicationContext(), R.string.login_seller_nick_needed);
			return;
		case USER_NAME_EMPTY:
			mProgressDialog.dismiss();
			Util.toast(getApplicationContext(), R.string.login_user_name_needed);
			return;
		case PASSWORD_EMPTY:
			mProgressDialog.dismiss();
			Util.toast(getApplicationContext(), R.string.login_password_needed);
			return;
		default:
			break;
		}
		
		ServicePool.getinstance().getEventCenter().registerListener(this, SocketService.SCK_CONN_SUCC);
		ServicePool.getinstance().getEventCenter().registerListener(this, SocketService.SCK_CONN_FAILED);

		Intent socketItn = new Intent(this, SocketService.class);
		this.startService(socketItn);
	}
	
	private void doLogin() {
		
		ServicePool.getinstance().getEventCenter().registerListener(this, WDTLogin.LOGIN_SUCC);
		ServicePool.getinstance().getEventCenter().registerListener(this, WDTLogin.LOGIN_FAILED);
		
		mWdtLogin.login(this,
				mSellerNickEdit.getText().toString(), 
				mUserNameEdit.getText().toString(), 
				mPasswordEdit.getText().toString(),
				this);
	}

	private void initData() {
		
		mWdtLogin = WDTLogin.getinstance();

		// Init progress dialog
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setTitle(R.string.please_wait);
		mProgressDialog.setMessage(getString(R.string.login) + "...");
		mProgressDialog.setCancelable(false);
		
		readLoginInfo();
		
		if (checkInputStatus() == INPUT_OK && mAutoLoginCheck.isChecked()) {
			autoLoginPrepare();
		} else {
//			mSellerNickEdit.setText("duoduoyun");
//			mUserNameEdit.setText("admin");
//			mPasswordEdit.setText("ddy!@#$%^");
//			mSellerNickEdit.setText("demo");
//			mUserNameEdit.setText("ws");
//			mPasswordEdit.setText("123");
//			mSellerNickEdit.setText("xiaobaxi");
//			mUserNameEdit.setText("admin");
//			mPasswordEdit.setText("xiaobaxi");
//			mSellerNickEdit.setText("duoduotest");
//			mUserNameEdit.setText("admin");
//			mPasswordEdit.setText("123456");
//			mSellerNickEdit.setText("haoxu");
//			mUserNameEdit.setText("admin");
//			mPasswordEdit.setText("172c3a");
//			mSellerNickEdit.setText("yinpai");
//			mUserNameEdit.setText("pda");
//			mPasswordEdit.setText("1234");
//			mSellerNickEdit.setText("qiqu");
//			mUserNameEdit.setText("航航");
//			mPasswordEdit.setText("123456");
//			mSellerNickEdit.setText("joyvio");
//			mUserNameEdit.setText("财务");
//			mPasswordEdit.setText("666666");
			mSellerNickEdit.setText("jiulong");
			mUserNameEdit.setText("admin");
			mPasswordEdit.setText("wenwen0023");
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		ServicePool.getinstance().getEventCenter()
			.registerListener(this, HttpServiceIntf.CONNECTION_ERROR);
	}

	@Override
	protected void onStop() {
		super.onStop();
		ServicePool.getinstance().getEventCenter()
			.removeListener(this, HttpServiceIntf.CONNECTION_ERROR);
	}

	@Override
	public void onEvent(Object source, String event) {

		if (SocketService.SCK_CONN_SUCC == event || SocketService.SCK_CONN_FAILED == event) {
			ServicePool.getinstance().getEventCenter().removeListener(this, SocketService.SCK_CONN_SUCC);
			ServicePool.getinstance().getEventCenter().removeListener(this, SocketService.SCK_CONN_FAILED);
		}
		
		if (SocketService.SCK_CONN_SUCC == event) {
			doLogin();
		} else if (SocketService.SCK_CONN_FAILED == event) {
			handler.sendEmptyMessage(HandlerCases.SOCKET_CONN_FAIL);
		}
		
		if (HttpServiceIntf.CONNECTION_ERROR == event) {
			handler.sendEmptyMessage(HandlerCases.UPDATE_FAIL_NO_CONN);
		}
	}
	
	private int checkInputStatus() {
		if (mSellerNickEdit.getText().toString().equals("")) {
			return SELLER_NICK_EMPTY;
		}
		if (mUserNameEdit.getText().toString().equals("")) {
			return USER_NAME_EMPTY;
		}
		if (mPasswordEdit.getText().toString().equals("")) {
			return PASSWORD_EMPTY;
		}
		
		return INPUT_OK;
	}
	
	private void saveLoginInfo() {
		SharedPreferences loginPrefs = 
				PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = loginPrefs.edit();
		
		editor.clear();
		
		editor.putString(PrefKeys.SELLER_NICK, 
				mSellerNickEdit.getText().toString());
		editor.putString(PrefKeys.USER_NAME, 
				mUserNameEdit.getText().toString());
		
		editor.putBoolean(PrefKeys.MEM_PSD, 
				mMemPasswordCheck.isChecked());
		if (mMemPasswordCheck.isChecked()) {
			editor.putString(PrefKeys.PASSWORD, 
					mPasswordEdit.getText().toString());
		}
		
		editor.putBoolean(PrefKeys.AUTO_LOGIN, 
				mAutoLoginCheck.isChecked());
		editor.commit();
		
		Globals.setSellerNick(mSellerNickEdit.getText().toString());
	}
	
	private void readLoginInfo() {
		// Check Login info memorization
		SharedPreferences loginPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		String curSellerNick = loginPrefs.getString(PrefKeys.SELLER_NICK, "");
		String curUserName = loginPrefs.getString(PrefKeys.USER_NAME, "");
		String curPassword = loginPrefs.getString(PrefKeys.PASSWORD, "");
		
		if (!curSellerNick.equals("")) {
			// Set current user to TextEdit
			mSellerNickEdit.setText(curSellerNick);
		}
		if (!curUserName.equals("")) {
			// Set current user to TextEdit
			mUserNameEdit.setText(curUserName);
		}
		
	
		boolean memorizePassword = 
				loginPrefs.getBoolean(PrefKeys.MEM_PSD, true);
		boolean autoLogin = 
				loginPrefs.getBoolean(PrefKeys.AUTO_LOGIN, false);
		mAutoLoginCheck.setChecked(autoLogin);
		mMemPasswordCheck.setChecked(memorizePassword);
		if (memorizePassword) {
			if (!curPassword.equals("")) {
				// Set current user to TextEdit
				mPasswordEdit.setText(curPassword);
			}
		}
	}
	
	private void sendCheckUpdateRequest() {
		if (downloading()) {
			Util.toast(getApplicationContext(), "更新文件下载中，请稍等");
			finish();
			return;
		}

		try {
			ConfigAccess ca = null;
			if (null == ca) {
				ConfigAccessImpl cai = new ConfigAccessImpl();
				cai.init(this);
				ca = cai;
			}
			
			HttpServiceIntf hsi = null;
			if (null == hsi) {
				HttpServiceImpl httpImpl = new HttpServiceImpl();
				httpImpl.init(this);
				httpImpl.start();
				hsi = httpImpl;
			}
			
			String requestUrl = "client.init.php?";
			String requestParam = null;
			try {
				JSONObject params = new JSONObject();
				params.put("width", 		Util.getScreenWidth(this));
				params.put("height", 		Util.getScreenHeight(this));
				// 
				params.put("imei", 			Util.getDeviceIMEI(this));
				params.put("client_agent", 	ca.getConfig(ConfigAccess.CLIENT_AGENT));
				params.put("channelid", 	ca.getConfig(ConfigAccess.CHANNEL_ID));
				params.put("softver", 		ca.getConfig(ConfigAccess.SOFTWARE_VERSION));
				params.put("sysver", 		Util.getSysVer());
				String ip = ca.getConfig(ConfigAccess.SOCKET_HOST);
				String ipMd5 = getMd5(ip, "UTF-8");
				params.put("ip_check", ipMd5);
				requestParam = Util.getRequestParams(params);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			mUpdateDataId = hsi.addRequest(this, new String[]{requestUrl, requestParam}, HttpServiceImpl.REQUEST_TYPE_DATA, HttpServiceImpl.SERVER_TYPE_NORMAL);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getMd5(String input, String charset) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] bytesOfMessage = input.getBytes(charset);
			byte[] theDigest = md.digest(bytesOfMessage);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < theDigest.length; i++) {
                String hex = Integer.toHexString(0xff & theDigest[i]);
                if (hex.length() == 1) {
                	sb.append("0");
                }
                sb.append(hex);
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void showUpdateInfo(
			int type, 
			final int method, 
			final String url, 
			final String ver, 
			final long size, 
			String msg
			) {
		
		if (0 == type) {
			// No update 
			return;
		} else if (1 == type) {
			// Option update 
			new AlertDialog.Builder(LoginActivity.this)
				.setTitle("升级提示")
				.setMessage(msg)
				.setPositiveButton("下载", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						download(url);
					}
				})
				.setNegativeButton("暂不升级", null)
				.show();
		} else if (2 == type) {
			
			// Force update 
			final AlertDialog d = new AlertDialog.Builder(LoginActivity.this)
			.setTitle("升级提示")
			.setMessage(msg)
			.setPositiveButton("下载", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					download(url);
					finish();
				}
			})
			.setOnCancelListener(new DialogInterface.OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			})
			.create();
			d.show();
		}
	}
	

	@Override
	public void onLoginSuccess(User user, Account[] accounts) {
		Globals.setUser(user);
		Globals.setAccounts(accounts);
		handler.sendEmptyMessage(HandlerCases.LOGIN_SUCCESS);
	}

	@Override
	public void onLoginFail(int status, WDTException ex) {
		if (null == ex) {
			handler.sendEmptyMessage(status);
			return;
		}
		switch (status) {
		case TdiErrorCode.DBE_INVALID_LICENSE:
			handler.sendEmptyMessage(HandlerCases.SHAKE_1_FAIL);
			break;
		case TdiErrorCode.DBE_INVALID_PWD:
			handler.sendEmptyMessage(HandlerCases.INVALID_PWD);
			break;
		default:
			break;
		}
	}

	@Override
	public void onReponse(Object id, InputStream data) throws IOException {
		if (null != mUpdateDataId && mUpdateDataId.toString().equals(id.toString())) {
			
			try {
				String rtnData = new String(Util.readDataFromIS(data));
				if (null == rtnData || rtnData.equals("")) {
					sendCheckUpdateRequest();
					return;
				}
				
				JSONObject result = new JSONObject(rtnData);
				// Message return 
		        Bundle bundle = new Bundle();
		        bundle.putInt("update_type", 	result.optJSONObject("init_response").optInt("update_type"));
		        bundle.putInt("update_method", 	result.optJSONObject("init_response").optInt("update_method"));
		        bundle.putString("update_url", 	result.optJSONObject("init_response").optString("update_url"));
		        bundle.putString("update_ver", 	result.optJSONObject("init_response").optString("update_ver"));
		        bundle.putLong("update_size", 	result.optJSONObject("init_response").optLong("update_size"));
		        bundle.putString("update_msg", 	result.optJSONObject("init_response").optString("update_msg"));
				Message message = new Message();
		        message.what = HandlerCases.UPDATE_CHECK;
		        message.setData(bundle);
		        handler.sendMessage(message);
			} catch (Exception e) {
			}
		}
	}

	@Override
	public void onFileDownloaded(Object id, InputStream data)
			throws IOException {
	}
}
