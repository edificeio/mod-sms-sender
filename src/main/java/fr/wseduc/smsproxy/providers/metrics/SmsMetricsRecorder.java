package fr.wseduc.smsproxy.providers.metrics;

/**
 * Records metrics specific to OVH SMS-sending service.
 */
public interface SmsMetricsRecorder {

    /** Record the fact that an interaction with SMS service to send SMS has been done and has taken {@code duration}
     * milliseconds.
     * N.B. : Call this method even if the interaction has failed.
     * */
    void onSmsSent(final long duration);

    void onSmsFailure(final long duration);

    /**
     * Mock implementation used when no metrics options are defined.
     */
    class NoopSmsMetricsRecorder implements SmsMetricsRecorder {
        @Override
        public void onSmsSent(final long delay) {
            // Do nothing in this implementation
        }

        @Override
        public void onSmsFailure(final long duration) {
            // Do nothing in this implementation
        }
    }
}
