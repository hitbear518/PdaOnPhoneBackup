package com.zsxj.pda.service.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;
import com.zsxj.pda.service.ServicePool;
import com.zsxj.pda.service.http.HttpRequesterIntf;
import com.zsxj.pda.util.MD5;

/**
 * operate file
 * @author eyeshot
 *
 */
public class FileStore implements HttpRequesterIntf{
	protected static Context mContext;
	protected static String SDPATH;
	protected static int FILESIZE = 4 * 1024; 
	protected static String RAMPATH;
	protected static String storePath = "";
	protected static boolean isStorageReset = true; //the whole fileStore flag which show file can or not cache
	@SuppressWarnings("unchecked")
	protected  HashMap cacheMap = new HashMap();
	protected Log l = LogFactory.getLog(FileStore.class);
	protected FileRecordManager frm = ServicePool.getinstance().getFileRecordManager();
	public void init() {
		XmlReader xmlReader = new XmlReader();
		FileConfig fc = xmlReader.getFileConfig(mContext);
		storePath = fc.getStorePath();
		SDPATH = Environment.getExternalStorageDirectory() +  "/"; //get Extenrnal Directory( /SDCARD )
		RAMPATH = mContext.getFilesDir().getPath();//get RamData Directory
		executeCreateDir(); //create folder
		registerSDCardListener(); //check SDCard status
	}
	public  boolean isCachable() {
		return isStorageReset;
	}
	public static void setCachable(boolean isCachable) {
		FileStore.isStorageReset = isCachable;
	}
	public synchronized void setContext(Context ctx) throws Exception{
		mContext = ctx;
		init();
	}
	protected  String getSDPATH(){
		return SDPATH;
	}
	protected String getRAMPATH(){
		return RAMPATH;
	}
	
	public boolean isFileExist(String fileName)
	{
		return (isFileExistSDCardAndRam(fileName) != null); 
	}
	
	/**
	 * request file by fileName, the return InputStream should be close after used
	 * @param fileName  fileName
	 * @param requester FileRequester Interface
	 * @return
	 */
	public  InputStream requestFile(String fileName, FileRequester fileRequester){
		File f = isFileExistSDCardAndRam(fileName);
		if(f != null)
			try
			{
				return new FileInputStream(f);
			}
			catch (FileNotFoundException e)
			{
				l.error("error on gen read stream for file:" + fileName, e);
			}
		return null;
	}
	/**
	 * store file in sdcard or ram
	 * @param is
	 * @param fileName
	 */
	public synchronized boolean storeFile(InputStream is, String fileName) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		boolean isSuccess = true;
 		if(!isStorageReset){
 			isSuccess = false;
 		}
 		else
 		{
 			if(FileUtils.isSDCardExist()){ //check sdCard exsit
 				sb.append(getSDPATH()).append(storePath).append("/").append(MD5.md5(fileName));
 				isSuccess = writeFile(sb.toString(), fileName, is, mContext, FileUtils.getSDCardAvailableSize());
 			} else {
 				sb.append(RAMPATH).append("/").append(storePath).append("/").append(MD5.md5(fileName));
 				isSuccess = writeFile(sb.toString(), fileName, is, mContext, FileUtils.getRamAvailableSize(mContext));
 			}
 		}

 		if(isStorageReset && !isSuccess){
 			isStorageReset = false;
 			throw new IOException(); //store files failure
 		}
 		return isSuccess;
	}
	
	/**
	 * append file in sdcard or ram
	 * @param input
	 * @param offset
	 * @param count
	 * @param fileName
	 */
	public synchronized boolean appendFile(byte[] input, int offset, int count, String fileName) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		boolean isSuccess = true;
 		if(!isStorageReset)
 			isSuccess = false;
 		else
 		{
 			if(FileUtils.isSDCardExist()){ //check sdCard exsit
 				sb.append(getSDPATH()).append(storePath).append("/").append(MD5.md5(fileName));
 				isSuccess = doAppendFile(sb.toString(), fileName, input, offset, count, mContext, FileUtils.getSDCardAvailableSize());
 			} else {
 				sb.append(RAMPATH).append("/").append(storePath).append("/").append(MD5.md5(fileName));
 				isSuccess = doAppendFile(sb.toString(), fileName, input, offset, count, mContext, FileUtils.getRamAvailableSize(mContext));
 			}
 		}

 		if(isStorageReset && !isSuccess){
 			isStorageReset = false;
 		}
 		return isSuccess;
	}
	/**
	 * store file in sdcard or ram with on serialiable
	 * @param is
	 * @param fileName
	 */
	public void storeFileNoSerial(InputStream is, String fileName) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		if(FileUtils.isSDCardExist()){ 
			sb.append(getSDPATH()).append(storePath).append("/").append(MD5.md5(fileName));
		} else {
			sb.append(RAMPATH).append("/").append(storePath).append("/").append(MD5.md5(fileName));
		}
		writeFileNoSerial(sb.toString(), is);
	}	
	

	public void onFileDownloaded(Object id, InputStream data) 
	{
		
		FileRequester fileReq = (FileRequester)cacheMap.get("fileReq");
		fileReq.onFileReady(id, data);
		
	}
	
	public synchronized void retireFile(String fileName)
	{
	    String sourceName = new String(fileName);
		StringBuilder sb = new StringBuilder();
		fileName = MD5.md5(fileName);
		if(FileUtils.isSDCardExist()){
			sb.append(getSDPATH()).append(storePath).append("/").append(fileName);
		} else {
			sb.append(RAMPATH).append("/").append(storePath).append("/").append(fileName);
		}
		File file = new File(sb.toString());
		if(file.exists()){
			long fileLength = file.length();
			if(file.delete()) {
				FileSerialInfo serialInfo = (FileSerialInfo)frm.readObject("fileSeiralInfo");
				if(null != serialInfo && serialInfo.cachedSize >= 0) {
					for(DataRecord<String> dr : frm.fileList){
						if(sourceName.equals(dr.fileName)){
							serialInfo.cachedSize = serialInfo.cachedSize - fileLength;
							frm.setCurrentCacheSize(serialInfo.cachedSize);
							frm.storeObject(serialInfo, "fileSeiralInfo");
							break;
						}
					}
				}
			}
		}
	}
	public void onReponse(Object id, InputStream data) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * creat file on SDCARD
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	protected File createSDFile(String fileName) throws IOException{
		//fileName = MD5.md5(fileName);
		File file = new File(fileName);
		if(file.exists()){
			file.delete();
		}
		try{
			file.createNewFile();
		} catch(IOException e){
			executeCreateDir();
			createSDFile(fileName);
		}
		return file;
	}
	
	/**
	 * create directory 
	 * @param dirName
	 * @return
	 */
	protected  File createDir(String dirName){
		File dir = new File(dirName);
		if(!dir.exists()){
			dir.mkdirs();
		}
		return dir;
	}
	
	/**
	 * check file exist on SDCARD or RAM
	 * @param fileName
	 * @return
	 */
	protected boolean isFileExistSDCardOrRam(String fileName){
		fileName = MD5.md5(fileName);
		StringBuilder sb = new StringBuilder();
		//if(storePlace.equals("outer")){
		if(FileUtils.isSDCardExist()){
			sb.append(SDPATH).append(storePath).append("/").append(fileName);
		} else {
			sb.append(RAMPATH).append("/").append(storePath).append("/").append(fileName);
		}
		File file = new File(sb.toString());
		return file.exists();
	}
	
	/**
	 * check file exist on SDCARD and RAM
	 * @param fileName
	 * @return
	 */
	protected File isFileExistSDCardAndRam(String fileName){
		fileName = MD5.md5(fileName);
		StringBuilder sb = new StringBuilder();
		//if(storePlace.equals("outer")){
		if(FileUtils.isSDCardExist()){
			sb.append(SDPATH).append("/").append(storePath).append("/").append(fileName);
			File file = new File(sb.toString());
			if(file.exists())
				return file;
		} 
		sb = new StringBuilder();
		sb.append(RAMPATH).append("/").append(storePath).append("/").append(fileName);
		File file = new File(sb.toString());
		if(file.exists())
			return file;
		else
			return null;
	}
	
	
	
	/**
	 * put inputStream data into ram
	 * @param path
	 * @param fileName
	 * @param input
	 * @return
	 */
	protected boolean writeFile(String path,String fileName,InputStream input, Context mContext, long availableSize)throws IOException{
		OutputStream output = null;
		boolean successFlag = true;
		try {
			output = new FileOutputStream(path);
			byte[] buffer = new byte[FILESIZE];
			int readLen = 0;
			long addLength = 0;
			while((readLen = input.read(buffer)) != -1){
				long currentCacheSize = frm.getCurrentCacheSize();
				addLength += readLen;
				if(availableSize < FileUtils.SDCARD_ALERT_SIZE || 
						currentCacheSize + addLength > FileUtils.APP_CACHE_SIZE * 1024 * 1024)
				{
					if(!frm.removeOldFiles(addLength)){
						successFlag = false; //there is not enough space to save files
						break;
					}
				} 
				output.write(buffer, 0, readLen);
			}
			output.flush();
			//l.error("the currentCacheSize is " + frm.getCurrentCacheSize()/1024/1024.000);
			frm.serialInfo(fileName, addLength); //serialiable fileName and fileLength
		} 
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			l.error("error on FileStore's writefile : ", e);
		}
		finally{
			try {
				if(output != null)
					output.close();
			} catch (IOException e) {
				l.error("error on FileStore's writefile finally method : ", e);
			}
		}
		return successFlag;
	}
	
	/**
	 * execute append data
	 * @param path
	 * @param fileName
	 * @param input
	 * @return
	 */
	protected boolean doAppendFile(String path,String fileName,byte[] input, int offset, int count, Context mContext,
			                       long availableSize)throws IOException{
		OutputStream output = null;
		boolean successFlag = true;
		try {
			output = new FileOutputStream(path, true);
			int readLen = count;
			long currentCacheSize = frm.getCurrentCacheSize();
			if(availableSize < FileUtils.SDCARD_ALERT_SIZE || 
					currentCacheSize + readLen > FileUtils.APP_CACHE_SIZE * 1024 * 1024)
			{
				if(!frm.removeOldFiles(readLen)){
					successFlag = false; //there is not enough space to save files
				}
			} 
			//l.error("the currentCacheSize is " + frm.getCurrentCacheSize()/1024/1024.000);
			if(successFlag)
			{
				output.write(input, offset, count);
				frm.appendSerialInfo(fileName, readLen); //serialiable fileName and fileLength
			}
			output.flush();
		} 
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			l.error("error on FileStore's doAppendFile : ", e);
		}
		finally{
			try {
				if(output != null)
					output.close();
			} catch (IOException e) {
				l.error("error on FileStore's doAppendFile finally method : ", e);
			}
		}
		return successFlag;
	}
	
	/**
	 * write file with no serialiable
	 * @param path
	 * @param fileName
	 * @param input
	 * @return
	 */
	protected void writeFileNoSerial(String path,InputStream input)throws IOException{
		OutputStream output = null;
		try {
			output = new FileOutputStream(new File(path));
			byte[] buffer = new byte[FILESIZE];
			int readLen = 0;
			while((readLen = input.read(buffer)) != -1){
				output.write(buffer, 0, readLen);
			}
			output.flush();
		} 
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			l.error("error on FileStore's writeFileNoSerial : ", e);
		}
		finally{
			try {
				if(output != null)
					output.close();
			} catch (IOException e) {
				l.error("error on FileStore's writeFileNoSerial finally method : ", e);
			}
		}
	}
	

	/**
	 * get inputStream from file
	 * @param fileName fileName
	 * @return  inputStream
	 */
	protected InputStream getFile(String path){
		FileInputStream is = null;
		try {
			is = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			//l.error("error on FileStore's getFile : ", e);
			is = null;
		}
		return is;
	}
	/**
	 * create directory on SDCARD or RAM
	 */
	protected void executeCreateDir(){
		String[] storeDirArray = storePath.split("/");
		StringBuilder sb = new StringBuilder();
		if(FileUtils.isSDCardExist()){ //check sdCard exist
			sb.append(SDPATH).append(storeDirArray[0]);
			createDir(sb.toString());
			sb.append("/").append(storeDirArray[1]);
			createDir(sb.toString());
		}
		
		//create file directory in ram
		sb = new StringBuilder();
		sb.append(RAMPATH).append("/").append(storeDirArray[0]);
		createDir(sb.toString());
		sb.append("/").append(storeDirArray[1]);
		createDir(sb.toString());
		
	}
	
	  // listener SDCard status
    private void registerSDCardListener(){
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addDataScheme("file");
        mContext.registerReceiver(sdcardListener, intentFilter);
    }
    
	private final BroadcastReceiver sdcardListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	l.debug("storage is reset.................");
        	isStorageReset = true;
        	executeCreateDir();
        	frm.init();
        }
    };
    /**
     * delete files in /filestore/files
     */
    public  void deleteFilesInFileStore()
	{
		StringBuilder sb = new StringBuilder();
		if(FileUtils.isSDCardExist()){
			sb.append(getSDPATH()).append(storePath);
		} else {
			sb.append(RAMPATH).append("/").append(storePath);
		}
		File file = new File(sb.toString());
		if(file.isDirectory())
		{
			File[] files = file.listFiles();
			if(files !=null && files.length > 0)
			{
				for(int i = 0; i < files.length; i++)
				{
					File oneFile = files[i];
					oneFile.delete();
				}
			}
		}
	}

}
