package com.zsxj.pda.service.http;

/**
 * this is to implements mass download files.
 * 
 * this will not send out network error message but to listen.
 * when got this message all requests include running will be 
 * canceled and not to notify caller.
 * 
 * @author ice
 */

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;
import com.zsxj.pda.service.ServicePool;
import com.zsxj.pda.service.event.EventListener;
import com.zsxj.pda.service.network.NetStatusWatcher;

import android.content.Context;

public class HTTPMassDownload implements HttpServiceIntf, EventListener
{
	protected Log l = LogFactory.getLog(HTTPMassDownload.class);
	protected int threads = 2;	// define max threads for downloading
	protected List<HttpServiceImpl> lst = new ArrayList<HttpServiceImpl>();
	protected Context ctx;
	
	public void init(Context ctx)throws Exception
	{
		this.ctx = ctx;
		checkConnThread();
		ServicePool.getinstance().getEventCenter().registerListener(this, NetStatusWatcher.NETWORK_CONNECTION_ON);
	}
	
	/**
	 * generate thread
	 * @param ctx
	 * @return
	 * @throws Exception 
	 */
	protected HttpServiceImpl genThread(Context ctx) throws Exception
	{
		HttpServiceImpl hsi = new HttpServiceImpl();
		hsi.init(ctx);
		hsi.start();
		ServicePool.getinstance().getEventCenter().registerListener(hsi, CONNECTION_ERROR);
		
		return hsi;
	}
	
	/**
	 * get a service with less pending work
	 * @param lst
	 * @return
	 */
	protected HttpServiceImpl getLessPendingService(List<HttpServiceImpl> lst)
	{
		HttpServiceImpl hsi = null;
		for(HttpServiceImpl tmp : lst)
		{
			if(hsi == null)
				hsi = tmp;
			else
				if(hsi.getPendingReqCount() > tmp.getPendingReqCount())
					hsi = tmp;
		}
		return hsi;
	}
	
	/**
	 * check to resize thread
	 * @param ctx
	 * @param to
	 * @throws Exception 
	 */
	protected void checkResizeThread(Context ctx, int to) throws Exception
	{
		if(lst.size() > to)
			trimThread(to);
		else if(lst.size() < to)
			increaseThread(ctx, to);
	}
	
	/**
	 * increase thread to given number
	 * @param ctx
	 * @param toSize
	 * @throws Exception 
	 */
	protected void increaseThread(Context ctx, int toSize) throws Exception
	{
		while(lst.size() < toSize)
			lst.add(genThread(ctx));
	}
	
	/**
	 * trim thread size to given number
	 * @param toSize
	 */
	protected void trimThread(int toSize)
	{
		List<HttpServiceImpl> tmp = new LinkedList<HttpServiceImpl>();
		for(HttpServiceImpl hsi : lst)
		{
			if(hsi.getPendingReqCount() == 0)
				tmp.add(hsi);
		}
		
		while(lst.size() > toSize)
		{
			HttpServiceImpl hsi = tmp.remove(0);
			lst.remove(hsi);
			hsi.stop();
		}
	}
	
	public Object addRequest(HttpRequesterIntf sender, String[] request,
			String requestType, String serverType)
	{
		try
		{
			checkResizeThread(ctx, threads);
		}
		catch (Exception e)
		{
			l.error("error on checkResizeThread()", e);
		}
		return getLessPendingService(lst).addRequest(sender, request, requestType, serverType);
	}

	public Object addPost(HttpRequesterIntf sender, String[] request,
			InputStream is,String requestType, String serverType)
	{
		try
		{
			checkResizeThread(ctx, threads);
		}
		catch (Exception e)
		{
			l.error("error on checkResizeThread()", e);
		}
		return getLessPendingService(lst).addPost(sender, request, is,requestType, serverType);
	}

	public void removeRequestBySender(HttpRequesterIntf sender)
	{
		for(HttpServiceImpl hsi : lst)
			hsi.removeRequestBySender(sender);
	}

	public void removeRequestByID(Object id)
	{
		for(HttpServiceImpl hsi : lst)
			hsi.removeRequestByID(id);
	}

	public byte[] login(String username, String password) throws Exception
	{
		throw new RuntimeException("not implemented");
	}

	public void logout()
	{
		throw new RuntimeException("not implemented");
	}

	public boolean hasRequestInPending(Object id)
	{
		for(HttpServiceImpl hsi : lst)
			if(hsi.hasRequestInPending(id))
				return true;
		
		return false;
	}

	public int getLastLoginErrorCode()
	{
		throw new RuntimeException("not implemented");
	}

	public void start()
	{
		for(HttpServiceImpl hsi : lst)
			hsi.start();
	}

	public void stop()
	{
		for(HttpServiceImpl hsi : lst)
			hsi.stop();
	}

	public String getToken()
	{
		throw new RuntimeException("not implemented");
	}

	public String getClientId()
	{
		throw new RuntimeException("not implemented");
	}

	public int getConnectionCount()
	{
		return lst.size();
	}

	/**
	 * to check connection status to make a conn thread
	 */
	protected void checkConnThread()
	{
		NetStatusWatcher nsw = ServicePool.getinstance().getNetStatusWatcher();
		if(nsw.getConnecitonSpeed() == NetStatusWatcher.CONNECTION_SPEED_HEIGHT)
			threads = 5;
		else if(nsw.getConnecitonSpeed() == NetStatusWatcher.CONNECTION_SPEED_LOW)
			threads = 2;
		
		try
		{
			checkResizeThread(ctx, threads);
		}
		catch (Exception e)
		{
			l.error("error on checkResizeThread()", e);
		}
	}
	
	public void onEvent(Object source, String event)
	{
		if(event == NetStatusWatcher.NETWORK_CONNECTION_ON)
			checkConnThread();
	}
}
