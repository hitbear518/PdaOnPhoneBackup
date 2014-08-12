package com.zsxj.pda.service.file;

import java.io.InputStream;

public interface FileRequester {
	/**
	 * file get loaded
	 * 
	 * This should be a THREAD SAFE implementation
	 * @param id the file name
	 * @param data the fileStream
	 */
	public void onFileReady(Object id, InputStream data);

}
