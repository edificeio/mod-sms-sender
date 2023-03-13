package fr.wseduc.smsproxy.providers.metrics.impl;

import fr.wseduc.smsproxy.providers.metrics.SmsMetricsRecorder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.micrometer.backends.BackendRegistries;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MicrometerSmsMetricsRecorder implements SmsMetricsRecorder {
    private final Timer smsSendingTimes;

    public MicrometerSmsMetricsRecorder(final Configuration configuration) {
        final MeterRegistry registry = BackendRegistries.getDefaultNow();
        if(registry == null) {
            throw new IllegalStateException("micrometer.registries.empty");
        }
        final Timer.Builder smsSendingTimesBuilder = Timer.builder("sms.sending.time")
                .description("time to send SMS");
        if(configuration.sla.isEmpty()) {
            smsSendingTimesBuilder
                    .publishPercentileHistogram()
                    .maximumExpectedValue(Duration.ofSeconds(2L));
        } else {
            smsSendingTimesBuilder.sla(configuration.sla.toArray(new Duration[0]));
        }
        smsSendingTimes = smsSendingTimesBuilder.register(registry);
    }

    @Override
    public void onSmsSent(final long duration) {
        smsSendingTimes.record(duration, TimeUnit.MILLISECONDS);
    }

    public static class Configuration {
        private final List<Duration> sla;

        public Configuration(final List<Duration> sla) {
            this.sla = sla;
        }

        /**
         * Create the recorder's configuration from the attribute {@code metrics} of the parameter {@code conf} (or send
         * back a default configuration if conf is {@code null} or if it has no {@code metrics} field.
         * @param conf an object with the following keys :
         *          <ul>
         *               <li>sla, an ordered list of longs whose values are the bucket boundaries for the exported Timer which records the time
         *               spent waiting for a response from OVH</li>
         *           </ul>
         * @return The built configuration
         */
        public static Configuration fromJson(final JsonObject conf) {
            final List<Duration> sla;
            if(conf == null || !conf.containsKey("metrics")) {
                sla = Collections.emptyList();
            } else {
                final JsonObject metrics = conf.getJsonObject("metrics");
                sla = metrics.getJsonArray("sla", new JsonArray()).stream()
                    .mapToLong(long.class::cast)
                    .sorted()
                    .mapToObj(Duration::ofMillis)
                    .collect(Collectors.toList());
            }
            return new Configuration(sla);
        }
    }
}
