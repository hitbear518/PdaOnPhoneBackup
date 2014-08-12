package com.zsxj.pda.service.file;

import java.io.Serializable;
import java.util.List;

/**
 * file serialiable info, contains : fileList,cacheSizeRecord
 * @author eyeshot
 *
 */
public class FileSerialInfo implements Serializable{
	
	private static final long serialVersionUID = -653618247875550322L;
	public List<DataRecord<String>> fileList ; //file list info
	public long cachedSize; // cachesize unit:byte
}
