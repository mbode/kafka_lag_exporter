package com.github.mbode.kafkalagexporter;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import java.io.IOException;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@NoArgsConstructor
@Log4j2
class PrometheusAdapter {
  private static final String[] LABEL_NAMES = {"consumer_group", "topic", "partition"};

  private static final Gauge highWatermarks =
      Gauge.build()
          .name("kafka_high_watermark")
          .help("Kafka high watermark reported by kafka_lag_exporter.")
          .labelNames(new String[] {"topic", "partition"})
          .register();

  private static final Gauge offsets =
      Gauge.build()
          .name("kafka_offset")
          .help("Kafka consumer offset reported by kafka_lag_exporter.")
          .labelNames(LABEL_NAMES)
          .register();

  private static final Gauge lags =
      Gauge.build()
          .name("kafka_lag")
          .help("Kafka consumer lag reported by kafka_lag_exporter.")
          .labelNames(LABEL_NAMES)
          .register();

  private KafkaClientWrapper kafkaClientWrapper;

  PrometheusAdapter(KafkaClientWrapper kafkaClientWrapper, int port) throws IOException {
    this.kafkaClientWrapper = kafkaClientWrapper;
    log.info("Starting Prometheus HTTPServer.");
    new HTTPServer(port);
  }

  void report() throws KafkaClientException {
    log.debug("Reporting metrics.");
    for (String consumerGroup : kafkaClientWrapper.listConsumerGroups()) {
      kafkaClientWrapper
          .listConsumerGroupOffsets(consumerGroup)
          .forEach(
              (tp, offset) ->
                  offsets.setChild(
                      new Gauge.Child() {
                        @Override
                        public double get() {
                          return offset;
                        }
                      },
                      consumerGroup,
                      tp.topic(),
                      Integer.toString(tp.partition())));
      kafkaClientWrapper
          .listLag(consumerGroup)
          .forEach(
              (tp, offset) ->
                  lags.setChild(
                      new Gauge.Child() {
                        @Override
                        public double get() {
                          return offset;
                        }
                      },
                      consumerGroup,
                      tp.topic(),
                      Integer.toString(tp.partition())));
    }
    kafkaClientWrapper
        .listTopics()
        .forEach(
            x ->
                kafkaClientWrapper
                    .listHighWatermarks(x)
                    .forEach(
                        (tp, hw) ->
                            highWatermarks.setChild(
                                new Gauge.Child() {
                                  @Override
                                  public double get() {
                                    return hw;
                                  }
                                },
                                tp.topic(),
                                Integer.toString(tp.partition()))));
  }
}
