package com.artspace.post.incoming;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * {@code RecordConsumingException} is a exceptions that can be thrown during the cosuming external
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
}
