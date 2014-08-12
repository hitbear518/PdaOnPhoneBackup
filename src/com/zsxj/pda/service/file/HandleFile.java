package com.zsxj.pda.service.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;
import com.zsxj.pda.service.ServicePool;
import com.zsxj.pda.service.http.HttpRequesterIntf;
import com.zsxj.pda.service.http.HttpServiceIntf;
import com.zsxj.pda.util.Util;

public class HandleFile implements HttpRequesterIntf {
	
	Log l = LogFactory.getLog(HandleFile.class);
	protected FileStore fs = ServicePool.getinstance().getFileStore();
	protected HttpServiceIntf hsi = ServicePool.getinstance().getMassDownloadService();
	protected FileRequester fr = null;
	protected Map<Object, String> hashMap = new HashMap<Object, String>();
	protected Map<Object, FileRequester> requesterMap = new HashMap<Object, FileRequester>();
	protected Map<String, Object> fileName2IdMap = new HashMap<String, Object>();
	protected Map<Object, String> id2TypeMap = new HashMap<Object, String>();
	protected List<String> cacheUrl = new ArrayList<String>();
	/**
	 * request file with name. When file ready will call FileRequester on onFileReady.
	 * @param fileName
	 * @param fileRequester
	 * @param type	Here type must be data type definition of HttpServiceintf
	 */
	public InputStream getFile(String fileName, FileRequester fileRequester, String type){
		InputStream is = fs.requestFile(fileName, null);
		if(is == null && !cacheUrl.contains(fileName)){
			synchronized(this){
				    Object id = hsi.addRequest(this, new String[]{fileName, null}, type, HttpServiceIntf.SERVER_TYPE_NORMAL);
					hashMap.put(id, fileName);
					requesterMap.put(id, fileRequester);
					fileName2IdMap.put(fileName, id);
					id2TypeMap.put(id, type);
					cacheUrl.add(fileName);
				}
		}
		return is;
	}

	public void onFileDownloaded(Object id, InputStream data) {
		
	}

	public void onReponse(Object id, InputStream is) throws IOException {
		String fileName = null;
		FileRequester fr = null;
		synchronized(this){
			fileName = (String)hashMap.get(id);
			fr = requesterMap.get(id);
		}
		
		try{
			if(null != is && fs.isCachable()){
		    	fs.storeFile(is, fileName);
		    }
		} catch(IOException e){
			l.error("onFileReady method has error in HandleFile", e);
		} 
		boolean isFileCached = false;
		InputStream requestFile = null;
		try{
			if(fs.isCachable()){
				requestFile = fs.requestFile(fileName, null);
				synchronized (this) {
					isFileCached = true;
				}
			} else {
				requestFile = is;
			}
			fr.onFileReady(fileName, requestFile);
		} finally {
			synchronized (this) {
				if(isFileCached){
					Util.close(requestFile);
				}
			}
		}
		
		synchronized (this)
		{
			if(hashMap.containsKey(id))
			{
				hashMap.remove(id);
				fileName2IdMap.remove(fileName);
			}
			if(cacheUrl.contains(id))
			{
				cacheUrl.remove(id);
			}
		}
	}	
	/**
	 * cancel file request 
	 * @param fileName  : request File url
	 * @param fileRequester : the class which invoke handleFile
	 */
	public void cancelRequestFile(String fileName, FileRequester fileRequester){
		synchronized(this) {
			Object id = fileName2IdMap.get(fileName);
			FileRequester fr = requesterMap.get(id);
			if(fr!=null && fileRequester!=null && fr.getClass().getName().equals(fileRequester.getClass().getName())){
				hsi.removeRequestByID(id);
			}
		}
	}


}
