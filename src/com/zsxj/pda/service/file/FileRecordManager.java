package com.zsxj.pda.service.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;
import com.zsxj.pda.service.ServicePool;
import com.zsxj.pda.util.Util;

/**
 * file cache serialiable
 * @author eyeshot
 *
 */
public class FileRecordManager {
	
	protected Log l = LogFactory.getLog(FileRecordManager.class);
	protected String fileSeiralInfo = "fileSeiralInfo";
	protected List<DataRecord<String>> fileList ; //file list info
	protected long cachedSize; // cachesize
	protected FileSerialInfo fsi; //file serialiable info
	protected static long currentCacheSize; 
	
	public long getCurrentCacheSize() {
		return currentCacheSize;
	}

	public void setCurrentCacheSize(long current) {
		currentCacheSize = current;
	}
	
	/**
	 * init data
	 */
	public void init()
	{
		fsi = (FileSerialInfo)readObject(fileSeiralInfo);
		if(null == fsi){
			fsi = new FileSerialInfo();
		}
		cachedSize = fsi.cachedSize;
		currentCacheSize = cachedSize;
		//fileList = (ArrayList<DataRecord<String>>)readObject(fileInfoName);
		fileList = fsi.fileList;
		if(null == fileList){
			fileList = new ArrayList<DataRecord<String>>();
		}
	}

	/**
	 * read serialiable data
	 * @return	object
	 */
	protected Object readObject(String name)
	{
		InputStream is = null;
		if(ServicePool.getinstance().getFileStore().isFileExistSDCardOrRam(name))
			is = ServicePool.getinstance().getFileStore().requestFile(name, null);
		Object rec = null;
		if(is != null)
		{
			try
			{
				ObjectInputStream ois = new ObjectInputStream(is);
				rec = ois.readObject();
			}
			catch (Exception e)
			{
			}
			finally
			{
				Util.close(is);
			}
		}
		
		return rec;
	}
	
	
	/**
	 * storeObject 
	 */
	public boolean storeObject(Object o, String name)
	{
		boolean result = true;
		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream output = new ObjectOutputStream(baos);
			output.writeObject(o);
			ByteArrayInputStream input = new ByteArrayInputStream(baos.toByteArray());
			ServicePool.getinstance().getFileStore().storeFileNoSerial(input, name);
		} catch(IOException e){
			l.error("store object has error in FileRecordManager ", e);
			result = false;
		}
		return result;
	}
	
	/**
	 * serialiable info(cacheSize, fileList)
	 */
	
	public void serialInfo(String fileName, long fileLength)
	{
		for(DataRecord<String> dr : fileList){
			if(dr.fileName!=null && dr.fileName.equals(fileName))return;
		}
		if(fileName == null || fileName.length() <= 0) return;
		DataRecord<String> dr = new DataRecord<String>();
		dr.fileName = fileName;
		fileList.add(dr);
		currentCacheSize += fileLength;
		fsi.fileList = fileList;
		fsi.cachedSize = currentCacheSize;
		boolean storefileSeiralInfo = storeObject(fsi, fileSeiralInfo); //save fileSeiralInfo Data
		if(!storefileSeiralInfo){ //storefileSeiralInfo failure
			l.debug("storefileSeiralInfo save failed...");
			ServicePool.getinstance().getFileStore().retireFile(fileName); //delete file
		}
		
	}
	
	/**
	 * serialiable info(cacheSize, fileList) for appendFile
	 */
	
	public void appendSerialInfo(String fileName, long fileLength)
	{
		if(fileName == null || fileName.length() <= 0) return;
		if(!fileList.contains(fileName)){
			DataRecord<String> dr = new DataRecord<String>();
			dr.fileName = fileName;
			fileList.add(dr);
		}
		currentCacheSize += fileLength;
		fsi.fileList = fileList;
		fsi.cachedSize = currentCacheSize;
		boolean storefileSeiralInfo = storeObject(fsi, fileSeiralInfo); //save fileSeiralInfo Data
		if(!storefileSeiralInfo){ //storefileSeiralInfo failure
			l.debug("storefileSeiralInfo save failed in appendSerialInfo method...");
			ServicePool.getinstance().getFileStore().retireFile(fileName); //delete file
		}
		
	}
	
	
	/**
	 * remove old files util cachedSize low app_cache_size
	 * @param cacheSize : cacheing File size
	 * @param appCacheSize : the max cache size 
	 */

	public boolean removeOldFiles(long addLength)
	{
		boolean removeFlag = true;
		boolean isDelete = false;
		while(currentCacheSize + addLength > FileUtils.APP_CACHE_SIZE * 1024 * 1024)
		{
			if(fileList.size() <= 0)
			{
				isDelete = true;
				currentCacheSize = 0;
				new Thread(new Runnable()
				{
					public void run() 
					{
						ServicePool.getinstance().getFileStore().deleteFilesInFileStore();
					}
				}).start();
				if(FileUtils.isSDCardExist())
				{
					if(currentCacheSize + addLength > FileUtils.SDCARD_ALERT_SIZE * 1024 * 1024)
					{
						removeFlag = false;
						break;
					}
				} else 
				{
					if(currentCacheSize + addLength > FileUtils.RAM_ALERT_SIZE * 1024 * 1024)
					{
						removeFlag = false;
						break;
					}
				}
				
				break;
			}
			DataRecord<String> dr = fileList.get(0);
			if(null != dr)
			{
				ServicePool.getinstance().getFileStore().retireFile(dr.fileName); //delete oldest file
			}
			fileList.remove(0);
		}
		if(!isDelete)
		{
			fsi.cachedSize = currentCacheSize;
			fsi.fileList = fileList;
			boolean storeResult = storeObject(fsi, fileSeiralInfo);//update the file serialiable file
			//l.error("the fileList size  is " + fileList.size());
			if(!storeResult) //storeFile failure
			{ 
				l.debug("fileSerialInfo save failed...");
			}
		}
		
		return removeFlag;
	}

}
