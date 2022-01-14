package com.artspace.post.incoming;

import com.artspace.post.Author;
import com.artspace.post.PostService;
import io.smallrye.mutiny.Uni;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.validation.ValidationException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.bson.types.ObjectId;
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

  /**
   * Consumes records from Kafka with {@link AppUserDTO}. Consumed appUsers will be persisted or
   * updated. If the user were previously registered, an update will occur.
   * <p>
   * Messages are required to have a {@code correlationId} header to identify the transaction that
   * originated this message. Failing to ship a message with this header will force the consumer to
   * ignore the message. No failure will happen, and the record will be acknowledged.
   * <p>
   * Also invalid DTOs will be ignored as well and won't return as a failure, acknowledging the
   * received record.
   * <p>
   * In both cases a {@code Optional.empty()} will returned within a {{@code Uni}
   * <p>
   * Records won't be acknowledged only if a persistence error occurs. This will be considered a
   * failure and will dirty this consumer. A Failed {@code Uni} will be returned in this case. A
   * {@link RecordConsumingException} will be wrapping the original exception
   *
   * @param incomingMessage A record containing a {@code AppUserDTO} to be persisted or updated
   * @return A {@link Uni} that might successfully resolve into a {@link  Author} or a failed uni
   * with {@link RecordConsumingException}
   */
  @Incoming("appusers-in")
  @Timeout()
  @Retry(delay = 10, maxRetries = 5)
  public Uni<Optional<Author>> consume(final ConsumerRecord<String, AppUserDTO> incomingMessage) {
    final var headers = incomingMessage.headers();
    final var correlationHeader = headers.headers(HEADER_CORID);
    if (!correlationHeader.iterator().hasNext()) {
      logger.errorf("Required headers not found. Ignoring Message %s",
          incomingMessage);
      return Uni.createFrom().item(Optional.empty());
    }

    var correlation = Optional.ofNullable(correlationHeader.iterator().next())
        .map(Header::value)
        .map(String::new)
        .map(String::trim)
        .filter(s -> !s.isBlank());

    if (correlation.isEmpty()) {
      logger.errorf("CorrelationId header not found. Ignoring Message %s",
          incomingMessage);
      return Uni.createFrom().item(Optional.empty());
    }

    final var correlationId = correlation.get();
    final var appUser = incomingMessage.value();
    logger.debugf("[%s] New incoming AppUser message to process. %s", correlationId,
        appUser);

    try {
      return postService.persistOrUpdateAuthor(toEntity(appUser))
          .invoke(author -> logger.infof(
              "[%s] AppUser with username %s processed. Author updated/registered with id %s",
              correlationId, appUser.getUsername(), author.map(Author::getId).map(
                  ObjectId::toString).orElse("N/A")))
          .onFailure()
          .transform(e -> new RecordConsumingException(e.getMessage(), incomingMessage));

    } catch (ValidationException e) {
      logger.debugf(
          "[%s] Message with invalid payload. Reason %s",
          correlationId, e);
      return Uni.createFrom().item(Optional.empty());
    } catch (Exception e) {
      return Uni.createFrom()
          .failure(new RecordConsumingException(e.getMessage(), incomingMessage));
    }

  }

  private static Author toEntity(final AppUserDTO appUserDTO) {
    final var author = new Author();
    author.setUsername(appUserDTO.getUsername());
    author.setActive(appUserDTO.isActive());
    return author;
  }

}
