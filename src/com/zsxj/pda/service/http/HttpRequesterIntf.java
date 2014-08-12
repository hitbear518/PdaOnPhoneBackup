package com.zsxj.pda.service.http;

import java.io.IOException;
import java.io.InputStream;

/**
 * To receive http response
 * @author ice
 *
 */

public interface HttpRequesterIntf
{
	/**
	 * invoke on response received
	 * @param id the ID generated when add request to service
	 * @param data	Whole received data
	 */
	public void onReponse(Object id, InputStream data)throws IOException;
	
	/**
	 * notify when request file download finished
	 * @param id the ID generated when add request to service
	 */
	public void onFileDownloaded(Object id, InputStream data)throws IOException;
}
