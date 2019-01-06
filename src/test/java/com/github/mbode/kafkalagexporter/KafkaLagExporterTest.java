package com.github.mbode.kafkalagexporter;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class KafkaLagExporterTest {
  @RegisterExtension static final SharedKafkaTestResource kafka = new SharedKafkaTestResource();

  @Test
  void exporterStartsUp() {
    assertThrows(
        TimeoutException.class,
        () ->
            Executors.newSingleThreadExecutor()
                .submit(
                    () -> {
                      KafkaLagExporter.main(new String[] {"-k", kafka.getKafkaConnectString()});
                    })
                .get(5, TimeUnit.SECONDS));
  }

  @Test
  void exceptionIsThrownOnUnreachableKafkaCluster() {
    final KafkaLagExporter kafkaLagExporter = new KafkaLagExporter();
    kafkaLagExporter.setKafkaConnectString("localhost:9999");
    assertThrows(RuntimeException.class, kafkaLagExporter::call);
  }
}
