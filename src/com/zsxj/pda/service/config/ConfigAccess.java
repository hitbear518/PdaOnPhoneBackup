package com.zsxj.pda.service.config;

/**
 * to access app configure
 * @author ice
 *
 */

public interface ConfigAccess {
	
	public static final String SOCKET_HOST = "socketHost";
	public static final String SOCKET_HOST_PORT = "socketHostPort";
	public static final String LIST_IMAGE_SUFFIX = "listImageSuffix";
	public static final String ITEM_IMAGE_SUFFIX = "itemImageSuffix";
	public static final String BIG_IMAGE_SUFFIX = "bigImageSuffix";
	
	public static final String CHANNEL_ID = "channelID";
	public static final String CLIENT_AGENT = "clientAgent";
	public static final String SOFTWARE_VERSION = "softver";
	public static final String SELLER_NICK = "sellerNick";
	public static final String SECRET = "secret";
	public static final String CALLBACK_URL = "callbackUrl";
	public static final String APPKEY = "appkey";
	
	/**
	 * get configure by given name
	 * @param name
	 * @return	related value or 'null' if no such name
	 */
	public String getConfig(String name);
	
	/**
	 * set configure value
	 * @param name
	 * @param value
	 */
	public void setConfig(String name, String value);
}
