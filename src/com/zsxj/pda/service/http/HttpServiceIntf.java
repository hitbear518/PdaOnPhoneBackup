package com.zsxj.pda.service.http;

import java.io.InputStream;

public interface HttpServiceIntf
{
	public static String REQUEST_TYPE_DATA = "REQUEST_TYPE_DATA";	// JASON request
	public static String REQUEST_TYPE_DIRECT = "REQUEST_TYPE_DIRECT";	// the request with "http://server"
	public static String REQUEST_TYPE_RESIZED_IMAGE = "REQUEST_TYPE_RESIZED_IMAGE";		// to request resized image
	
	public static String SERVER_TYPE_LOCATION = "SERVER_TYPE_LOCATION";
	public static String SERVER_TYPE_NORMAL = "SERVER_TYPE_NORMAL";
	
	public static String CONNECTION_ERROR = "com.jiayuan.http.ConnectionError";
	public static int LOGIN_NO_ERROR = Integer.MIN_VALUE;
	public static int LOGIN_ERROR_BLACK = -1;
	public static int LOGIN_ERROR_ON_BLACK = -5;
	public static int LOGIN_ERROR_ID_PWD_ERROR = 2;
	
	/**
	 * add request.
	 * 
	 * Currently when return 'null' it means connection error in processing.
	 * 
	 * @param sender
	 * @param request	Request string in array, like: String[api/request.php][?para1=1&para2=2]
	 * @param requestType
	 * @param serverType
	 * @return	ID of the request
	 */
	public Object addRequest(HttpRequesterIntf sender, String[] request, String requestType, String serverType);
	
	/**
	 * add post request
	 * 
	 * Currently when return 'null' it means connection error in processing.
	 * 
	 * @param sender
	 * @param request	Request string in array, like: String[api/request.php][?para1=1&para2=2]
	 * @param is	Content to post
	 * @param serverType
	 * @return
	 */
	public Object addPost(HttpRequesterIntf sender, String[] request, InputStream is,String requestType, String serverType);
	
	/**
	 * remove all requests for sender
	 * @param sender
	 */
	public void removeRequestBySender(HttpRequesterIntf sender);
	
	/**
	 * remove request with ID
	 * @param id	The ID generated by addRequest()
	 */
	public void removeRequestByID(Object id);
	
	/**
	 * 
	 * @param username
	 * @param password
	 * @return	raw data from server response, or 'null' if not login then can check error code from server with getLastLoginErrorCode()
	 * 
	 * @throws When any exception happened. This means login faulty
	 */
    public byte[] login(String username,String password)throws Exception;
    
    /**
     * logout current user
     */
    public void logout();
    
    /**
     * 
     * @param id
     * @return
     */
    public boolean hasRequestInPending(Object id);
    
    /**
     * get last login error code
     * @return
     */
    public int getLastLoginErrorCode();

    /**
     * stop the service
     */
    public void start();

    /**
     * stop the service
     */
    public void stop();
    
    /**
     * return the current token
     * @return
     */
    public String getToken();
    
    /**
     * get current usable connection count.
     * both of working & idle
     * @return
     */
    public int getConnectionCount();
}
