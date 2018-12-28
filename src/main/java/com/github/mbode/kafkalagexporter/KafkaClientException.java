package com.github.mbode.kafkalagexporter;

class KafkaClientException extends Exception {
  KafkaClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
