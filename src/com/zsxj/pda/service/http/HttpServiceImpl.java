package com.zsxj.pda.service.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;
import com.zsxj.pda.service.CommonEventNames;
import com.zsxj.pda.service.ServicePool;
import com.zsxj.pda.service.config.ConfigAccess;
import com.zsxj.pda.service.event.EventCenter;
import com.zsxj.pda.service.event.EventListener;
import com.zsxj.pda.util.HttpUtils;
import com.zsxj.pda.util.SHA1;
import com.zsxj.pda.util.Util;
//import com.js.sellerclient.service.user.UserInfo;

public class HttpServiceImpl implements HttpServiceIntf, EventListener
{
	protected Log l = LogFactory.getLog(HttpServiceImpl.class);

	protected HttpRequestQueue queue = new HttpRequestQueue();
	protected HttpTask ht = null;
	protected UUID lastReqId = null;

	protected volatile String token = null;
	protected volatile String sex = null;
	public String userName;
	public String pwd;

	protected HttpCfgReader hcr = new HttpCfgReader();
	protected int reLogin = 1;
	protected Context ctx;
	protected int lastLoginCode = 0;
	
	protected EventCenter eventCenter = ServicePool.getinstance().getEventCenter();
	protected final String boundary = "--------boundary";
	protected String param;
	protected boolean refuseRequest = false;
	volatile protected static boolean someOneNotifiedNetworkError;		// marks some instance of this class is firing network error. then the rest should not do it.

	/**
	 * 
	 * @param ctx
	 *            Application context is recommended
	 * @throws Exception
	 */
	public void init(Context context) throws Exception
	{
		ctx = context;
		hcr.read(ctx, "http_config.xml");
	}

	/**
	 * Start service
	 * this MUST invoke after call login succeed.
	 */
	protected volatile boolean isStart = false;
	public void start()
	{
		if(isStart == false)
		{
			isStart = true;
			startService();
		}
	}
	
	protected synchronized void startService()
	{
		if(ht != null)
			return;
		
		ht = new HttpTask();
		Thread t = new Thread(ht);
		t.start();
	}

	protected void notifyListener(Object oo)
	{
		synchronized (oo)
		{
			oo.notifyAll();
		}
	}

	/**
	 * Stop service
	 */
	public void stop()
	{
		if(isStart == true)
		{
			isStart = false;
			stopService();
		}
	}
	
	protected void stopService()
	{
		if(ht == null)
			return;
		ht.stop();
		synchronized (ht) {
			ht.notifyAll();
		}
		
		clear();
	}

	protected void checkReqType(String type)
	{
		if (!type.equalsIgnoreCase(REQUEST_TYPE_DATA) && !type.equalsIgnoreCase(REQUEST_TYPE_DIRECT) && !type.equalsIgnoreCase(REQUEST_TYPE_RESIZED_IMAGE) )
			throw new RuntimeException("request type not recognized");
	}

	protected String checkStr(String str)
	{
		if((str == null) || (str.length() == 0))
			return null;
		else
			return str;
	}
	
	public Object addPost(HttpRequesterIntf sender, String[] request, InputStream is,String requestType, String serverType)
	{
		if(refuseRequest)
			return null;
		
		HttpRequest hr = new HttpRequest();
		hr.req = request;
		hr.is = is;
		hr.reqType = requestType;
		hr.svrType = serverType;
		param = request[1];
		UUID oo = null;
		synchronized (queue)
		{
			oo = queue.addRequest(sender, hr);
		}
		
		synchronized (ht)
		{
			ht.notifyAll();	
		}
		return oo;
	}
	
	protected static final String POST = "POST";
	protected static final String GET = "GET";
	public Object addRequest(HttpRequesterIntf sender,
			String[] request, String requestType, String serverType)
	{
		if(refuseRequest)
			return null;
		if(!isStart)
			throw new RuntimeException("HTTP service has not been started.");
		if(ht == null)
			return null;

		checkReqType(requestType);
		HttpRequest hr = new HttpRequest();
		hr.req = request;
		hr.req[1] = checkStr(request[1]);
		hr.reqType = requestType;
		hr.svrType = serverType;
		UUID oo = null;
		synchronized (queue)
		{
			oo = queue.addRequest(sender, hr);
		}
		synchronized (ht)
		{
			ht.notifyAll();	
		}
		return oo;
	}

	public void removeRequestBySender(HttpRequesterIntf sender)
	{
		synchronized (queue)
		{
			queue.removeRequestBySender(sender);	
		}
	}

	public void removeRequestByID(Object id)
	{
		synchronized (queue)
		{
			queue.removeRequestByID((UUID) id);
		}
	}

	/**
	 * get the current top request related UUID
	 * 
	 * @return UUID or 'null' if no request is pending
	 */
	protected UUID getNextRequestID()
	{
		synchronized (queue)
		{
			lastReqId = queue.getNextRequest();
		}
		return lastReqId;
	}

	/**
	 * check if current token is legal
	 * 
	 * @param data
	 *            The HTTP response
	 * @return
	 * 
	 * @throws Exception
	 *             if the response is not checkable
	 */
	protected boolean isTokenLegal(byte[] data) throws Exception
	{
		String str = new String(data, "UTF-8");
		l.debug("reply from server is:" + str);
		boolean legal = true;
		legal = (-1 != str.indexOf("-1025,\"msg\":\"invalid parameter: token\""));
		legal = legal || (-1 != str.indexOf("\"msg\":\"miss user token\""));
		
		return legal;
	}

	/**
	 * re-login 
	 * @return	'true' if succeed or 'false' if not
	 */
	protected boolean reLogin()
	{
		int failedCount = 0;
		while(failedCount < 3)
		{
			try
			{
				byte[] b = login(userName, pwd);
				if(b == null)
				{
					queue.clear();
					ServicePool.getinstance().getEventCenter().fireEvent(this, CommonEventNames.ACCOUNT_ERROR);
				}
				break;
			}
			catch (Exception e)
			{
				failedCount++;
			}
		}
		
		if(failedCount < 3)
			return true;
		else
			return false;
	}
	
	/**
	 * init all 
	 */
	public void clear()
	{
		synchronized (queue)
		{
			queue.clear();
		}
		
		lastReqId = null;
		reLogin = 1;
		lastLoginCode = 0;
	}
	
	/**
	 * read data from stream
	 * @param is
	 * @return
	 */
	protected byte[] readDataFromIS(InputStream is)throws IOException
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] data = new byte[50];
		int readLen = 0;
		while ((readLen = is.read(data)) > 0)
			os.write(data, 0, readLen);
		return os.toByteArray();
	}
	
	/**
	 * deal with request data response
	 * @param is
	 * 
	 * @return 'true' if 
	 * @throws IOException, TokenIllegalException
	 */
	protected void onDataResponse(InputStream is)throws IOException, TokenIllegalException
	{
		boolean reqSucceed = false;
		byte[] data = null;
		
		try
		{
			data = readDataFromIS(is);
			reqSucceed = !isTokenLegal(data);
		}
		catch (IOException e) 
		{
			throw e;
		}
		catch (Exception e)
		{
			StringBuilder sb = new StringBuilder();
			sb.append("the response which expected as String is format error: ");
			if(data != null)
				sb.append(Util.getHexString(data));
			l.error(sb.toString(), e);
		}

		if (!reqSucceed)
			throw new TokenIllegalException();

		HttpRequesterIntf reqer = null;
		synchronized (queue)
		{
			reqer = queue.getRequester(lastReqId);
		}
		if(reqer != null)
		{
			InputStream i = new ByteArrayInputStream(data);
			reqer.onReponse(lastReqId, i);
		}
	}
	
	/**
	 * Deal with raw request response
	 * @param is
	 * 
	 * @throws TokenIllegalException
	 */
	protected void onFileResponse(InputStream is)throws TokenIllegalException, IOException
	{
		HttpRequesterIntf reqer = null;
		synchronized (queue)
		{
			reqer = queue.getRequester(lastReqId);
		}
		if(reqer != null)
			reqer.onReponse(lastReqId, is);
	}
	/**
	 * first response for the last request
	 * 
	 * @param is
	 */
	protected int failedCount = 0;
	protected synchronized void onResponse(InputStream is)throws IOException
	{
		HttpRequest hr = null;
		synchronized (queue)
		{
			hr = queue.getRequest(lastReqId);
		}
		try
		{
			if(hr == null)
				return;
			
			if(hr.reqType==null || (hr.reqType.equalsIgnoreCase(REQUEST_TYPE_DIRECT) || hr.reqType.equalsIgnoreCase(REQUEST_TYPE_RESIZED_IMAGE)))
				onFileResponse(is);
			else 
				onDataResponse(is);

			synchronized (queue)
			{
				queue.removeRequestByID(lastReqId);
			}
		}
		catch (TokenIllegalException e)
		{
			if(!reLogin())
				onConnectionError(true);
		}
	}
	
	protected void close(HttpURLConnection uc)
	{
		try
		{
			uc.disconnect();
		}
		catch (Exception e)
		{
		}
	}

	/**
	 * attach token to URL
	 * 
	 * @param base
	 * @return
	 */
	public String attachToken(String request)
	{
		if(request != null)
		{
			if(token != null)
			{
				StringBuilder sb = new StringBuilder();
				sb.append(request).append("&token=").append(token);
				request = sb.toString();
			}
		}
		
		return request;
	}

	/**
	 * get server prifix with given request & server type
	 * @param reqType
	 * @param svrType
	 * @return
	 */
	protected StringBuilder genServerPrifix(String reqType, String svrType)
	{
		StringBuilder sb = null;
		if(reqType == REQUEST_TYPE_RESIZED_IMAGE)
			sb = getImageServerPrifix();
		else if(reqType == REQUEST_TYPE_DATA)
		{
			if(svrType == SERVER_TYPE_LOCATION)
				sb = getLocationServerPrifix(true);
			else
				sb = getServerPrifix();
		}
		else if(reqType!=null && reqType.equalsIgnoreCase(REQUEST_TYPE_DIRECT))
			sb = null;
		
		return sb;
	}
	
	/**
	 * currently here fires can't reach server only
	 */
	protected void fireConnectionError()
	{
		eventCenter.fireEvent(this, CONNECTION_ERROR);
	}
	
	/**
	 * deal with connection error
	 * @param fireEvent 'true' if need to fire this error or 'false' if not
	 */
	protected void onConnectionError(boolean fireEvent)
	{
		refuseRequest = true;
		if(fireEvent)
			if(!someOneNotifiedNetworkError)
				synchronized (HttpServiceImpl.class)
				{
					if(!someOneNotifiedNetworkError)
					{
						someOneNotifiedNetworkError = true;
						fireConnectionError();
						someOneNotifiedNetworkError = false;
					}
				}
		synchronized (queue)
		{
			queue.clear();
		}
		refuseRequest = false;

	}
	
	class HttpTask implements Runnable
	{
		protected boolean stop = false;
		HttpURLConnection conn = null;
		InputStream is = null;
		
		public void stop()
		{
			stop = true;
//			Log l = LogFactory.getLog(HTTPMassDownload.class);
//			l.debug(toString() + new Date() + " thread stop()1=====");
//			Util.close(is);
//			l.debug(toString() + new Date() + " thread stop()2=====");
//			close(conn);
//			l.debug(toString() + new Date() + " thread stop()3=====");
		}

		public void run()
		{
			int errCound = 0;
			while (true)
			{
				UUID reqID = getNextRequestID();
				StringBuilder sb = null;
					
				if (stop)
				{
					l.debug("exit http task on 'stop'");
					return;
				}

				try
				{
					if (reqID == null)
					{
						synchronized (this)
						{
							wait();
						}
					}
					else
					{
						errCound++;
						HttpRequest hr = null;
						synchronized (queue)
						{
							hr = queue.getRequest(reqID);
						}
						sb = genServerPrifix(hr.reqType, hr.svrType);
						if(sb == null)
							// here means direct download
						{
							String url = replaceMobileDNS(hr.req[0]);
							url = checkImageQuality(url);
							sb = new StringBuilder();
							sb.append(url);
						}
						else
							sb.append(hr.req[0]);
						String req = hr.req[1];
						if(req != null)
						{
							if(req.indexOf("&token=") == -1)
								req = attachToken(req);
							sb.append(req);
						}
						String tmp = sb.toString();//.replace("_iphone.jpg", "_wap.jpg");
						android.util.Log.i("Conn", tmp);
						URL url = new URL(tmp);
						conn = doConnection(url, hr.is);
						is = conn.getInputStream();
						if(!stop)
							onResponse(is);
						sb.setLength(0);
						sb.append("read finished from URL: ").append(url.toString()).append("; at: ").append(Calendar.getInstance().getTime().toGMTString());
						l.debug(sb.toString());
						errCound = 0;
						
					}
				}
				catch (InterruptedException e)
				{
					StringBuilder tmp = new StringBuilder();
					tmp.append("waiting was breaked while waiting for new request: ");
					if(sb != null)
						tmp.append(sb);
					l.error(tmp.toString(), e);
				}
				catch (MalformedURLException e)
				{
					l.error("error on build url request: " + sb.toString(), e);
				}
				catch (IOException e) 
				{
					StringBuilder tmp = new StringBuilder();
					tmp.append("error on using received data: ");
					if(sb != null)
						tmp.append(sb);
					l.error(tmp.toString(), e);
				}
				catch(Exception e)
				{
					StringBuilder ss = new StringBuilder();
					ss.append("error when connecting to server:");
					if(sb != null)
						ss.append(sb);
					l.error(ss.toString(), e);
				}
				finally
				{
					Util.close(is);
					close(conn);
				}
				if(errCound == 20)
				{
					errCound = 0;
					synchronized (queue)
					{
						// if next ID != reqID means has got network error from some one else
						onConnectionError(getNextRequestID() == reqID);
					}
				}
			}
		}
	}

	/**
	 * get HTTP connection based on URL
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	protected HttpURLConnection getConnection(URL url, boolean isPost)throws IOException
	{
		String hostName = url.getHost();
		HttpURLConnection httpConn = HttpUtils.openConnection(ctx, url);
		// �ֹ�����Hostͷ��Ϣ��֧����������ͨ������ķ�ʽ�����Ա���ȥ�ֹ�����/etc/hosts���ƹ�dns�������鷳�������ǳ��򾭳��ڲ�ͬ�����������е�ʱ�򣬷ǳ�ʵ��
		httpConn.setRequestProperty("Host", hostName);
		httpConn.setRequestProperty("Authorization",
				"Basic bG92ZTIxY246amlheXVhbiFAIw==");
		httpConn.setReadTimeout(10000);
		httpConn.setConnectTimeout(5000);
		httpConn.setDoInput(true);
		httpConn.setUseCaches(false);
		httpConn.setRequestProperty("Connection", "Keep-Alive");
		httpConn.setRequestProperty("Charset", "UTF-8");
		httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		if(isPost)
		{
			httpConn.setDoOutput(true);
			httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		}
		return httpConn;
	}

	protected void postData(HttpURLConnection conn, InputStream src1)throws IOException
	{
		int readLen = 0;
		OutputStream os = conn.getOutputStream();
		os.write(("--" + boundary + "\r\n").getBytes());
		os.write("Content-Disposition: form-data; name=\"upload_file\"; filename=\"image.png\"\r\n".getBytes());
		os.write("Content-Type:image/png\r\n\r\n".getBytes());
		while((readLen = src1.read(data)) != -1)
		{
			os.write(data, 0, readLen);
		}
		os.write("\r\n".getBytes());
		
		
		if(param != null && param.indexOf("&") != -1){
			String[] tmpArr = param.split("&");
			String temp1 = "--" + boundary + "\r\n";
			for(int i=0 ; i<tmpArr.length; i++) {
				String name = tmpArr[i].split("=")[0];
				String value = tmpArr[i].split("=")[1];
				os.write(temp1.getBytes());
				String nameStr = "Content-Disposition: form-data; name="+name+"\r\n";
				os.write(nameStr.getBytes());
				os.write("\r\n".getBytes());
				String t1 = value + "\r\n";
				os.write(t1.getBytes());
			}
		}
		os.flush();
	}
	/**
	 * 
	 * @param url
	 * @param is
	 * @return
	 * @throws Exception
	 */
	protected byte[] data = new byte[512];
	protected HttpURLConnection doConnection(URL url, InputStream is)throws IOException
	{
		HttpURLConnection httpConn = null;
		int retryCount = 0;
		IOException uhe = null;
		while(true)
		{
			if(retryCount == 5)
				throw uhe;
			else
				uhe = null;
			
			try
			{
				httpConn = getConnection(url, (is != null));
				if(is != null)
					httpConn.setRequestMethod(POST);
				else
					httpConn.setRequestMethod(GET);
				StringBuilder sb = new StringBuilder();
				sb.append("connect to URL: ").append(url.toString()).append("; at: ").append(Calendar.getInstance().getTime().toGMTString());
				//l.debug(sb.toString());
				httpConn.connect();
				sb.setLength(0);
				sb.append("connected to URL: ").append(url.toString()).append("; at: ").append(Calendar.getInstance().getTime().toGMTString());
				//l.debug(sb.toString());
				if(is != null)
					postData(httpConn, is);
			}
			catch (IOException e)	// when no network connection some ROM fires this
			{
				uhe = e;
			}
			
			if(uhe != null)
				retryCount++;
			else
				break;
		}

		return httpConn;
	}

	/**
	 * generate login URL
	 * @return
	 */
	protected String[] genLoginURL()
	{
		String[] str = new String[2];
		StringBuilder loginUrl = getServerPrifix();
		loginUrl.append("sign/signoninfo.php?");
		str[0] = loginUrl.toString();
		loginUrl.setLength(0);
		int[] userInfoTypes = {
//				Profile.PROFILE_EMAIL,
//				Profile.PROFILE_SEX,
//				Profile.PROFILE_NICKNAME,
//				Profile.PROFILE_MATCH_MIN_AGE,
//				Profile.PROFILE_MATCH_MAX_AGE,
//				Profile.PROFILE_MATCH_MIN_HEIGHT,
//				Profile.PROFILE_MATCH_MAX_HEIGHT,
//				Profile.PROFILE_MATCH_CERTIFIED,
//				Profile.PROFILE_MATCH_MARRIAGE,
//				Profile.PROFILE_MATCH_EDUCATION,
//				Profile.PROFILE_MATCH_EDU_MORE_THAN,
//				Profile.PROFILE_MATCH_AVATAR,
//				Profile.PROFILE_MATCH_AVATAR_URL,
//				Profile.PROFILE_MATCH_WORK_LOCATION_1,
//				Profile.PROFILE_MATCH_WORK_SUBLOCATION_1,
//				Profile.PROFILE_WORK_LOCATION,
//				Profile.PROFILE_WORK_SUBLOCATION,
//				Profile.PROFILE_CHILDREN,
//				Profile.PROFILE_LEVEL,
//				Profile.PROFILE_INCOME,
//				Profile.PROFILE_ZODIAC,
//				Profile.PROFILE_BLOODTYPE,
//				Profile.PROFILE_HOUSE,
//				Profile.PROFILE_AUTO,
//				Profile.PROFILE_BELIEF,
//				Profile.PROFILE_INDUSTRY,
//				Profile.PROFILE_COMPANY,
//				Profile.PROFILE_HOME_LOCATION,
//				Profile.PROFILE_HOME_SUBLOCATION,
//				Profile.PROFILE_NATION,
//				Profile.PROFILE_CHARACTERS,
//				Profile.PROFILE_AVATAR_URL,
//				Profile.PROFILE_AVATAR,
//				Profile.PROFILE_BIRTHYEAR,
//				Profile.PROFILE_BIRTHDAY
				};
		loginUrl.append("name=").append(userName);
		loginUrl.append("&logmod=1");
		loginUrl.append("&password=").append(SHA1.Encrypt(pwd, "SHA-1"));
//		loginUrl.append("&password=").append(pwd);
		loginUrl.append("&channel=").append(ServicePool.getinstance().getConfig().getConfig(ConfigAccess.CHANNEL_ID));
		loginUrl.append("&clientid=").append(ServicePool.getinstance().getConfig().getConfig(ConfigAccess.CLIENT_AGENT));
		loginUrl.append("&reallogin=").append(reLogin);
		loginUrl.append("&ver=").append(Util.getVersionName(ctx));
		loginUrl.append("&deviceid=").append(Util.getDeviceId(ctx));
		loginUrl.append("&userinfotypes=[");
		for(int i=0;i<userInfoTypes.length;i++){
			loginUrl.append(userInfoTypes[i]);
			if(i<userInfoTypes.length-1){
			    loginUrl.append(",");
			}
		}
		loginUrl.append("]");
		
		str[1] = loginUrl.toString();
		return str;
	}
	
	public void logout()
	{
		userName = null;
		pwd = null;
		token = null;
	}
	
	public byte[] login(String username, String password)throws Exception
	{
		userName = username;
		pwd = password;
		l.info(username);
		l.info(password);
		String response = null;
		byte[] data = null;
		HttpURLConnection conn = null;
		InputStream is = null;
		try
		{
			String[] url = genLoginURL();
			conn = doConnection(new URL(url[0]+url[1]), null);
			is = conn.getInputStream();
			data = readDataFromIS(is);
			response = Util.toString(data);
			l.debug("login response: " + response);
			
			JSONObject j = new JSONObject(response);
			if(j.has("retcode")){
				lastLoginCode = j.getInt("retcode");
				if(lastLoginCode != 1)
				{
					l.debug("login with retcode:" + lastLoginCode);
//					onConnectionError(true);
					return null;
				}
				
				if(j.has("token")){
					token = j.getString("token");
				}
				if(j.has("userinfo")){
					sex = j.getJSONObject("userinfo").getString("2");
				}
			}
			reLogin = 0;
			return data;
		}
		catch (MalformedURLException e)
		{
			// should never come here
			throw new Exception(e);
		}catch(Exception e)
		{
			lastLoginCode = HttpServiceIntf.LOGIN_NO_ERROR;
			l.error("can't reach jiayuan server", e);
			onConnectionError(true);
			return null;
		}
		finally
		{
			Util.close(is);
			close(conn);
		}
	}

	/**
	 * get server prifix like: http://api.jiayuan.com:80/path/
	 */
	protected StringBuilder sbServer;
	protected StringBuilder getServerPrifix()
	{
		if (sbServer == null)
		{
			sbServer = new StringBuilder();
			sbServer.append("http://").append(hcr.server).append("/");
//			.append(":").append(hcr.serverPort);	// remark port for Google known issue
			if(hcr.serverBasicPath != null)
				sbServer.append(hcr.serverBasicPath).append("/");
		}

		return new StringBuilder(sbServer);
	}

	/**
	 * get location based server like: http://location.jiayuan.com:80/path/
	 */
	protected StringBuilder sbLocationServer;
	protected StringBuilder getLocationServerPrifix(boolean needBasePath)
	{
//		if (sbLocationServer == null)
		{
			sbLocationServer = new StringBuilder();
			sbLocationServer.append("http://").append(hcr.locationBasedServer).append("/");
//					.append(":").append(hcr.locationBasedServerPort)	// remark port for Google known issue
			if(needBasePath)
				if(hcr.locationBasedServerPath != null)
					sbLocationServer.append(hcr.locationBasedServerPath).append("/");
		}

		return new StringBuilder(sbLocationServer);
	}

	/**
	 * get location based server like: http://location.jiayuan.com:80/path/
	 */
	protected StringBuilder sbImageServer;
	protected StringBuilder getImageServerPrifix()
	{
		if (sbImageServer == null)
		{
			sbImageServer = new StringBuilder();
			sbImageServer.append("http://").append(replaceMobileDNS(hcr.imageServer)).append("/");
//					.append(":").append(hcr.imageServerPort)	// remark port for Google known issue
			if(hcr.imageServerPath != null)
				sbServer.append(hcr.imageServerPath).append("/");
		}

		return new StringBuilder(sbImageServer);
	}

	/**
	 * get DNS post fix for a server  
	 * @return
	 */
	protected String getSvrPostfix()
	{
		if(isCurrentCMCC())
			return "m";
		else
			return "c";
	}
	/**
	 * to replace src name with mobile DNS
	 * @param src
	 * @return
	 */
	protected static final String STR_DOT = ".";
	protected String replaceMobileDNS(String src)
	{
		if(isWIFIActive())
			return src;
		
		String strs[] = null;
		if((src.indexOf("images") != -1) ||
				(src.indexOf("photos") != -1))
		{
			strs = src.split("\\.");
			strs[0] = strs[0] + getSvrPostfix();
		}
		else if((src.indexOf("watermark") != -1) && isCurrentCMCC())
		{
			strs = src.split("\\.");
			strs[0] = strs[0] + getSvrPostfix();
		}
		
		if(strs != null)
		{
			StringBuilder sb = new StringBuilder();
			sb.append(strs[0]);
			for(int i=1; i<strs.length; i++)
				sb.append(STR_DOT).append(strs[i]);
			src = sb.toString();
		}
		
		return src;
	}
	
	/**
	 * check user selected image quality 
	 * @param baseUrl
	 * @return
	 */
	protected String checkImageQuality(String baseUrl)
	{
//		UserInfo ui = ServicePool.getinstance().getUserManager().getCurrentUser();
//		if(ui != null)
//		{
//			if(ui.imageQuality == UserInfo.IMAGE_QUALITY_HEIGHT)
//				baseUrl = baseUrl.replaceAll("_iphone", "");
//		}
		
		return baseUrl;
	}
	
	/**
	 * to check if current is in CMCC network
	 * @return
	 */
	protected Set<String> cmccOps = new HashSet<String>();
	{
		cmccOps.add("46000");
		cmccOps.add("46002");
		cmccOps.add("46007");
	}
	protected boolean isCurrentCMCC()
	{
		TelephonyManager telManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE); 
		String operator = telManager.getSimOperator();
		return cmccOps.contains(operator);
//		if(operator!=null){
//			if(operator.equals("46000") || operator.equals("46002") || operator.equals("46007")){ 
//			 //�й��ƶ� 
//			}else if(operator.equals("46001")){ 
//			 //�й���ͨ 
//			}else if(operator.equals("46003")){ 
//			 //�й���� 
//			} 
//		}
	}
	
	/**
	 * get current active network info
	 * @return
	 */
	protected NetworkInfo getActiveNetInfo()
	{
		ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		return connectivityManager.getActiveNetworkInfo();
	}
	
	/**
	 * check if current is WIFI active
	 * @return
	 */
	protected boolean isWIFIActive()
	{
		return getActiveNetInfo().getTypeName().contains("WIFI");
	}
	
	public boolean hasRequestInPending(Object id)
	{
		throw new RuntimeException("not been implemented");
	}

	public int getLastLoginErrorCode()
	{
		return lastLoginCode;
	}
	
	/**
	 * call on event received.
	 * @param source	The source who sends out the event.
	 * @param event		Event name.
	 */
	protected boolean isNetConnActive = true;
	protected String NETWORK_CONNECTION_ON = "com.jiayuan.service.NetStatusWatcher.NETWORK_CONNECTION_ON";
	protected String NETWORK_CONNECTION_OFF = "com.jiayuan.service.NetStatusWatcher.NETWORK_CONNECTION_OFF";
	public void onEvent(Object source, String event)
	{
		if(event.equals(NETWORK_CONNECTION_ON))
		{
			isNetConnActive = true;
		}
		else if(event.equals(NETWORK_CONNECTION_OFF))
		{
			isNetConnActive = false;
		}
		else if(event.equals(CONNECTION_ERROR))
		{
			onConnectionError(false);
		}
		
	}
	
	/**
	 * check network status start connect on ok and stop connect on error
	 */
	protected void checkSendThreadOnNetStatus()
	{
		if(isNetConnActive)
		{
			if(isStart)
				startService();
		}
		else
		{
			stopService();
		}
	}

	public String getToken() {
		return token;
	}

	
	public int getPendingReqCount()
	{
		synchronized (queue)
		{
			return queue.getPendingCount();
		}
	}

	public int getConnectionCount()
	{
		return 1;
	}
}
