package com.artspace.post.incoming;

import com.artspace.post.Author;
import com.artspace.post.PostService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.validation.ValidationException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Kafka AppUser Consumer service.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@ApplicationScoped
public class AppUserConsumer {

  private static final String HEADER_CORID = "correlationId";

  final Logger logger;
  final PostService postService;
  final MeterRegistry registry;

  Timer processTimer;

  @PostConstruct
  void init() {
    this.processTimer = Timer.builder("post_consumer_appusers_latency")
        .description("The latency of the AppUsers pipeline")
        .publishPercentiles(0.5, 0.75, 0.95, 0.98, 0.99, 0.999)
        .percentilePrecision(3)
        .distributionStatisticExpiry(Duration.ofMinutes(25))
        .register(registry);
  }

  /**
   * Consumes records from Kafka with {@link AppUserDTO}. Consumed appUsers will be persisted or
   * updated. If the user were previously registered an update will occur.
   * <p>
   * Messages are required to have a {@code correlationId} header to identify the transaction that
   * originated this message. Failing to ship a message with this header will force the consumer to
   * ignore the message. No failure will happen, and the record will be acknowledged.
   * <p>
   * Also invalid DTOs will be ignored as well and won't return as a failure, acknowledging the
   * received record.
   * <p>
   * Records won't be acknowledged only if a persistence error occurs. This will be considered a
   * failure and will dirty this consumer. A Failed {@code Uni} will be returned in this case. A
   *
   * @param incomingMessage A record containing a {@code AppUserDTO} to be persisted or updated
   * @throws RecordConsumingException if the data within the message could not be persisted
   */
  @Incoming("appusers-in")
  @Timeout()
  @Retry(delay = 10, maxRetries = 5)
  @Transactional
  public void consume(final ConsumerRecord<String, AppUserDTO> incomingMessage) {
    final var headers = incomingMessage.headers();
    final var correlationHeader = headers.headers(HEADER_CORID);
    if (!correlationHeader.iterator().hasNext()) {
      logger.errorf("Required headers not found. Ignoring Message %s",
          incomingMessage);
      return;
    }

    var correlation = Optional.ofNullable(correlationHeader.iterator().next())
        .map(Header::value)
        .map(String::new)
        .map(String::trim)
        .filter(s -> !s.isBlank());

    if (correlation.isEmpty()) {
      logger.errorf("CorrelationId header not found. Ignoring Message %s",
          incomingMessage);
      return;
    }

    final var correlationId = correlation.get();
    final var appUser = incomingMessage.value();
    logger.debugf("[%s] New incoming AppUser message to process. %s", correlationId,
        appUser);

    final var eventStartTime = incomingMessage.timestamp();

    Optional<Author> optionalAuthor = Optional.empty();
    try {
      optionalAuthor = postService.persistOrUpdateAuthor(toEntity(appUser));
    } catch (ValidationException e) {
      logger.errorf(
          "[%s] Message with invalid payload. Ignoring Message. Reason %s",
          correlationId, e);
      return;
    } catch (Exception ex) {
      final var message = String.format("[%s] It was not possible to Persist Message", correlation);
      throw new RecordConsumingException(message, ex);
    }

    if (optionalAuthor.isEmpty()) {
      recordTimer(eventStartTime);
      final var message = String.format("[%s] Persist process does not returned anything",
          correlation);
      throw new RecordConsumingException(message);
    }

    final var savedUser = optionalAuthor.get();
    logger.infof(
        "[%s] AppUser with username %s processed. Author updated/registered with id %s",
        correlationId, savedUser.getUsername(), savedUser.getId());
    recordTimer(eventStartTime);
  }

  private void recordTimer(long eventStartTime) {
    final var totalTime = Instant.now()
        .minus(eventStartTime, ChronoUnit.MILLIS)
        .toEpochMilli();
    this.processTimer.record(totalTime, TimeUnit.MILLISECONDS);
  }

  private static Author toEntity(final AppUserDTO appUserDTO) {
    final var author = new Author();
    author.setUsername(appUserDTO.getUsername());
    author.setActive(appUserDTO.isActive());
    return author;
  }

}
