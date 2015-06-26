package fr.wseduc.smsproxy.providers.ovh;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.smsproxy.providers.ovh.OVHHelper.OVHClient;
import fr.wseduc.smsproxy.providers.ovh.OVHHelper.OVH_ENDPOINT;
import fr.wseduc.smsproxy.providers.SmsProvider;

public class OVHSmsProvider extends SmsProvider{

	private OVHClient ovhRestClient;
	private String AK, AS, CK, endPoint;

	@Override
	public void initProvider(Vertx vertx, JsonObject config) {
		this.AK = config.getString("applicationKey", "");
		this.AS = config.getString("applicationSecret", "");
		this.CK = config.getString("consumerKey", "");
		this.endPoint = config.getString("ovhEndPoint", OVH_ENDPOINT.ovh_eu.getValue());

		ovhRestClient = new OVHClient(vertx, endPoint, AK, AS, CK);
	}

	private void retrieveSmsService(final Message<JsonObject> message, final Handler<String> callBack){
		ovhRestClient.get("/sms/", new JsonObject(), new Handler<HttpClientResponse>() {
			public void handle(final HttpClientResponse response) {
				logger.debug("[OVH][retrieveSmsService] /sms/ call returned : "+response);
				if(response == null){
					logger.error("[OVH][retrieveSmsService] /sms/ call response is null.");
					sendError(message, "ovh.apicall.error", null);
					return;
				}
				response.bodyHandler(new Handler<Buffer>() {
					public void handle(Buffer body) {
						if(response.statusCode() == 200){
							logger.debug("[OVH][retrieveSmsService] Ok with body : "+body);
							JsonArray smsServices = new JsonArray(body.toString("UTF-8"));
							callBack.handle(smsServices.get(0).toString());
						} else {
							logger.error("[OVH][retrieveSmsService] /sms/ reponse code ["+response.statusCode()+"] : "+body.toString("UTF-8"));
							sendError(message, body.toString("UTF-8"), null);
						}
					}
				});
			}
		});
	}

	@Override
	public void sendSms(final Message<JsonObject> message) {
		final JsonObject parameters = message.body().getObject("parameters");
		logger.debug("[OVH][sendSms] Called with parameters : "+parameters);

		final Handler<HttpClientResponse> resultHandler = new Handler<HttpClientResponse>() {
			public void handle(HttpClientResponse response) {
				if(response == null){
					sendError(message, "ovh.apicall.error", null);
				} else {
					response.bodyHandler(new Handler<Buffer>(){
						public void handle(Buffer body) {
							final JsonObject response = new JsonObject(body.toString());
							final JsonArray invalidReceivers = response.getArray("invalidReceivers", new JsonArray());
							final JsonArray validReceivers = response.getArray("validReceivers", new JsonArray());

							if(validReceivers.size() == 0){
								sendError(message, "invalid.receivers.all", null, new JsonObject(body.toString()));
							} else if(invalidReceivers.size() > 0){
								sendError(message, "invalid.receivers.partial", null, new JsonObject(body.toString()));
							} else {
								message.reply(new JsonObject(body.toString()));
							}
						}
					});
				}
			}
		};

		Handler<String> serviceCallback = new Handler<String>() {
			public void handle(String service) {
				if(service == null){
					sendError(message, "ovh.apicall.error", null);
				} else {
					ovhRestClient.post("/sms/"+service+"/jobs/", parameters, resultHandler);
				}
			}
		};

		retrieveSmsService(message, serviceCallback);
	}

}
