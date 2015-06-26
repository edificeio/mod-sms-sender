package fr.wseduc.smsproxy.providers;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;


public abstract class SmsProvider {

	/**
	 * Logger object.
	 */
	protected Logger logger = LoggerFactory.getLogger(SmsProvider.class);

	/**
	 * Initialization method of the provider class.
	 * @param vertx : Vertx object
	 * @param conf : Specific provider configuration
	 */
	public abstract void initProvider(Vertx vertx, JsonObject conf);

	/**
	 * Sends a new text message.
	 * @param message : Message contents, implementation is provider dependent.
	 */
	public abstract void sendSms(final Message<JsonObject> message);

	/**
	 * Error management method, sends back a message containing the error details on the bus.
	 * @param message : Original message
	 * @param error : Error message
	 * @param e : Exception thrown
	 * @param data : Additional data
	 */
	protected void sendError(Message<JsonObject> message, String error, Exception e, JsonObject data){
		logger.error(error, e);
	    JsonObject json = new JsonObject().putString("status", "error")
	    		.putString("message", error)
	    		.putObject("data", data);
	    message.reply(json);
	}

	/**
	 * Error management method, sends back a message containing the error details on the bus.
	 * @param message : Original message
	 * @param error : Error message
	 * @param e : Exception thrown
	 */
	protected void sendError(Message<JsonObject> message, String error, Exception e){
		sendError(message, error, e, null);
	}

	/**
	 * Error management method, sends back a message containing the errer details on the bus.
	 * @param message : Original message
	 * @param error : Error message
	 */
	protected void sendError(Message<JsonObject> message, String error) {
	    sendError(message, error, null);
	  }

}
