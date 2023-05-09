package fr.wseduc.smsproxy.providers.sinch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Report model based on Sinch API response after sending sms.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SinchSmsSendingReport {
    private final String id;

    private final String[] to;
    private final String clientReference;

    public SinchSmsSendingReport(@JsonProperty("id") String id,
                                 @JsonProperty("to") String[] to,
                                 @JsonProperty("client_reference") String clientReference) {
        this.id = id;
        this.to = to;
        this.clientReference = clientReference;
    }

    public String getId() {
        return id;
    }

    public String[] getTo() {
        return to;
    }

    public String getClientReference() {
        return clientReference;
    }
}
