package com.zsxj.pda.service.event;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;

/**
 * To broadcast events to all listeners
 * @author ice
 *
 */

public class EventCenter 
{
	protected Log l = LogFactory.getLog(EventCenter.class);
	
	// key is event name, value is a list of listeners
	protected Map<String, List<EventListener>> listeners;
	{
		listeners = new HashMap<String, List<EventListener>>();
	}
	
	/**
	 * To fire event
	 * @param source
	 * @param event
	 */
	public void fireEvent(Object source, String event)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(source.toString()).append(" fires event: ").append(event);
		l.debug(sb.toString());
		synchronized(listeners)
		{
			List<EventListener> lst = listeners.get(event);
			if(lst == null)
				return;
			for(EventListener el : lst)
				if(el != source)
					el.onEvent(source, event);
		}
	}
	
	/**
	 * register listener to event
	 * @param el
	 * @param event
	 */
	public void registerListener(EventListener el, String event)
	{
		synchronized (listeners) 
		{
			List<EventListener> lst = listeners.get(event);
			if(lst == null)
			{
				lst = new LinkedList<EventListener>();
				listeners.put(event, lst);
			}
			lst.add(el);
		}
	}
	
	/**
	 * register listener to event
	 * @param el
	 * @param event
	 */
	public void registerListener(EventListener el, String[] events)
	{
		for(String ev : events)
			registerListener(el, ev);
	}
	
	/**
	 * remove listener from listening event
	 * @param el
	 * @param event
	 */
	public void removeListener(EventListener el, String event)
	{
		synchronized (listeners) 
		{
			List<EventListener> lst = listeners.get(event);
			if(lst == null)
				return;
			lst.remove(el);
			if(lst.size() == 0)
				listeners.remove(event);
		}
	}
	
	/**
	 * remove listener from listening event
	 * @param el
	 * @param events
	 */
	public void removeListener(EventListener el, String[] events)
	{
		for(String e : events)
			removeListener(el, e);
	}
	
	/**
	 * clear mapping
	 */
	public void clear()
	{
		listeners.clear();
	}
}
