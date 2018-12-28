package com.github.mbode.kafkalagexporter;

import static org.assertj.core.api.Assertions.assertThat;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class KafkaClientWrapperTest {
  @RegisterExtension
  static final SharedKafkaTestResource kafka = new SharedKafkaTestResource().withBrokers(2);

  private static KafkaClientWrapper kafkaClientWrapper;

  @BeforeAll
  static void setUpKafka() throws KafkaClientException {
    kafkaClientWrapper = new KafkaClientWrapper(kafka.getKafkaConnectString(), 5000);
    kafka.getKafkaTestUtils().createTopic("topic1", 1, (short) 1);
    kafka.getKafkaTestUtils().createTopic("topic2", 1, (short) 1);
    kafka.getKafkaTestUtils().createTopic("topic3", 1, (short) 1);
  }

  @Test
  void exceptionIsThrownOnUnresolvableKafkaCluster() {
    Assertions.assertThrows(
        KafkaClientException.class, () -> new KafkaClientWrapper("i.dont.exist:9092", 5000));
  }

  @Test
  void exceptionIsThrownOnUnreachableKafkaCluster() {
    Assertions.assertThrows(
        KafkaClientException.class, () -> new KafkaClientWrapper("localhost:9999", 1));
  }

  @Test
  void topicsCanBeListed() throws KafkaClientException {
    assertThat(kafkaClientWrapper.listTopics()).containsOnly("topic1", "topic2", "topic3");
  }

  @Test
  void consumerGroupsCanBeListed() throws KafkaClientException {
    kafka.getKafkaTestUtils().consumeAllRecordsFromTopic("topic1");

    Properties properties = new Properties();
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "group");
    final KafkaConsumer<byte[], byte[]> consumer =
        kafka
            .getKafkaTestUtils()
            .getKafkaConsumer(ByteArrayDeserializer.class, ByteArrayDeserializer.class, properties);
    consumer.subscribe(Collections.singleton("topic1"));
    consumer.poll(Duration.ofMillis(500));
    consumer.commitSync();

    final Set<String> consumerGroups = kafkaClientWrapper.listConsumerGroups();
    assertThat(consumerGroups).contains("", "group");
  }

  @Test
  void consumerGroupOffsetsCanBeListed() throws KafkaClientException {
    kafka.getKafkaTestUtils().produceRecords(7, "topic1", 0);
    kafka.getKafkaTestUtils().consumeAllRecordsFromTopic("topic1");

    final Map<TopicPartition, Long> offsets = kafkaClientWrapper.listConsumerGroupOffsets("");
    assertThat(offsets).containsEntry(new TopicPartition("topic1", 0), 7L);
  }

  @Test
  void highWatermarkCanBeListed() {
    kafka.getKafkaTestUtils().produceRecords(7, "topic2", 0);
    kafka.getKafkaTestUtils().consumeAllRecordsFromTopic("topic2");

    final Map<TopicPartition, Long> lags = kafkaClientWrapper.listHighWatermarks("topic2");
    assertThat(lags).containsEntry(new TopicPartition("topic2", 0), 7L);
  }

  @Test
  void lagCanBeListed() throws KafkaClientException {
    kafka.getKafkaTestUtils().produceRecords(7, "topic3", 0);
    kafka.getKafkaTestUtils().consumeAllRecordsFromTopic("topic3");
    kafka.getKafkaTestUtils().produceRecords(42, "topic3", 0);

    final Map<TopicPartition, Long> lags = kafkaClientWrapper.listLag("");
    assertThat(lags).containsEntry(new TopicPartition("topic3", 0), 42L);
  }
}
