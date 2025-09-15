package fr.wseduc.smsproxy.providers.metrics;

import fr.wseduc.smsproxy.providers.metrics.impl.MicrometerSmsMetricsRecorder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;

public class SmsMetricsRecorderFactory {
    private static JsonObject config;
    private static MetricsOptions metricsOptions;
    private static SmsMetricsRecorder smsMetricsRecorder;
    private SmsMetricsRecorderFactory(){}
    public static Future<Void> init(final Vertx vertx, final JsonObject config) {
        SmsMetricsRecorderFactory.config = config;
        final String metricsOptionsName = "metricsOptions";
        final Future<Void> future;
        if(config.getJsonObject(metricsOptionsName) == null) {
          final Promise<Void> promise = Promise.promise();
            vertx.sharedData().<String, String>getLocalAsyncMap("server")
              .compose(map -> map.get(metricsOptionsName))
              .onSuccess(metricsOptions -> {
                if (metricsOptions == null) {
                  SmsMetricsRecorderFactory.metricsOptions = new MetricsOptions().setEnabled(false);
                } else {
                  SmsMetricsRecorderFactory.metricsOptions = new MetricsOptions(new JsonObject(metricsOptions));
                }
                promise.complete();
              })
              .onFailure(promise::fail);
            future = promise.future();
        } else {
            metricsOptions = new MetricsOptions(config.getJsonObject(metricsOptionsName));
            future = Future.succeededFuture();
        }
        return future;
    }

    /**
     * @return The backend to record metrics. If metricsOptions is defined in the configuration then the backend used
     * is MicroMeter. Otherwise a dummy registrar is returned and it collects nothing.
     */
    public static SmsMetricsRecorder getInstance() {
        if(smsMetricsRecorder == null) {
            if(metricsOptions == null) {
                throw new IllegalStateException("sms.metricsrecorder.factory.not.set");
            }
            if(metricsOptions.isEnabled()) {
                smsMetricsRecorder = new MicrometerSmsMetricsRecorder(MicrometerSmsMetricsRecorder.Configuration.fromJson(config));
            } else {
                smsMetricsRecorder = new SmsMetricsRecorder.NoopSmsMetricsRecorder();
            }
        }
        return smsMetricsRecorder;
    }
}
