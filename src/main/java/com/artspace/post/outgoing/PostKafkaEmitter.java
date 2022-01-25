package com.artspace.post.outgoing;

import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecord;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.eclipse.microprofile.reactive.messaging.OnOverflow.Strategy;
import org.jboss.logging.Logger;

/**
 * Concrete implementation of a {@link DataEmitter} which utilizes Kafka as Message Broker
 *
 * <p>The data emitted will be done via {@link PostDTO}, which includes only the necessary
 * information that must be exposed to the external world.
 *
 */
@ApplicationScoped
class PostKafkaEmitter implements DataEmitter<PostDTO> {

  @Getter(AccessLevel.PROTECTED)
  @ConfigProperty(name = "outgoing.correlation.key", defaultValue = "correlationId")
  String correlationKey;

  @Inject Logger logger;

  @Inject
  @Channel("post-out")
  @OnOverflow(value = Strategy.BUFFER, bufferSize = 1000)
  MutinyEmitter<PostDTO> emitter;

  /**
   * {@inheritDoc}
   **
   * @param correlationId identifier of the transaction that originated this necessity of emission
   * @param input data to be emitted
   * @throws IllegalArgumentException if correlationId is null or blank or input is null
   */
  @Timeout()
  @CircuitBreaker(
      requestVolumeThreshold = 10,
      delay = 500L,
      skipOn = {IllegalArgumentException.class})
  @Retry(
      maxRetries = 5,
      delay = 200,
      abortOn = {IllegalArgumentException.class})
  @FibonacciBackoff
  @Bulkhead()
  @Override
  public void emit(final String correlationId, final PostDTO input) {
    final var corId =
        Optional.ofNullable(correlationId)
            .filter(s -> !s.isBlank())
            .orElseThrow(
                () -> new IllegalArgumentException("CorrelationId should not be blank nor null"));

    final var message =
        Optional.ofNullable(input)
            .map(user -> messageOf(user, corId))
            .orElseThrow(() -> new IllegalArgumentException("Emit AppUser can not be null"));

    emitter.send(message);
  }

  private Message<PostDTO> messageOf(final PostDTO post, String correlationId) {
    final var message = Message.of(post);
    return OutgoingKafkaRecord.from(message)
        .withHeader(correlationKey, correlationId.getBytes())
        .withAck(() -> handleAck(correlationId, post))
        .withNack(throwable -> handleNack(correlationId, throwable));
  }

  private CompletableFuture<Void> handleNack(
      String correlationId, final Throwable throwable) {
    return CompletableFuture.runAsync(getFailureRunnable(correlationId, throwable));
  }

  private CompletionStage<Void> handleAck(String correlationId, final PostDTO postDto) {
    return CompletableFuture.runAsync(getCompletionRunnable(correlationId, postDto));
  }

  private Runnable getCompletionRunnable(String correlationId, final PostDTO postDto) {
    return () ->
        logger.infof("[%s] Sent %s was acknowledged by the broker", correlationId, postDto);
  }

  private Runnable getFailureRunnable(
      String correlationId, final Throwable throwable) {
    return () -> logger.errorf(
        "[%s] Sent message was NOT acknowledged by the broker. %s", correlationId, throwable);
  }
}
