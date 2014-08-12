package com.zsxj.pda.log;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogImpl implements Log
{
    private Logger logger;
    
    public LogImpl(String tag)
	{
		logger = LogManager.manager.createLogger(tag);
	}
    
    protected StringBuilder sb = new StringBuilder("jiayuan 2.0 ");
    protected String prifix(String str)
	{
		StringBuilder tmp = new StringBuilder(sb);
		tmp.append(str);
		
		return tmp.toString();
	}

	public void debug(Object message)
	{
		if (LogManager.manager.isLogging())
		{
			//@see android.util.Log.isLoggable(tag, level)
			//The default level of any tag is set to INFO.
			//but we output DEBUG message to logcat constrainedly.
			if(isDebugEnabled())
			{
				android.util.Log.d(logger.getName(), prifix(message.toString()));
		        log(Level.FINE, prifix(message.toString()));
			}
		}
	}

	public void debug(Object message, Throwable t)
	{
        if (LogManager.manager.isLogging())
        {
        	if(isDebugEnabled())
        	{
        		android.util.Log.d(logger.getName(), prifix(message.toString()), t);
        		log(Level.FINE, prifix(message.toString()), t);
        	}
        }		
	}

	public void error(Object message)
	{
        if (LogManager.manager.isLogging())
        {
        	if(isErrorEnabled())
        		log(Level.SEVERE, prifix(message.toString()));
        }
	}

	public void error(Object message, Throwable t)
	{
        if (LogManager.manager.isLogging())
        {
        	if(isErrorEnabled())
        		log(Level.SEVERE, prifix(message.toString()), t);            
        }       		
	}

	public void fatal(Object message)
	{
        if (LogManager.manager.isLogging())
        {
        	if(isFatalEnabled())
        		log(Level.SEVERE, prifix(message.toString()));            
        }		
	}

	public void fatal(Object message, Throwable t)
	{
        if (LogManager.manager.isLogging())
        {
        	if(isFatalEnabled())
        		log(Level.SEVERE, prifix(message.toString()), t);            
        }       		
	}

	public void info(Object message)
	{
        if (LogManager.manager.isLogging())
        {
        	if(isInfoEnabled())
        		log(Level.INFO, prifix(message.toString()));            
        }       		
	}

	public void info(Object message, Throwable t)
	{
        if (LogManager.manager.isLogging())
        {
        	if(isInfoEnabled())
        		log(Level.INFO, prifix(message.toString()), t);            
        }       		
	}

	public void trace(Object message)
	{
        if (LogManager.manager.isLogging())
        {
            log(Level.ALL, prifix(message.toString()));            
        }       		
	}

	public void trace(Object message, Throwable t)
	{
        if (LogManager.manager.isLogging())
        {
            log(Level.ALL, prifix(message.toString()), t);            
        }       		
	}

	public void warn(Object message)
	{
        if (LogManager.manager.isLogging())
        {
        	if(isWarnEnabled())
        		log(Level.WARNING, prifix(message.toString()));            
        }       		
	}

	public void warn(Object message, Throwable t)
	{
        if (LogManager.manager.isLogging())
        {
        	if(isWarnEnabled())
        		log(Level.WARNING, prifix(message.toString()), t);            
        }       		
	}

	public boolean isDebugEnabled()
	{
		return logger.isLoggable(Level.CONFIG);
	}

	public boolean isErrorEnabled()
	{
		return logger.isLoggable(Level.SEVERE);
	}

	public boolean isFatalEnabled()
	{
		return logger.isLoggable(Level.SEVERE);
	}

	public boolean isInfoEnabled()
	{
		return logger.isLoggable(Level.INFO);
	}

	public boolean isTraceEnabled()
	{
		return isDebugEnabled();
	}

	public boolean isWarnEnabled()
	{
		return logger.isLoggable(Level.WARNING);
	}
	
	private void log(Level level, String msg)
	{
        LogRecord record = new JiaYuanLogRecord(level, msg);
        record.setLoggerName(logger.getName());
        logger.log(record);
	}
	
	private void log(Level level, String msg, Throwable t)
	{
        LogRecord record = new JiaYuanLogRecord(level, msg);
        record.setLoggerName(logger.getName());
        record.setThrown(t);
        logger.log(record);
	}
	
	static class JiaYuanLogRecord extends LogRecord
	{
		private boolean sourceInited;
		
		public JiaYuanLogRecord(Level level, String msg) {
			super(level, msg);
		}
		
	    /**
	     * Gets the name of the class that is the source of this log record. This
	     * information can be changed, may be {@code null} and is untrusted.
	     * 
	     * @return the name of the source class of this log record (possiblity {@code null})
	     * @since Android 1.0
	     */
	    public String getSourceClassName() {
	        initSource();
	        return super.getSourceClassName();
	    }
		
	    /**
	     * Gets the name of the method that is the source of this log record.
	     * 
	     * @return the name of the source method of this log record.
	     * @since Android 1.0
	     */
	    public String getSourceMethodName() {
	        initSource();
	        return super.getSourceMethodName();
	    }
	    
	    /*
	     *  Init the sourceClass and sourceMethod fields.
	     */
	    private void initSource() {
	        if (!sourceInited) {
	            StackTraceElement[] elements = (new Throwable()).getStackTrace();
	            int i = 0;
	            String current = null;
	            FINDLOG: for (; i < elements.length; i++) {
	                current = elements[i].getClassName();
	                if (current.equals(LogImpl.class.getName())) {
	                    break FINDLOG;
	                }
	            }
	            while(++i<elements.length && elements[i].getClassName().equals(current)) {
	                // do nothing
	            }
	            if (i < elements.length) {
	            	super.setSourceClassName(elements[i].getClassName());
	            	super.setSourceMethodName(elements[i].getMethodName());
	            }
	        }
	    }

		private static final long serialVersionUID = 1L;
	}
}
