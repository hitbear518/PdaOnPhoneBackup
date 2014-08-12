package com.zsxj.pda.service.file;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;

import com.zsxj.pda.log.Log;
import com.zsxj.pda.log.LogFactory;
import com.zsxj.pda.util.Util;
/**
 * split xml file
 * @author eyeshot
 *
 */
public class XmlReader extends DefaultHandler{
	static FileConfig fileConfig;
	protected String subTarName = "";
    protected String tarName = "";
	protected String  storePeriod = "storePeriod";
	protected String storePlace = "storePlace";
	protected String storePath = "storePath";
	protected Log l = LogFactory.getLog(XmlReader.class);
	public XmlReader () {
		
	}
	    @Override
	    public void startElement(String uri, String localName, String qName,
	            Attributes atts) throws SAXException {
	        if ("fileStore".equals(localName)) {
	        	fileConfig = new FileConfig();
	        	tarName = localName;
	        }
	        
	        if(storePeriod.equals(localName) || storePlace.equals(localName) || storePath.equals(localName)){
	            subTarName = localName;
	        }
	    }
	    @Override
	    public void characters(char[] ch, int start, int length)
	            throws SAXException {
	        if(tarName != "" && tarName.length() > 0 && tarName.equals("fileStore")){
	            String text = new String(ch, start, length);
	            if(storePeriod.equals(subTarName)){
	            	fileConfig.setStorePeriod(text);
	            } else if(storePlace.equals(subTarName)){
	            	fileConfig.setStorePlace(text);
	            } else if(storePath.equals(subTarName)){
	            	fileConfig.setStorePath(text);
	            } 
	        }
	        subTarName = "";
	    }
	    
	    public FileConfig splitXml(Context mContext){
	    	InputStream is = null;
	    	try{
	    		   SAXParserFactory factory = SAXParserFactory.newInstance();
		           SAXParser parser = factory.newSAXParser();
		           XMLReader xr = parser.getXMLReader();
		           xr.setContentHandler(this);    
	               is = new FileInputStream(Util.genFilePath("file_store.xml", mContext));
	               xr.parse(new InputSource(is));
	    	} catch (Exception e){
	    		l.error("error on XmlReader.splitXml : ", e);
	    	} finally{
	    		if(is!=null){
	    			try {
						is.close();
					} catch (IOException e) {
						l.error("split file_store error ", e);
					}
	    		}
	    	}
            return fileConfig;
	    }
	  public FileConfig getFileConfig(Context mContext){
		  splitXml(mContext);
		  return fileConfig;
	  }  
	    
}
