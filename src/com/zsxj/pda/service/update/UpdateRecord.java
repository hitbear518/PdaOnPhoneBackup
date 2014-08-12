package com.zsxj.pda.service.update;

import java.io.Serializable;
import java.net.URL;

import com.zsxj.pda.util.Util;

/**
 * to record current update data 
 */
public class UpdateRecord implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3116270471753778060L;
	
	public String ver;			// marks ver of the apk which is downloading
	public long fileLength;		// marks expected file length
	public URL url;				// URL of the file to upgrade to
	
	/**
	 * get file name of the given URL
	 * @return
	 */
	public String getFileName() {
		
		return Util.lastSectionInURL(url);
	}
	
	/**
	 * to check if current record is nerer then the given record
	 * @param ur
	 * @return
	 */
	public boolean isNewerThen(UpdateRecord ur) {
		
		return (isNewerVersion(ur.ver, ver));
	}
	
	/**
	 * to check if the given ver2 is newer then ver1
	 * @param ver
	 * @return
	 */
	protected boolean isNewerVersion(String ver1, String ver2) {
		
//		float curVer = Float.valueOf(ver1);
//		float newVer = Float.valueOf(ver2);
//		
//		return newVer > curVer;
		
		return (ver1.compareToIgnoreCase(ver2) < 0);
	}
	
	/**
	 * convert given string to 
	 * @return
	 */
	protected int[] convertVer(String ver) {
		
		String vals[] = ver.split(".");
		int vers[] = new int[vals.length];
		for(int i=0; i<vals.length; i++)
			vers[i] = Integer.valueOf(vals[i]);
		
		return vers;
	}
}
