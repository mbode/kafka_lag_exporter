This project is in a very early phase.
For a more production-ready exporter refer to e.g. [danielqsj/kafka_exporter](https://github.com/danielqsj/kafka_exporter).

---

# kafka_lag_exporter

[![Build Status](https://travis-ci.org/mbode/kafka_lag_exporter.svg?branch=master)](https://travis-ci.org/mbode/kafka_lag_exporter)
[![codecov](https://codecov.io/gh/mbode/kafka_lag_exporter/branch/master/graph/badge.svg)](https://codecov.io/gh/mbode/kafka_lag_exporter)
[![Github release](https://img.shields.io/github/release/mbode/kafka_lag_exporter.svg)](https://github.com/mbode/kafka_lag_exporter/releases)
[![Docker Hub](https://img.shields.io/docker/pulls/maximilianbode/kafka_lag_exporter.svg)](https://hub.docker.com/r/maximilianbode/kafka_lag_exporter)

## Usage
```
kafka_lag_exporter [-hV] [-i=<intervalMs>] -k=<kafkaConnectString>
                          [-l=<logLevel>] [-p=<port>] [-r=<maxRetries>]
                          [-t=<timeoutMs>]
Report Kafka consumer group lags to Prometheus.
  -h, --help          Show this help message and exit.
  -i, --interval=<intervalMs>
                      Report interval (in milliseconds) used between querying Kafka
                        API for new data, defaults to 500. Note: it can take up to
                        this timespan plus the configured scrape interval for
                        information to be available in Prometheus.
  -k, --kafka=<kafkaConnectString>
                      Connection string that is used to connect to Kafka cluster, e.
                        g. localhost:9092.
  -l, --loglevel=<logLevel>
                      Log level, valid values: ERROR|WARN|INFO|DEBUG|TRACE. Defaults
                        to WARN.
  -p, --port=<port>   Port the Prometheus HTTP endpoint is exposed to, defaults to
                        9526.
  -r, --retries=<maxRetries>
                      Max retries when connection to Kafka cluster is lost
                        intermittently, defaults to 120.
  -t, --timeout=<timeoutMs>
                      Timeout (in milliseconds) used when connecting to Kafka
                        cluster, defaults to 5000.
  -V, --version       Print version information and exit.
```

## Metrics

| name                   | labels |
| ---------------------- | --------------------------------------- |
| `kafka_high_watermark` | `partition` , `topic`                   |
| `kafka_offset`         | `consumer_group`, `partition` , `topic` |
| `kafka_lag`            | `consumer_group`, `partition` , `topic` |

## Development
typical tasks:
- verify: `./gradlew check`
- list outdated dependenices: `./gradlew dependencyUpdates`
- update gradle: `./gradlew wrapper --gradle-version=<x.y>` (twice)
