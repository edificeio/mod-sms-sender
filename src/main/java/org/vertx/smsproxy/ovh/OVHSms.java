package org.vertx.smsproxy.ovh;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.smsproxy.Sms;
import org.vertx.smsproxy.ovh.OVHHelper.OVHClient;
import org.vertx.smsproxy.ovh.OVHHelper.OVH_ENDPOINT;

public class OVHSms extends Sms{
	
	private OVHClient ovhRestClient;
	private String AK, AS, CK, endPoint;
	
	@Override
	public void start() {
		super.start();
		this.AK = config.getString("applicationKey", "");
		this.AS = config.getString("applicationSecret", "");
		this.CK = config.getString("consumerKey", "");
		this.endPoint = container.config().getString("ovhEndPoint", OVH_ENDPOINT.ovh_eu.getValue());
		
		ovhRestClient = new OVHClient(vertx, endPoint, AK, AS, CK);
	}
	
	private void retrieveSmsService(final Message<JsonObject> message, final Handler<String> callBack){
		ovhRestClient.get("/sms/", new JsonObject(), new Handler<HttpClientResponse>() {
			public void handle(final HttpClientResponse response) {
				logger.debug("[retrieveSmsService] /sms/ call returned : "+response);
				if(response == null){
					logger.error("[retrieveSmsService] /sms/ call response is null.");
					sendError(message, "ovh.apicall.error");
					return;
				}
				response.bodyHandler(new Handler<Buffer>() {
					public void handle(Buffer body) {
						if(response.statusCode() == 200){
							logger.debug("[retrieveSmsService] Ok with body : "+body);
							JsonArray smsServices = new JsonArray(body.toString("UTF-8"));
							callBack.handle(smsServices.get(0).toString());
						} else {
							logger.error("[retrieveSmsService] /sms/ reponse code ["+response.statusCode()+"] : "+body.toString("UTF-8"));
							sendError(message, body.toString("UTF-8"));
						}
					}
				});
			}
		});
	}

	
	@Override
	protected void sendSms(final Message<JsonObject> message) {
		final JsonObject parameters = message.body().getObject("parameters");
		logger.debug("[sendSms] Called with parameters : "+parameters);
		
		final Handler<HttpClientResponse> resultHandler = new Handler<HttpClientResponse>() {
			public void handle(HttpClientResponse response) {
				if(response == null){
					sendError(message, "ovh.apicall.error");
				} else {
					response.bodyHandler(new Handler<Buffer>(){
						public void handle(Buffer body) {
							message.reply(new JsonObject(body.toString()));
						}
					});
				}
			}
		};
		
		Handler<String> serviceCallback = new Handler<String>() {
			public void handle(String service) {
				if(service == null){
					sendError(message, "ovh.apicall.error");
				} else {
					ovhRestClient.post("/sms/"+service+"/jobs/", parameters, resultHandler);
				}
			}
		};
		
		retrieveSmsService(message, serviceCallback);
	}

}
