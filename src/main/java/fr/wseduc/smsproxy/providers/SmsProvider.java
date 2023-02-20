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

package fr.wseduc.smsproxy.providers;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import fr.wseduc.sms.SmsSendingReport;


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
	 * Retrieves the account information.
	 * @param message : Message contents, implementation is provider dependent.
	 */
	public abstract void getInfo(final Message<JsonObject> message);

	/**
	 * Error management method, sends back a message containing the error details on the bus.
	 * @param message : Original message
	 * @param error : Error message
	 * @param e : Exception thrown
	 * @param data : Additional data from the provider
	 */
	protected void sendError(Message<JsonObject> message, ErrorCodes error, Exception e, SmsSendingReport data){
		logger.error(error + " -> " + data, e);
	    final JsonObject json = new JsonObject().put("status", "error")
	    		.put("message", error.getCode())
	    		.put("data", JsonObject.mapFrom(data));
	    message.reply(json);
	}

	/**
	 * Error management method, sends back a message containing the error details on the bus.
	 * @param message : Original message
	 * @param error : Error message
	 * @param e : Exception thrown
	 */
	protected void sendError(Message<JsonObject> message, ErrorCodes error, Exception e){
		sendError(message, error, e, null);
	}

	/**
	 * Error management method, sends back a message containing the error details on the bus.
	 * @param message : Original message
	 * @param report : Additional data from the provider
	 */
	protected void replyOk(Message<JsonObject> message, final SmsSendingReport report){
		final JsonObject json = new JsonObject().put("status", "ok")
				.put("data", JsonObject.mapFrom(report));
		message.reply(json);
	}

	public enum ErrorCodes {
		CALL_ERROR("provider.apicall.error"),
		INVALID_RECEIVERS_ALL("invalid.receivers.all"),
		INVALID_RECEIVERS_PARTIAL("invalid.receivers.partial");

		private final String code;

		ErrorCodes(final String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}

}
