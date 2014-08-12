package com.zsxj.pda.util;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * implements a soft reference cache map.
 * The value is wrapped with SoftReference.
 * This is NOT A THREAD SAFE implementation.
 * @author ice
 *
 */
public class SoftReferenceMap<V, T>
{
	protected Map<V, SoftReference<T>> cache = new HashMap<V, SoftReference<T>>();
	
	/**
	 * to put a key & value pare into store.
	 * 
	 * @param key	If a same key exists then then old one will be replaced
	 * @param value		If a same old value exists then the new key will map to old value
	 */
	public void put(V key, T value)
	{
		Set<V> invalidKeys = new HashSet<V>();	// hold keys to be removed
		for(V k : cache.keySet())
		{
			SoftReference<T> srf = cache.get(k);
			T tmp = srf.get();
			if(tmp == null)
				invalidKeys.add(k);
			else
			{
				if(tmp == value)
				{
					invalidKeys.add(k);
					break;
				}
			}
		}
		
		removeKeys(invalidKeys);
		
		if(key != null)
		{
			SoftReference<T> srf = new SoftReference<T>(value);
			cache.put(key, srf);
		}
	}
	
	/**
	 * get key related value
	 * @param key
	 * @return	related value or 'null' if it does not exist
	 */
	public T get(Object key)
	{
		T val = null;
		SoftReference<T> srf = cache.get(key);
		if(srf != null)
			val = srf.get();
		
		if((val == null) && (srf != null))
			cache.remove(key);
		return val;
	}
	
	/**
	 * to remove all given keys & mapped values
	 * @param keys
	 */
	public void removeKeys(Set<V> keys)
	{
		for(Object k : keys)
			cache.remove(k);
	}
	
	/**
	 * remove specified key
	 * @param key
	 */
	public void removeKey(V key)
	{
		cache.remove(key);
	}
	
	/**
	 * check if this map contains the key
	 * @param key
	 */
	public boolean containsKey(V key)
	{
		return cache.containsKey(key);
	}
	
	/**
	 * to clear all cache
	 */
	public void clear()
	{
		cache.clear();
	}
}
