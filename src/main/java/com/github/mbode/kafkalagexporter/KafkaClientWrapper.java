package com.github.mbode.kafkalagexporter;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

@Log4j2
class KafkaClientWrapper {
  private final AdminClient adminClient;
  private final int timeoutMs;
  private final Properties kafkaProperties = new Properties();

  KafkaClientWrapper(final String kafkaConnectString, final int timeoutMs)
      throws KafkaClientException {
    this.timeoutMs = timeoutMs;

    kafkaProperties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectString);

    log.info("Connecting to Kafka with connect string {}.", kafkaConnectString);
    try {
      adminClient = AdminClient.create(kafkaProperties);
      adminClient.describeCluster().clusterId().get(timeoutMs, MILLISECONDS);
    } catch (ExecutionException | InterruptedException | KafkaException | TimeoutException e) {
      throw new KafkaClientException("Error while connecting to " + kafkaConnectString + ".", e);
    }
    log.debug("Connecting successful.");
  }

  Set<String> listTopics() throws KafkaClientException {
    log.debug("Listing topics.");
    final Set<String> topics;
    try {
      topics = adminClient.listTopics().names().get(timeoutMs, MILLISECONDS);
    } catch (ExecutionException | InterruptedException | KafkaException | TimeoutException e) {
      throw new KafkaClientException("Error while listing topics.", e);
    }
    log.debug("Topics: {}.", topics);
    return topics;
  }

  Set<String> listConsumerGroups() throws KafkaClientException {
    log.debug("Listing consumer groups.");
    final Set<String> consumerGroups;
    try {
      consumerGroups =
          adminClient
              .listConsumerGroups()
              .all()
              .get(timeoutMs, MILLISECONDS)
              .stream()
              .map(ConsumerGroupListing::groupId)
              .collect(Collectors.toSet());
    } catch (ExecutionException | InterruptedException | KafkaException | TimeoutException e) {
      throw new KafkaClientException("Error while listing consumer groups.", e);
    }
    log.debug("Consumer groups: {}.", consumerGroups);
    return consumerGroups;
  }

  Map<TopicPartition, Long> listConsumerGroupOffsets(String groupId) throws KafkaClientException {
    log.debug("Listing offsets.");
    final Map<TopicPartition, Long> offsets;
    try {
      offsets =
          adminClient
              .listConsumerGroupOffsets(groupId)
              .partitionsToOffsetAndMetadata()
              .get(timeoutMs, MILLISECONDS)
              .entrySet()
              .stream()
              .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().offset()));
    } catch (ExecutionException | InterruptedException | KafkaException | TimeoutException e) {
      throw new KafkaClientException("Error while listing consumer groups.", e);
    }
    log.debug("Offsets: {}", offsets);
    return offsets;
  }

  Map<TopicPartition, Long> listHighWatermarks(String topic) {
    log.debug("Listing high watermarks.");
    final Properties consumerProperties = new Properties();
    consumerProperties.putAll(kafkaProperties);
    consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProperties.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "lags");

    final KafkaConsumer<Object, Object> consumer = new KafkaConsumer<>(consumerProperties);
    final Set<TopicPartition> topicPartitions =
        consumer
            .partitionsFor(topic)
            .stream()
            .map(x -> new TopicPartition(x.topic(), x.partition()))
            .collect(Collectors.toSet());
    consumer.assign(topicPartitions);
    consumer.seekToEnd(consumer.assignment());
    final Map<TopicPartition, Long> highWatermarks =
        topicPartitions.stream().collect(Collectors.toMap(Function.identity(), consumer::position));
    log.debug("High watermarks: {}", highWatermarks);
    return highWatermarks;
  }

  Map<TopicPartition, Long> listLag(String groupId) throws KafkaClientException {
    log.debug("Listing lags.");
    final Map<TopicPartition, Long> offsets = listConsumerGroupOffsets(groupId);
    final Map<TopicPartition, Long> lags =
        offsets
            .keySet()
            .stream()
            .map(TopicPartition::topic)
            .distinct()
            .flatMap(x -> listHighWatermarks(x).entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    offsets.forEach((k, v) -> lags.merge(k, v, (x, y) -> x - y));
    log.debug("Lags: {}", lags);
    return lags;
  }
}
