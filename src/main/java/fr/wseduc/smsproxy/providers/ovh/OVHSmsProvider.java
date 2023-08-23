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

package fr.wseduc.smsproxy.providers.ovh;

import fr.wseduc.smsproxy.providers.metrics.SmsMetricsRecorder;
import fr.wseduc.smsproxy.providers.metrics.SmsMetricsRecorderFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.smsproxy.providers.ovh.OVHHelper.OVHClient;
import fr.wseduc.smsproxy.providers.ovh.OVHHelper.OVH_ENDPOINT;
import fr.wseduc.smsproxy.providers.SmsProvider;
import fr.wseduc.sms.SmsSendingReport;
import static java.lang.System.currentTimeMillis;

import java.nio.charset.StandardCharsets;

public class OVHSmsProvider extends SmsProvider{

	private OVHClient ovhRestClient;
	private String AK, AS, CK, endPoint;
	private SmsMetricsRecorder smsMetricsRecorder;

	@Override
	public void initProvider(Vertx vertx, JsonObject config) {
		this.AK = config.getString("applicationKey", "");
		this.AS = config.getString("applicationSecret", "");
		this.CK = config.getString("consumerKey", "");
		this.endPoint = config.getString("ovhEndPoint", OVH_ENDPOINT.ovh_eu.getValue());

		ovhRestClient = new OVHClient(vertx, endPoint, AK, AS, CK);
		this.smsMetricsRecorder = SmsMetricsRecorderFactory.getInstance();
	}

	private void retrieveSmsService(final Message<JsonObject> message, final Handler<String> callBack){
		ovhRestClient.get("/sms/", new JsonObject(), response -> {
			logger.debug("[OVH][retrieveSmsService] /sms/ call returned : "+response);
			if(response == null){
				logger.error("[OVH][retrieveSmsService] /sms/ call response is null.");
				sendError(message, ErrorCodes.CALL_ERROR, null);
			} else {
				response.bodyHandler(body -> {
					if (response.statusCode() == 200) {
						logger.debug("[OVH][retrieveSmsService] Ok with body : " + body);
						JsonArray smsServices = new JsonArray(body.toString(StandardCharsets.UTF_8));
						callBack.handle(smsServices.getString(0));
					} else {
						logger.error("[OVH][retrieveSmsService] /sms/ reponse code [" + response.statusCode() + "] : " + body.toString(StandardCharsets.UTF_8));
						sendError(message, ErrorCodes.CALL_ERROR, null);
					}
				});
			}
		});
	}

	@Override
	public void doSendSms(final Message<JsonObject> message) {
		final JsonObject parameters = message.body().getJsonObject("parameters");
		logger.debug("[OVH][sendSms] Called with parameters : "+parameters);
		final long start = currentTimeMillis();
		final Handler<HttpClientResponse> resultHandler = response -> {
			final long duration = currentTimeMillis() - start;
			smsMetricsRecorder.onSmsSent(duration);
			if(response == null){
				sendError(message, ErrorCodes.CALL_ERROR, null);
			} else {
				response.bodyHandler(body -> {
					try {
						final OVHSmsSendingReport ovhSmsSendingReport = Json.decodeValue(body, OVHSmsSendingReport.class);
						logger.debug("[OVH][sendSms] " + ovhSmsSendingReport.getTotalCreditsRemoved() + " credits have been removed");
						if (ovhSmsSendingReport.getValidReceivers().length == 0) {
							sendError(message, ErrorCodes.INVALID_RECEIVERS_ALL, null, toSmsReport(ovhSmsSendingReport));
						} else if (ovhSmsSendingReport.getInvalidReceivers().length > 0) {
							sendError(message, ErrorCodes.INVALID_RECEIVERS_PARTIAL, null, toSmsReport(ovhSmsSendingReport));
						} else {
							replyOk(message, toSmsReport(ovhSmsSendingReport));
						}
					} catch (DecodeException e) {
						logger.error("[OVH][sendSms] Could not decode OVHSmsSendingReport : " + body.toString(), e);
						sendError(message, ErrorCodes.CALL_ERROR, e);
					}
				});
			}
		};

		Handler<String> serviceCallback = service -> {
			if(service == null){
				sendError(message, ErrorCodes.CALL_ERROR, null);
			} else {
				ovhRestClient.post("/sms/"+service+"/jobs/", parameters, resultHandler);
			}
		};

		retrieveSmsService(message, serviceCallback);
	}

	private SmsSendingReport toSmsReport(OVHSmsSendingReport ovhSmsSendingReport) {
		final String[] ids;
		final long[] ovhIds = ovhSmsSendingReport.getIds();
		if(ovhIds == null) {
			ids = null;
		} else {
			ids = new String[ovhIds.length];
			for (int i = 0; i < ovhIds.length; i++) {
				ids[i] = String.valueOf(ovhIds[i]);
			}
		}
		return new SmsSendingReport(
				ids,
				ovhSmsSendingReport.getInvalidReceivers(),
				ovhSmsSendingReport.getValidReceivers()
		);
	}

	@Override
	public void getInfo(final Message<JsonObject> message) {
		final JsonObject parameters = message.body().getJsonObject("parameters");
		logger.debug("[OVH][getInfo] Called with parameters : "+parameters);

		retrieveSmsService(message, new Handler<String>() {
			public void handle(String service) {
				if(service == null){
					sendError(message, ErrorCodes.CALL_ERROR, null);
				} else {
					ovhRestClient.get("/sms/"+service, parameters, new Handler<HttpClientResponse>() {
						public void handle(HttpClientResponse response) {
							if(response == null){
								sendError(message, ErrorCodes.CALL_ERROR, null);
								return;
							}
							response.bodyHandler(new Handler<Buffer>(){
								public void handle(Buffer body) {
									final JsonObject response = new JsonObject(body.toString());
									message.reply(response);
								}
							});
						}
					});
				}
			}
		});
	}

}
