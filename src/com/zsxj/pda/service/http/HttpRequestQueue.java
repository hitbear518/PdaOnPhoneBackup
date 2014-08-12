package com.zsxj.pda.service.http;

/**
 * Implements a HTTP request URL queue. FIFO
 * This is not THREAD SAFE. 
 */

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.zsxj.pda.util.UUIDGenerator;

class HttpRequest
{
	public String[] req;
	public String reqType;
	public String svrType;
	public InputStream is;
}

public class HttpRequestQueue
{
	// maps id to request
	protected Map<UUID, HttpRequest> uid2Req = new HashMap<UUID, HttpRequest>();
	
	// maps sender to requests
	protected Map<HttpRequesterIntf, List<HttpRequest>> requester2Reqs = new HashMap<HttpRequesterIntf, List<HttpRequest>>();
	
	// map sender to request IDs
	protected Map<HttpRequesterIntf, List<UUID>> requester2ReqIds = new HashMap<HttpRequesterIntf, List<UUID>>();
	
	// contains request UUID is sequence(FIFO)
	protected List<UUID> uidLst = new ArrayList<UUID>();
	
	protected UUIDGenerator idGen = new UUIDGenerator();
	
	/**
	 * clear all items
	 */
	public void clear()
	{
		uid2Req.clear();
		requester2ReqIds.clear();
		requester2Reqs.clear();
		uidLst.clear();
	}
	
	/**
	 * Add request with sender
	 * @param sender
	 * @param request
	 * @return
	 */
	public UUID addRequest(HttpRequesterIntf sender, HttpRequest request) {
		UUID uuid = idGen.generateVer3UUID();
		uid2Req.put(uuid, request);
		if(sender != null)
		{
			
			List<HttpRequest> reqs = requester2Reqs.get(sender);
			List<UUID> ids = requester2ReqIds.get(sender);
			if(reqs == null)
			{
				reqs = new ArrayList<HttpRequest>();
				requester2Reqs.put(sender, reqs);
				
				ids = new ArrayList<UUID>();
				requester2ReqIds.put(sender, ids);
			}
			reqs.add(request);
			ids.add(uuid);
		}

		uidLst.add(uuid);
		return uuid;
	}

	/**
	 * remove request with sender
	 * @param sender
	 */
	public void removeRequestBySender(HttpRequesterIntf sender) {
		List<HttpRequest> lst = requester2Reqs.remove(sender);
		if(lst == null)
			return;
		
		List<UUID> ids = requester2ReqIds.remove(sender);
		for(UUID id : ids)
			uid2Req.remove(id);
		uidLst.removeAll(ids);
	}

	/**
	 * remove given item from requester2ReqIds or requester2Reqs
	 * @return	'true' if found the item or 'false' if not
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected boolean removeItem(Map map, Object item) 
	{
		for(Object key : new HashSet(map.keySet()))
		{
			List<Object> lst = (List<Object>)map.get(key);
			if(lst.remove(item))
			{
				if(lst.size() == 0)
					map.remove(key);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * remove request with id
	 * @param id
	 */
	public void removeRequestByID(UUID uid) {
		HttpRequest req = uid2Req.remove(uid);
		if(req == null)
			return;
		
		uidLst.remove(uid);
		removeItem(requester2ReqIds, uid);
		removeItem(requester2Reqs, req);
	}

	/**
	 * get the current top request related UUID
	 * @return	UUID or 'null' if no request is pending
	 */
	public HttpRequest getRequest(UUID reqId) 
	{
		return uid2Req.get(reqId);
	}
	
	/**
	 * get requester by request ID
	 * @param reqId
	 * @return	Requester or 'null' if no proper requester found
	 */
	public HttpRequesterIntf getRequester(UUID reqId)
	{
		for(HttpRequesterIntf key : new HashSet<HttpRequesterIntf>(requester2ReqIds.keySet()))
		{
			List<UUID> lst = (List<UUID>)requester2ReqIds.get(key);
			if(lst.contains(reqId))
				return key;
		}
		
		return null;
	}
	
	/**
	 * get next UUID for the request
	 * @return
	 */
	public UUID getNextRequest()
	{
		if(uidLst.size() > 0)
			return uidLst.get(0);
		else
			return null;
	}
	
	/**
	 * get count of pending requests
	 * @return
	 */
	public int getPendingCount()
	{
		return uidLst.size();
	}
}
