package fr.wseduc.smsproxy.providers.ovh;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A structure describing all information about quota informations as described in OVH documentation.
 *
 * {@see https://api.ovh.com/console/#/sms/%7BserviceName%7D/users/%7Blogin%7D/jobs~POST}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OVHSmsSendingReport {
    private final long[] ids;
    private final String[] invalidReceivers;
    private final double totalCreditsRemoved;
    private final String[] validReceivers;

    @JsonCreator
    public OVHSmsSendingReport(@JsonProperty("ids") final long[] ids,
                               @JsonProperty("invalidReceivers") final String[] invalidReceivers,
                               @JsonProperty("totalCreditsRemoved") final double totalCreditsRemoved,
                               @JsonProperty("validReceivers") final String[] validReceivers) {
        this.ids = (ids != null) ? ids : new long[] {};
        this.invalidReceivers = (invalidReceivers != null) ? invalidReceivers : new String[] {};
        this.totalCreditsRemoved = totalCreditsRemoved;
        this.validReceivers = (validReceivers != null) ? validReceivers : new String[] {};
    }

    public long[] getIds() {
        return ids;
    }

    public String[] getInvalidReceivers() {
        return invalidReceivers;
    }

    public double getTotalCreditsRemoved() {
        return totalCreditsRemoved;
    }

    public String[] getValidReceivers() {
        return validReceivers;
    }
}
