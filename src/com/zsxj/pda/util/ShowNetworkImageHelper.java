package com.zsxj.pda.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;
import com.zsxj.pda.service.ServicePool;
import com.zsxj.pda.service.event.EventListener;
import com.zsxj.pda.service.file.FileStore;
import com.zsxj.pda.service.http.HttpRequesterIntf;
import com.zsxj.pda.service.http.HttpServiceIntf;
import com.zsxj.pda.util.SoftReferenceMap;
import com.zsxj.pda.util.Util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageSwitcher;
import android.widget.ImageView;

/**
 * help to maintain image request things
 * 
 * This is not a thread safe implementation
 * 
 * @author ice
 *
 */

public class ShowNetworkImageHelper implements HttpRequesterIntf, EventListener
{
	protected Map<View, String> img2URL = new HashMap<View, String>();
	protected Map<Object, String> id2URL = new ConcurrentHashMap<Object, String>();
	protected List<String> pendingUrls = new LinkedList<String>();	// holds all pending URLs
	protected List<String> doingUrls = new LinkedList<String>();	// holds all URLs in requesting
	protected SoftReferenceMap<String, Bitmap> imgCache = new SoftReferenceMap<String, Bitmap>();
	protected Bitmap defaultImg;
	protected Resources resource;
	protected boolean stoped;	// marks if request image from network has been stoped
	protected boolean metNetworkError;
	protected ResponseHandler uiHandler = new ResponseHandler();
	protected static final int MSG_IMAGE_READY = 1;		// notify image downloaded
	protected static final int MSG_NETWORK_ERROR = 2;	// notify network error
	protected static final int MSG_IMAGE_LOADED = 3;		// notify image has been loaded into memory
	protected Semaphore sem = new Semaphore(1, false);
	public volatile boolean isScrolling;
	protected Log l = LogFactory.getLog(ShowNetworkImageHelper.class);
	
	protected Map<Object, Integer> id2rc = new ConcurrentHashMap<Object, Integer>();	
	public Bitmap readImg(String url)
	{		
		InputStream is = null;
		Bitmap b = null;
		if(sem.tryAcquire())
		{
			try
			{
				is = ServicePool.getinstance().getFileStore().requestFile(url, null);
				b = BitmapFactory.decodeStream(is);
			}
			finally
			{
				Util.close(is);
				sem.release();
			}
		}
		
		return b;
	}
	
	/**
	 * update ImageView. if not cached the request download
	 * @param iv
	 * @param url
	 */
	public void updateImageView(View iv, String url)
	{
		if(resource == null)
			throw new RuntimeException("resource has not been set");
		
		Bitmap img = imgCache.get(url);
		if(img == null)
			img = readImg(url);
		
		if(img != null)
		{
			if(!imgCache.containsKey(url))
				imgCache.put(url, img);
			img2URL.remove(iv);
//			iv.setImageBitmap(img);
			if(iv instanceof ImageView){
				((ImageView) iv).setImageBitmap(img);
			}else 
			if(iv instanceof ImageSwitcher){
				((ImageSwitcher) iv).setImageDrawable(new BitmapDrawable(img));
			}
		}
		else{
//			if(iv instanceof ImageView){
//				((ImageView) iv).setImageResource(R.drawable.loading);
//			}else 
//			if(iv instanceof ImageSwitcher){
//				((ImageSwitcher) iv).setImageResource(R.drawable.loading);
//			}
			requestImage(iv, url);
		}
	}
	
	/**
	 * request image for a URL
	 * @param iv
	 * @param item
	 */
	protected void requestImage(View iv, String url)
	{
		img2URL.put(iv, url);
		if(!id2URL.containsValue(url))
		{
			if(pendingUrls.size() > 0)
			// remove previous call
			pendingUrls.remove(url);
			
			// make the last request the first to request
			pendingUrls.add(0, url);
			
			requestPendingURLs();
		}
	}
	
	/**
	 * request pending URLs if there is
	 */
	protected void requestPendingURLs()
	{
		if(metNetworkError || stoped || (pendingUrls.size() == 0))
			return;
		HttpServiceIntf download = ServicePool.getinstance().getMassDownloadService();
		while((id2URL.size() < download.getConnectionCount()) && (pendingUrls.size() > 0))
		{
			String url = pendingUrls.remove(0);
			if(!doingUrls.contains(url))
			{
				doingUrls.add(url);
				Object id = download.addRequest(this, new String[]{url, null}, HttpServiceIntf.REQUEST_TYPE_DIRECT, null);
				if(id != null)
					id2URL.put(id, url);
			}
		}
	}
	
	/**
	 * get all ImageViews binded to URL
	 * @param url
	 * @return	view set or 'null' if no proper views
	 */
	public Set<View> getViewsBindedToUrl(String url)
	{
		Set<View> ivs = new HashSet<View>();
		for(View iv : img2URL.keySet())
		{
			if(img2URL.get(iv).compareToIgnoreCase(url) == 0)
				ivs.add(iv);
		}
		
		if(ivs.size() > 0)
			return ivs;
		else
		{
			for(View iv : img2URL.keySet())
			{
				if(img2URL.get(iv).compareToIgnoreCase(url) == 0)
					ivs.add(iv);
			}
			return null;
		}
	}
	
	/**
	 * to try to store download image
	 * @param id
	 * @param data
	 * 
	 * @return 'true' if process is ok or 'false' no pending URL related with ID
	 * @throws IOException
	 */
	protected boolean storeHeaderImage(Object id, InputStream data)throws IOException
	{
		String url = id2URL.get(id);
		InputStream is = null;
		boolean succ = false;

		try
		{
			FileStore fs = ServicePool.getinstance().getFileStore();
			byte[] d = new byte[20480];
			int len = 0;
			try
			{
				while((len = data.read(d)) != -1)
				{
					sem.acquire();
					fs.appendFile(d,0,len,url);
					sem.release();
				}
				succ = true;
			}
			catch (IOException e)
			{
				sem.release();
				fs.retireFile(url);
				throw e;
			}
			finally
			{
				Util.close(is);
			}
		}
		catch (InterruptedException e)
		{
		}
		
		return succ;
	}
	
	/**
	 * on request thumbnail download finished
	 * @param url
	 */
	protected void thumbnailReady(String url)
	{
		Set<View> ivs = getViewsBindedToUrl(url);
		
		if(ivs == null)
			return;

		Bitmap b = readImg(url);
		if(b == null)
		{
			if(!isScrolling)
			{
				ReadImg ri = new ReadImg();
				ri.url = url;
				ri.start();
			}
		}
		else
		{
			for(View iv : ivs)
			{
//				iv.setImageBitmap(b);
				if(iv instanceof ImageView){
					((ImageView) iv).setImageBitmap(b);
				}else 
				if(iv instanceof ImageSwitcher){
					((ImageSwitcher) iv).setImageDrawable(new BitmapDrawable(b));
				}
				img2URL.remove(iv);
			}
			imgCache.put(url, b);
		}
	}
	
	/**
	 * to handle image cached message & update related ImageView
	 * @author ice
	 *
	 */
	class ResponseHandler extends Handler
	{
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case MSG_IMAGE_LOADED:
				{
					String url = (String)msg.obj;
					thumbnailReady(url);
				}
					break;
				case MSG_IMAGE_READY:
				{
					String url = id2URL.remove(msg.obj);
					doingUrls.remove(url);
					thumbnailReady(url);
					requestPendingURLs();
				}
					break;
				case MSG_NETWORK_ERROR:
					metNetworkError = true;
					id2URL.clear();
					pendingUrls.clear();
					break;
				default:
					break;
			}
		}
	}
	
	protected void sendReq(Object id){
		ServicePool.getinstance().getHttpService().addRequest(this, new String[]{id2URL.get(id), null}, HttpServiceIntf.REQUEST_TYPE_DIRECT, null);
	}

	public void onReponse(Object id, InputStream data)throws IOException
	{
		//if data is null, To send request retry count is 3
		if (data == null && id2rc.get(id) == null)
		{
			sendReq(id);
			id2rc.put(id, 1);
		}
		else if (data == null && id2rc.get(id) < 3)
		{
			sendReq(id);
			id2rc.put(id, id2rc.get(id) + 1);
		}
		else if (data != null)
		{
			id2rc.remove(id);
			Message msg = null;
			if(id2URL.containsKey(id))
				if(storeHeaderImage(id, data))
					msg = uiHandler.obtainMessage(MSG_IMAGE_READY, id);
			
			if((msg != null))
				uiHandler.sendMessage(msg);
		}
		else 
		{
			id2rc.remove(id);
		}
	}

	public void onFileDownloaded(Object id, InputStream data)
	{
		
	}

	public void onEvent(Object source, String event)
	{
		if(event.equals(HttpServiceIntf.CONNECTION_ERROR))
			uiHandler.sendMessage(uiHandler.obtainMessage(MSG_NETWORK_ERROR));
	}

	/**
	 * MUST be called at the end to clear resource
	 */
	public void clear()
	{
		ServicePool.getinstance().getMassDownloadService().removeRequestBySender(this);
		id2URL.clear();
		img2URL.clear();
		pendingUrls.clear();
		doingUrls.clear();
		imgCache.clear();
	}
	
	/**
	 * to stop send image download request. MUST be called when no more need to load image
	 */
	public void stopDownload()
	{
		stoped = true;
		ServicePool.getinstance().getEventCenter().removeListener(this, HttpServiceIntf.CONNECTION_ERROR);
	}
	
	/**
	 * to start send image download request
	 * 
	 * MUST call this then call to load image
	 */
	public void startDownload()
	{
		stoped = false;
		ServicePool.getinstance().getEventCenter().registerListener(this, HttpServiceIntf.CONNECTION_ERROR);
		requestPendingURLs();
	}
	
	/**
	 * call to retry network
	 */
	public void retryNetwork()
	{
		metNetworkError = false;
		requestPendingURLs();
	}

	public Resources getResource()
	{
		return resource;
	}

	public void setResource(Resources resource)
	{
		this.resource = resource;
	}
	
	class ReadImg extends Thread
	{
		protected String url;
		@Override
		public void run()
		{
			synchronized (this)
			{
				try
				{
					wait(30);
				}
				catch (InterruptedException e)
				{
				}
				uiHandler.sendMessage(uiHandler.obtainMessage(MSG_IMAGE_LOADED, url));
			}
		}
	}
}
