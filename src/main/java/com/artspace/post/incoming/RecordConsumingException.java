package com.artspace.post.incoming;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * {@code RecordConsumingException} is an exceptions that can be thrown during the consuming external
 * message operations sent by message brokers, such as Kafka.
 *
 * <p>{@code RecordConsumingException} is an <em>unchecked
 * exceptions</em>.
 */
class RecordConsumingException extends RuntimeException {

  public <K, V> RecordConsumingException(final String reason,
      final ConsumerRecord<K, V> consumerRecord) {
    super(reason + " [Record]: " + consumerRecord);
  }

  public RecordConsumingException(final String reason) {
    super(reason);
  }

  public RecordConsumingException(final String message, final Throwable throwable) {
    super(message, throwable);
  }
}
