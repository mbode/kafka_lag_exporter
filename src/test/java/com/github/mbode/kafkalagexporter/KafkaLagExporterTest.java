package com.github.mbode.kafkalagexporter;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KafkaLagExporterTest {
  @Test
  void exporterStartsUpWithMandatoryArg() {
    assertThrows(
        RuntimeException.class, () -> KafkaLagExporter.main(new String[] {"-k", "localhost:9999"}));
  }

  @Test
  void exceptionIsThrownOnUnreachableKafkaCluster() {
    final KafkaLagExporter kafkaLagExporter = new KafkaLagExporter();
    kafkaLagExporter.setKafkaConnectString("localhost:9999");
    assertThrows(RuntimeException.class, kafkaLagExporter::call);
  }
}
