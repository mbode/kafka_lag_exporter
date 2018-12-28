package com.github.mbode.kafkalagexporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

class KafkaLagExporterIT {
  private static final String PROMETHEUS_URL =
      "http://"
          + System.getProperty("prometheus.host")
          + ":"
          + Integer.getInteger("prometheus.tcp.9090")
          + "/";

  @Test
  void highWatermarkMetricIsAvailableInPrometheus() {
    await()
        .untilAsserted(
            () ->
                assertThat(
                        getFromPrometheus(
                            "kafka_high_watermark{topic=\"test\",partition=\"0\",job=\"kafka_lag_exporter\"}"))
                    .isEqualTo(4));
  }

  @Test
  void offsetMetricIsAvailableInPrometheus() {
    await()
        .untilAsserted(
            () ->
                assertThat(
                        getFromPrometheus(
                            "kafka_offset{consumer_group=\"mygroup\",topic=\"test\",partition=\"0\",job=\"kafka_lag_exporter\"}"))
                    .isEqualTo(3));
  }

  @Test
  void lagMetricIsAvailableInPrometheus() {
    await()
        .untilAsserted(
            () ->
                assertThat(
                        getFromPrometheus(
                            "kafka_lag{consumer_group=\"mygroup\",topic=\"test\",partition=\"0\",job=\"kafka_lag_exporter\"}"))
                    .isEqualTo(1));
  }

  private static Double getFromPrometheus(String query)
      throws UnirestException, UnsupportedEncodingException {
    final JSONArray resultArray =
        Unirest.get(
                PROMETHEUS_URL
                    + "api/v1/query?query="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8.displayName()))
            .asJson()
            .getBody()
            .getObject()
            .getJSONObject("data")
            .getJSONArray("result");
    if (resultArray.length() > 0) {
      return resultArray.getJSONObject(0).getJSONArray("value").getDouble(1);
    }
    return null;
  }
}
