package com.zsxj.pda.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;
import com.zsxj.pdaonphone.R;

/**
 * to copy service config file to app file folder
 * @author ice
 *
 */
public class ServiceCfgFileCopy
{
	protected Log l = LogFactory.getLog(ServiceCfgFileCopy.class);
	
	public void copyFile(Context ctx) throws IOException
	{
		copyFile(ctx, R.raw.common, "common.xml");
		copyFile(ctx, R.raw.file_store, "file_store.xml");
		copyFile(ctx, R.raw.http_config, "http_config.xml");
	}
	
	/**
	 * copy given resource file to given named file
	 * @param ctx
	 * @param resId
	 * @param fileName
	 * @throws IOException 
	 */
	protected void copyFile(Context ctx, int resId, String fileName) throws IOException
	{
		File f = new File(Util.genFilePath(fileName, ctx));
		if(f.exists())
			f.delete();
		
		byte[] data = new byte[512];
		
		InputStream is = ctx.getResources().openRawResource(resId);
		FileOutputStream fos = new FileOutputStream(f);
		int len = 0;
		try
		{
			while((len = is.read(data)) != -1)
				fos.write(data, 0, len);
		}
		finally
		{
			Util.close(is);
			Util.close(fos);
		}
	}
}
