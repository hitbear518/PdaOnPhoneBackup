package com.zsxj.pda.service;

import android.content.Context;
import android.content.Intent;

import com.zsxj.pda.log.LogManager;
import com.zsxj.pda.service.config.ConfigAccess;
import com.zsxj.pda.service.config.ConfigAccessImpl;
import com.zsxj.pda.service.event.EventCenter;
import com.zsxj.pda.service.file.FileRecordManager;
import com.zsxj.pda.service.file.FileStore;
import com.zsxj.pda.service.file.HandleFile;
import com.zsxj.pda.service.http.HTTPMassDownload;
import com.zsxj.pda.service.http.HttpServiceImpl;
import com.zsxj.pda.service.http.HttpServiceIntf;
import com.zsxj.pda.service.network.NetStatusWatcher;
import com.zsxj.pda.service.params.ParamsManager;
import com.zsxj.pda.service.params.ParamsManagerImpl;
import com.zsxj.pda.service.update.AppUpdateService;

public class ServicePool {
	
	protected static ServicePool instance = null;
	protected FileStore fileStore = null;
	protected FileRecordManager frm;
	protected ConfigAccess ca = null;
	protected EventCenter eventCenter = null;
	protected HandleFile handleFile= null;
	protected HttpServiceIntf httpIntf = null;
	protected HTTPMassDownload hmd;
	protected NetStatusWatcher nsw;
	protected ParamsManager pmgr = null;
	protected AppUpdateService aus;
	
	protected ServicePool() {
		
	}
	
	public static ServicePool getinstance() {
		
		if(null == instance) {
			instance = new ServicePool();
			instance.eventCenter = new EventCenter();
		}
		
		return instance;
	}

	/**
	 * call to do service stop
	 * @param ctx
	 */
	public void stop(Context ctx) {
		
		ctx.stopService(new Intent("com.ice.alarm.StartServicePoolService"));
		aus.stop();
		httpIntf.stop();
		hmd.stop();
		nsw.clear(ctx);
	}
	
	/**
	 * check if service pool has been initialed.
	 * @return
	 */
	public synchronized boolean isInitReady() {
		
		return (null != eventCenter && null != ca);
	}
	
	/**
	 * to initial all services
	 * 
	 * Here each service initial should be in a specific sequence,
	 * for some service need others as dependence.
	 * 
	 * @param ctx	
	 */
	public synchronized void init(Context ctx)throws Exception {
		
		LogManager.manager.init(ctx);
		
		eventCenter = new EventCenter();
		
		nsw = new NetStatusWatcher();
		nsw.init(ctx);
		
		ConfigAccessImpl cai = new ConfigAccessImpl();
		cai.init(ctx);
		ca = cai;
		
		HttpServiceImpl hsi = new HttpServiceImpl();
		hsi.init(ctx);
		eventCenter.registerListener(hsi, HttpServiceIntf.CONNECTION_ERROR);
		hsi.start();
		httpIntf = hsi;
		
		hmd = new HTTPMassDownload();
		hmd.init(ctx);
		hmd.start();
		
		frm = new FileRecordManager(); //this define must be prior to " fileStore = new FileStore();"
		fileStore = new FileStore();
		fileStore.setContext(ctx);
		frm.init();
		handleFile = new HandleFile();

		pmgr = new ParamsManagerImpl();
		pmgr.init(ctx);
		
		ctx.startService(new Intent("com.ice.alarm.StartServicePoolService"));
		
		aus = new AppUpdateService();
		aus.init(ctx);
	}

	
	public FileStore getFileStore() {
		
		return fileStore;
	}
	
	public FileRecordManager getFileRecordManager() {
		
		return frm;
	}
	
	public ConfigAccess getConfig() {
		
		return ca;
	}
	
	public EventCenter getEventCenter() {
		
		return eventCenter;
	}
	
	public HandleFile getHandleFile() {
	
		return handleFile;
	}
	
	public HttpServiceIntf getHttpService() {
		
		return httpIntf;
	}
	
	public HttpServiceIntf getMassDownloadService() {
		
		return hmd;
	}
	
	public NetStatusWatcher getNetStatusWatcher() {
		
		return nsw;
	}
	
	public ParamsManager getParamsManager() {
		
		return pmgr;
	}
	
	public AppUpdateService getAppUpdateService() {
		
		return aus;
	}
	
}
