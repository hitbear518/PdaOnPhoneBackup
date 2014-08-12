package com.zsxj.pda.service.event;


/**
 * Event listener interface. Subclass implements this to register to event center.
 * 
 * Listeners will be invoked one after another.
 * @author ice
 *
 */

public interface EventListener {
	
	
	/**
	 * call on event received.
	 * 
	 * Even though listing to the event, you will not get then event send by yourself.
	 * 
	 * @param source	The source who sends out the event.
	 * @param event		Event name.
	 */
	public void onEvent(Object source, String event);
}
