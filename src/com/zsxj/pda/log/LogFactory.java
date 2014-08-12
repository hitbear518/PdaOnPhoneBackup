package com.zsxj.pda.log;

import java.util.Hashtable;

public abstract class LogFactory
{
	protected static final Hashtable<String, Log> instances = new Hashtable<String, Log>();
	
	public synchronized static Log getLog(Class<?> cls)
	{
		String name = cls.getSimpleName();
		Log instance = (Log) instances.get(name);
        if (instance == null) {
        	instance = new LogImpl(name);
            instances.put(name, instance);
        }
        return instance;
	}

/*
	public static Log getLog(Class clazz)
	{
		return getFactory().getInstance(clazz);
	}

	public static LogFactory getFactory()
	{
		return new LogFactoryImpl();
	}

	public Log getInstance(Class clazz)
	{
		Log instance = getInstance(clazz.getClass().getSimpleName());
        return instance;
	}

	public Log getInstance(String name)
	{
		Log instance = (Log) instances.get(name);
        if (instance == null) {
            instance = newInstance(name);
            instances.put(name, instance);
        }
        return (instance);
	}
*/
	protected abstract Log newInstance(String name);
}
