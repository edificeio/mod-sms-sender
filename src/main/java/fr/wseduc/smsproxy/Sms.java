/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
		vertx.eventBus().registerHandler(config.getString("address", "entcore.sms"), this);
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
			case("get-info"):
				provider.getInfo(message);
				break;
			default:
				sendError(message, "invalid.action");
		}
	}

}
