package com.zsxj.pda.service.file;

import java.io.Serializable;

public class DataRecord<T> implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2091276068308555819L;
	public T fileName;		// data to record. Must implements Serializable
	public Long time;
}
