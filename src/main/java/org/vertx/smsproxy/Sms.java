package org.vertx.smsproxy;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public abstract class Sms extends BusModBase implements Handler<Message<JsonObject>> {
	
	protected abstract void sendSms(Message<JsonObject> message);
	
	@Override
	public void start(){
		super.start();
		vertx.eventBus().registerHandler(config.getString("sms-address", "entcore.sms"), this);
	}
	
	@Override
	public void handle(Message<JsonObject> message) {
		String action = message.body().getString("action", "");
		switch (action) {
			case("send-sms"):
				sendSms(message);
				break;
			default:
				sendError(message, "invalid.action");
		}
	}

}
