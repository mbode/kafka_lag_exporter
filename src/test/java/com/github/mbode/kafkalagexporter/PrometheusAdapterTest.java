package com.github.mbode.kafkalagexporter;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.prometheus.client.CollectorRegistry;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrometheusAdapterTest {
  private static final Map<TopicPartition, Long> TEST_DATA =
      ImmutableMap.of(
          new TopicPartition("topic1", 0), 42L,
          new TopicPartition("topic2", 1), 7L);

  @Mock KafkaClientWrapper kafkaClientWrapper;

  @InjectMocks PrometheusAdapter prometheusAdapter;

  @Test
  void metricsEndpointIsAvailableOnConfigurablePort() throws UnirestException, IOException {
    final int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      port = socket.getLocalPort();
    }
    new PrometheusAdapter(kafkaClientWrapper, port);
    assertThat(Unirest.get("http://localhost:" + port).asString().getBody()).isNotEmpty();
  }

  @Test
  void highWatermarksAreReported() throws KafkaClientException {
    Mockito.when(kafkaClientWrapper.listTopics()).thenReturn(Collections.singleton("topic1"));
    Mockito.when(kafkaClientWrapper.listHighWatermarks("topic1"))
        .thenReturn(Collections.singletonMap(new TopicPartition("topic1", 0), 42L));

    prometheusAdapter.report();

    assertThat(
            CollectorRegistry.defaultRegistry.getSampleValue(
                "kafka_high_watermark",
                new String[] {"topic", "partition"},
                new String[] {"topic1", Integer.toString(0)}))
        .isEqualTo(42.0);
  }

  @Test
  void offsetsAreReported() throws KafkaClientException {
    Mockito.when(kafkaClientWrapper.listConsumerGroups())
        .thenReturn(Collections.singleton("group"));
    Mockito.when(kafkaClientWrapper.listConsumerGroupOffsets("group")).thenReturn(TEST_DATA);

    prometheusAdapter.report();

    assertThat(sample("kafka_offset", "topic1", 0)).isEqualTo(42.0);
    assertThat(sample("kafka_offset", "topic2", 1)).isEqualTo(7.0);
  }

  @Test
  void lagsAreReported() throws KafkaClientException {
    Mockito.when(kafkaClientWrapper.listConsumerGroups())
        .thenReturn(Collections.singleton("group"));
    Mockito.when(kafkaClientWrapper.listLag("group")).thenReturn(TEST_DATA);

    prometheusAdapter.report();

    assertThat(sample("kafka_lag", "topic1", 0)).isEqualTo(42.0);
    assertThat(sample("kafka_lag", "topic2", 1)).isEqualTo(7.0);
  }

  private static Double sample(String metricName, String topic, int partition) {
    return CollectorRegistry.defaultRegistry.getSampleValue(
        metricName,
        new String[] {"consumer_group", "topic", "partition"},
        new String[] {"group", topic, Integer.toString(partition)});
  }
}
