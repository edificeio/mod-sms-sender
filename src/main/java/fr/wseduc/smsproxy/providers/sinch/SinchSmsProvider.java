package fr.wseduc.smsproxy.providers.sinch;

import fr.wseduc.sms.SmsSendingReport;
import fr.wseduc.smsproxy.providers.SmsProvider;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.*;
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
     * Client reference, not mandatory to call Sinch api, but can be useful to transmit parametrized information about the caller
     */
    private String clientReference;
    @Override
    public void initProvider(Vertx vertx, JsonObject conf) {
        this.apiToken = conf.getString("apiToken", "");
        this.apiEndpoint = conf.getString("baseUrl", "") + "/" + conf.getString("servicePlanId", "") + "/batches";
        this.clientReference = conf.getString("clientReference", "");
        this.httpClient = vertx.createHttpClient();
    }

    @Override
    public void doSendSms(Message<JsonObject> message) {
        final JsonObject parameters = message.body().getJsonObject("parameters");
        logger.debug("[Sinch][sendSms] Called with parameters : " + parameters);

        final Handler<HttpClientResponse> resultHandler = response -> {
            if (response == null) {
                sendError(message, ErrorCodes.CALL_ERROR, null);
            } else {
                response.bodyHandler(body -> {
                    try {
                        final SinchSmsSendingReport sinchSmsSendingReport = Json.decodeValue(body, SinchSmsSendingReport.class);
                        replyOk(message, toSmsReport(sinchSmsSendingReport));
                        logger.debug("[Sinch][sendSms] body : " + body);
                    } catch (DecodeException e) {
                        logger.error("[Sinch][sendSms] Could not decode SinchSmsSendingReport : " + body.toString(), e);
                        sendError(message, ErrorCodes.CALL_ERROR, e);
                    }
                });
            }
        };

            HttpClientRequest request = httpClient.postAbs(apiEndpoint, resultHandler);

            request.putHeader("Content-Type", "application/json");
            // Sinch specific authentication field
            request.putHeader("Authorization", "Bearer " + apiToken);

            String body = new JsonObject()
                .put("to", parameters.getJsonArray("receivers"))
                    .put("body", parameters.getValue("message"))
                    .put("client_reference", clientReference)
                    .toString();
            Buffer bodyBuffer = Buffer.buffer();
            bodyBuffer.appendString(body, "UTF-8");
            request.putHeader("Content-Length", Integer.toString(bodyBuffer.length()));
            request.write(bodyBuffer);

            request.end();
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
                new String[]{});
    }

    @Override
    public void getInfo(Message<JsonObject> message) {
        logger.debug("[Sinch][getInfo]");

        final Handler<HttpClientResponse> resultHandler = response -> {
            if (response == null) {
                sendError(message, ErrorCodes.CALL_ERROR, null);
            } else {
                response.bodyHandler(body -> message.reply(new JsonObject(body.toString())));
            }
        };

        HttpClientRequest request = httpClient.getAbs(apiEndpoint, resultHandler);

        // Sinch specific authentication field
        request.putHeader("Authorization", "Bearer " + apiToken);

        request.end();

    }
}
