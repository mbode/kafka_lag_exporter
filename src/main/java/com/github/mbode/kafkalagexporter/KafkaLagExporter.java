package com.github.mbode.kafkalagexporter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;

@Log4j2
@CommandLine.Command(
  name = "kafka_lag_exporter",
  description = "Report Kafka consumer group lags to Prometheus.",
  mixinStandardHelpOptions = true,
  version = "0.1.0-SNAPSHOT"
)
public class KafkaLagExporter implements Callable<Void> {
  @CommandLine.Option(
    names = {"-k", "--kafka"},
    required = true,
    description = "Connection string that is used to connect to Kafka cluster, e.g. localhost:9092."
  )
  @Setter
  private String kafkaConnectString;

  @CommandLine.Option(
    names = {"-t", "--timeout"},
    defaultValue = "5000",
    description =
        "Timeout (in milliseconds) used when connecting to Kafka cluster, defaults to 5000."
  )
  private int timeoutMs;

  @CommandLine.Option(
    names = {"-i", "--interval"},
    defaultValue = "500",
    description =
        "Report interval (in milliseconds) used between querying Kafka API for new data, defaults to 500. Note: it can take up to this timespan plus the configured scrape interval for information to be available in Prometheus."
  )
  private int intervalMs;

  @CommandLine.Option(
    names = {"-p", "--port"},
    defaultValue = "9526",
    description = "Port the Prometheus HTTP endpoint is exposed to, defaults to 9526."
  )
  @Setter
  private int port;

  @CommandLine.Option(
    names = {"-r", "--retries"},
    defaultValue = "120",
    description =
        "Max retries when connection to Kafka cluster is lost intermittently, defaults to 120."
  )
  private int maxRetries;

  @CommandLine.Option(
    names = {"-l", "--loglevel"},
    defaultValue = "WARN",
    description = "Log level, valid values: ERROR|WARN|INFO|DEBUG|TRACE. Defaults to WARN."
  )
  private Level logLevel;

  public static void main(String[] args) {
    CommandLine cmd =
        new CommandLine(new KafkaLagExporter())
            .registerConverter(Level.class, x -> Level.getLevel(x.toUpperCase()));
    cmd.parseWithHandlers(
        new CommandLine.RunLast().useOut(System.out).useAnsi(CommandLine.Help.Ansi.AUTO),
        new CommandLine.DefaultExceptionHandler<List<Object>>()
            .useErr(System.err)
            .useAnsi(CommandLine.Help.Ansi.AUTO)
            .andExit(1),
        args);
  }

  @Override
  public Void call() throws InterruptedException {
    Configurator.setRootLevel(logLevel);
    log.info("Starting kafka_lag_exporter.");

    PrometheusAdapter prometheusAdapter = null;
    try {
      prometheusAdapter =
          new PrometheusAdapter(new KafkaClientWrapper(kafkaConnectString, timeoutMs), port);
    } catch (IOException e) {
      throw new RuntimeException(
          "Problem while starting HttpServer for Prometheus endpoint, exiting.", e);
    } catch (KafkaClientException e) {
      throw new RuntimeException("Problem while connecting to Kafka cluster, exiting.", e);
    }

    int retries = 0;
    while (retries < maxRetries) {
      try {
        prometheusAdapter.report();
        retries = 0;
      } catch (KafkaClientException e) {
        retries++;
        log.warn(
            "Problem while connecting to Kafka cluster, trying again in {} ms, for {} more times.",
            intervalMs,
            maxRetries - retries);
        log.debug("Problem while connecting to Kafka cluster.", e);
      }
      Thread.sleep(intervalMs);
    }
    throw new RuntimeException("Problem while connecting to Kafka cluster, exiting.");
  }
}
