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

import fr.wseduc.smsproxy.providers.metrics.SmsMetricsRecorder;
import fr.wseduc.smsproxy.providers.metrics.SmsMetricsRecorderFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import fr.wseduc.sms.SmsSendingReport;
import fr.wseduc.webutils.StringValidation;
import static java.lang.System.currentTimeMillis;


public abstract class SmsProvider {
	private SmsMetricsRecorder smsMetricsRecorder;

	/**
	 * Logger object.
	 */
	protected Logger logger = LoggerFactory.getLogger(SmsProvider.class);

	/**
	 * Initialization method of the provider class.
	 * @param vertx : Vertx object
	 * @param conf : Specific provider configuration
	 */
	public void initProvider(Vertx vertx, JsonObject conf) {
		this.doInitProvider(vertx, conf);
		this.smsMetricsRecorder = SmsMetricsRecorderFactory.getInstance();
	}
	/**
	 * Initialization method of the provider class.
	 * @param vertx : Vertx object
	 * @param conf : Specific provider configuration
	 */
	protected abstract void doInitProvider(Vertx vertx, JsonObject conf);

	/**
	 * Sends a new text message.
	 * @param message : Message contents, implementation is provider dependent.
	 */
	public void sendSms(final Message<JsonObject> message) {
		final JsonObject parameters = message.body().getJsonObject("parameters");
		final JsonArray receivers = parameters.getJsonArray("receivers");
		final JsonArray receiversWithPrefix = new JsonArray();
		for (final Object receiver : receivers) {
			if (receiver instanceof String) {
				receiversWithPrefix.add(StringValidation.formatPhone((String) receiver));
			}
		}
		parameters.put("receivers", receiversWithPrefix);
		final long start = currentTimeMillis();
		// Log execution times
		doSendSms(message)
				.onSuccess(e -> smsMetricsRecorder.onSmsSent(currentTimeMillis() - start) )
				.onFailure(e -> smsMetricsRecorder.onSmsFailure(currentTimeMillis() - start));
	}

	/**
	 * Sends a new text message to a list of receivers (numbers have been prefixed with the default prefix).
	 * @param message : Message contents, implementation is provider dependent.
	 */
	protected abstract Future<Void> doSendSms(final Message<JsonObject> message);

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
