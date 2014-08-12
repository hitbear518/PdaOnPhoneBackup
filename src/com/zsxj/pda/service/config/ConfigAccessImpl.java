package com.zsxj.pda.service.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.zsxj.pda.util.Util;

import android.content.Context;

public class ConfigAccessImpl implements ConfigAccess
{
	protected Map<String, String> cfgs = new HashMap<String, String>();
	
	public String getConfig(String name)
	{
		return cfgs.get(name);
	}
	
	protected Document openFile(InputStream is)throws Exception
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(is);
		
		return doc;
	}
	
	protected void readCfg(Document doc)
	{
		Element root = doc.getDocumentElement();
		NodeList nl = root.getChildNodes();
		for(int i=0; i<nl.getLength(); i++)
		{
			Node n = nl.item(i);
			int type = n.getNodeType();
			if((type != Node.COMMENT_NODE) && (type != Node.TEXT_NODE))
				cfgs.put(n.getNodeName(), n.getChildNodes().item(0).getNodeValue());
		}
	}
	
	/**
	 * read config file
	 * @param ctx
	 * @throws Exception
	 */
	public void init(Context ctx)throws Exception
	{
		InputStream is = null;
		try
		{
			is = new FileInputStream(Util.genFilePath("common.xml", ctx));
			Document doc = openFile(is);
			readCfg(doc);
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			Util.close(is);
		}
	}

	public void setConfig(String name, String value)
	{
	}
}
