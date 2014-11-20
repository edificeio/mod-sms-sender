package fr.wseduc.smsproxy;

import java.util.ServiceLoader;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.smsproxy.providers.SmsProvider;

public class Sms extends BusModBase implements Handler<Message<JsonObject>> {

	private ServiceLoader<SmsProvider> implementations;
	private JsonObject providersList = new JsonObject();

	/**
	 * Verticle start method.
	 */
	@Override
	public void start(){
		super.start();
		vertx.eventBus().registerHandler(config.getString("sms-address", "entcore.sms"), this);
		implementations = ServiceLoader.load(SmsProvider.class);
		providersList = container.config().getObject("providers");

		if(providersList == null){
			logger.error("providers.list.empty");
			return;
		}

		for(String field : providersList.getFieldNames()){
			for(SmsProvider provider : implementations){
				if(provider.getClass().getSimpleName().equals(field + "SmsProvider")){
					provider.initProvider(vertx, providersList.getObject(field));
					logger.info("[Sms] "+provider.getClass().getName()+" registered.");
				}
			}
		}
	}

	//Return the instantiated provider given a provider name prefix
	private SmsProvider getService(String serviceName){
		for(SmsProvider provider : implementations){
			if(provider.getClass().getSimpleName().equals(serviceName + "SmsProvider"))
				return provider;
		}
		return null;
	}

	@Override
	public void handle(Message<JsonObject> message) {
		String action = message.body().getString("action", "");
		String providerName = message.body().getString("provider", "");

		SmsProvider provider = getService(providerName);
		if(provider == null){
			sendError(message, "invalid.provider");
			return;
		}

		switch (action) {
			case("send-sms"):
				provider.sendSms(message);
				break;
			default:
				sendError(message, "invalid.action");
		}
	}

}
