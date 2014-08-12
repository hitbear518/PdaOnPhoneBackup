package com.zsxj.pda.service.http;

/**
 * To read HTTP configuration
 */

import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.zsxj.pda.util.Util;

import android.content.Context;

public class HttpCfgReader
{
	public String server;
	public int serverPort;
	public String serverBasicPath;
	
	public String locationBasedServer;
	public int locationBasedServerPort;
	public String locationBasedServerPath;

	public String imageServer;
	public int imageServerPort;
	public String imageServerPath;

	protected void readServer(Element node)
	{
		NodeList nl = node.getElementsByTagName("address");
		Node n = nl.item(0);
		server = n.getChildNodes().item(0).getNodeValue();
		
		nl = node.getElementsByTagName("port");
		n = nl.item(0);
		serverPort = Integer.valueOf(n.getChildNodes().item(0).getNodeValue());
		
		nl = node.getElementsByTagName("basicPath");
		n = nl.item(0);
		if(n.getChildNodes().getLength() > 0)
			serverBasicPath = n.getChildNodes().item(0).getNodeValue();
	}
	
	protected void readLocationBasedServer(Element node)
	{
		NodeList nl = node.getElementsByTagName("address");
		Node n = nl.item(0);
		locationBasedServer = n.getChildNodes().item(0).getNodeValue();
		
		nl = node.getElementsByTagName("port");
		n = nl.item(0);
		locationBasedServerPort = Integer.valueOf(n.getChildNodes().item(0).getNodeValue());
		
		nl = node.getElementsByTagName("basicPath");
		n = nl.item(0);
		if(n.getChildNodes().getLength() > 0)
			locationBasedServerPath = n.getChildNodes().item(0).getNodeValue();
	}
	
	protected void readImageServer(Element node)
	{
		NodeList nl = node.getElementsByTagName("address");
		Node n = nl.item(0);
		imageServer = n.getChildNodes().item(0).getNodeValue();
		
		nl = node.getElementsByTagName("port");
		n = nl.item(0);
		imageServerPort = Integer.valueOf(n.getChildNodes().item(0).getNodeValue());
		
		nl = node.getElementsByTagName("basicPath");
		n = nl.item(0);
		if(n.getChildNodes().getLength() > 0)
			imageServerPath = n.getChildNodes().item(0).getNodeValue();
	}

	/**
	 * read config file
	 * @param ctx
	 * @throws Exception
	 */
	public void read(Context ctx, String fileName)throws Exception
	{
		InputStream is = null;
		
		try
		{
			is = new FileInputStream(Util.genFilePath(fileName, ctx));
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(is);
			Element root = doc.getDocumentElement();
			NodeList nl = root.getElementsByTagName("server");
			readServer((Element)nl.item(0));
			nl = root.getElementsByTagName("locationBasedServer");
			readLocationBasedServer((Element)nl.item(0));
			nl = root.getElementsByTagName("imageServer");
			readImageServer((Element)nl.item(0));
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			if(is != null)
				try
				{
					is.close();
				}
				catch (Exception e)
				{
				}
		}

		
	}
}
