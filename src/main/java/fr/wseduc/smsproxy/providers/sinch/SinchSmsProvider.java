package fr.wseduc.smsproxy.providers.sinch;

import fr.wseduc.sms.SmsSendingReport;
import fr.wseduc.smsproxy.providers.SmsProvider;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

/**
 * Implementation of Sms Provider using Sinch API
 */
public class SinchSmsProvider extends SmsProvider {

    /**
     * Http Client used to call Sinch endpoints
     */
    private HttpClient httpClient;
    /**
     * Api token for authentication
     */
    private String apiToken;
    /**
     * Sinch api endpoint for sms batch
     */
    private String apiEndpoint;
    /**
     * Identifier of the sender
     */
    private String senderId;
    /**
     * Client reference, not mandatory to call Sinch api, but can be useful to transmit parametrized information about the client
     */
    private String clientReference;

    @Override
    public void doInitProvider(Vertx vertx, JsonObject conf) {
        this.apiToken = conf.getString("apiToken", "");
        this.apiEndpoint = conf.getString("baseUrl", "") + "/" + conf.getString("servicePlanId", "") + "/batches";
        this.senderId = conf.getString("senderId", "");
        this.clientReference = conf.getString("clientReference", "");
        this.httpClient = vertx.createHttpClient();
    }

    @Override
    public Future<Void> doSendSms(Message<JsonObject> message) {
        final Promise<Void> promise = Promise.promise();
        final JsonObject parameters = message.body().getJsonObject("parameters");
        logger.debug("[Sinch][sendSms] Called with parameters : " + parameters);

        HeadersMultiMap headers = new HeadersMultiMap();
        headers.add("Content-Type", "application/json");
        // Sinch specific authentication field
        headers.add("Authorization", "Bearer " + apiToken);

        JsonObject body = new JsonObject()
                .put("to", parameters.getJsonArray("receivers"))
                .put("body", parameters.getValue("message"))
                .put("client_reference", clientReference);
        if (!senderId.isEmpty()) {
            body.put("from", senderId);
        }
        Buffer bodyBuffer = Buffer.buffer();
        bodyBuffer.appendString(body.toString(), "UTF-8");
        headers.add("Content-Length", Integer.toString(bodyBuffer.length()));

        httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.POST)
                        .setAbsoluteURI(apiEndpoint)
                        .setHeaders(headers))
                .flatMap(request -> request.send(bodyBuffer))
                .onSuccess(response -> {
                    if (response == null) {
                        sendError(message, ErrorCodes.CALL_ERROR, null);
	                    promise.fail(ErrorCodes.CALL_ERROR.getCode());
                    } else if (response.statusCode() != 201) {
                        sendError(message, ErrorCodes.CALL_ERROR, null);
                        response.bodyHandler(responseBody -> logger.error("[Sinch][sendSms] Error with status code : " + response.statusCode() + " when calling sinch API : " + responseBody.toString()));
	                    promise.fail(ErrorCodes.CALL_ERROR.getCode());
                    } else {
                        response.bodyHandler(responseBody -> {
                            try {
                                final SinchSmsSendingReport sinchSmsSendingReport = Json.decodeValue(responseBody, SinchSmsSendingReport.class);
                                replyOk(message, toSmsReport(sinchSmsSendingReport));
                                logger.debug("[Sinch][sendSms] body : " + responseBody);
	                            promise.complete();
                            } catch (DecodeException e) {
                                logger.error("[Sinch][sendSms] Could not decode SinchSmsSendingReport : " + responseBody.toString(), e);
                                sendError(message, ErrorCodes.CALL_ERROR, e);
	                            promise.fail(ErrorCodes.CALL_ERROR.getCode());
                            }
                        });
                    }
                });
		return promise.future();
    }

    /**
     * Method mapping specific Sinch sms sending report toward generic sms sending report
     * @param sinchSmsSendingReport the report received after sending sms through Sinch API
     * @return the generic sms sending report
     */
    private SmsSendingReport toSmsReport(SinchSmsSendingReport sinchSmsSendingReport) {
        return new SmsSendingReport(
                new String[]{sinchSmsSendingReport.getId()},
                new String[]{},
                sinchSmsSendingReport.getTo());
    }

    @Override
    public void getInfo(Message<JsonObject> message) {
        logger.debug("[Sinch][getInfo]");
        httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.POST)
                        .setAbsoluteURI(apiEndpoint)
                        .setHeaders(new HeadersMultiMap().add("Authorization", "Bearer " + apiToken)))
                .flatMap(HttpClientRequest::send)
                .onSuccess(response -> {
                    if (response == null) {
                        sendError(message, ErrorCodes.CALL_ERROR, null);
                    } else {
                        response.bodyHandler(body -> message.reply(new JsonObject(body.toString())));
                    }
                });
    }
}
