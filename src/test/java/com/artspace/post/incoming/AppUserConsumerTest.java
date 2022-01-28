package com.artspace.post.incoming;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artspace.post.Author;
import com.artspace.post.PostService;
import com.github.javafaker.Faker;
import com.mongodb.MongoClientException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.validation.ConstraintViolationException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppUserConsumerTest {

  private static final Logger LOGGER = Logger.getLogger(AppUserConsumer.class);

  static Faker FAKER;

  AppUserConsumer appUserConsumer;

  PostService mockedPostService;

  MeterRegistry meterRegistry;

  Timer timer;

  @Captor
  ArgumentCaptor<Author> argumentCaptor;

  @BeforeAll
  public static void setupBefore() {
    FAKER = new Faker(Locale.ENGLISH);
  }

  @BeforeEach
  public void setup() {
    mockedPostService = mock(PostService.class);
    meterRegistry = mock(MeterRegistry.class);
    timer = mock(Timer.class);
    appUserConsumer = new AppUserConsumer(LOGGER, mockedPostService, meterRegistry);
    appUserConsumer.processTimer = timer;
  }

  @Test
  @DisplayName("Messages without a CorrelationId header should be ignored")
  void messagesWithoutCorrelationIdShouldBeIgnored() {
    //give
    final var record = new ConsumerRecord<>("appuser", 1, 100L, "", new AppUserDTO());

    //when
    this.appUserConsumer.consume(record);

    //then
    verify(mockedPostService, never()).persistOrUpdateAuthor(any(Author.class));
  }

  @ParameterizedTest
  @NullSource
  @EmptySource
  @ValueSource(strings = "     ")
  @DisplayName("Messages without a valid CorrelationId should be ignored")
  void messagesWithInvalidCorrelationIdShouldBeIgnored(String invalidCorrelationId) {
    //give
    final var record = sampleIncomingMessage(invalidCorrelationId,
        new AppUserDTO());

    //when
    this.appUserConsumer.consume(record);

    //then
    verify(mockedPostService, never()).persistOrUpdateAuthor(any(Author.class));
  }

  @Test
  @DisplayName("Messages with an invalid author should be ignored")
  void messagesWithInvalidAuthorShouldBeIgnored() {
    //give
    final var record = sampleIncomingMessage(UUID.randomUUID().toString(),
        new AppUserDTO());

    when(this.mockedPostService.persistOrUpdateAuthor(any())).thenThrow(
        new ConstraintViolationException(Set.of()));

    //then
    Assertions.assertDoesNotThrow(()->this.appUserConsumer.consume(record));
  }


  @Test
  @DisplayName("Valid authors received from a Message should be Acknowledge")
  void messagesWithNewAuthorsShouldBePersisted() {
    //given
    final AppUserDTO appUserDTO = sampleAppUser();
    final var record = sampleIncomingMessage(UUID.randomUUID().toString(),
        appUserDTO);

    when(this.mockedPostService.persistOrUpdateAuthor(any())).thenReturn(
        Optional.of(toEntity(appUserDTO)));

    //when
    this.appUserConsumer.consume(record);

    //then
    verify(mockedPostService).persistOrUpdateAuthor(argumentCaptor.capture());
    final var data = argumentCaptor.getValue();
    assertThat(data.getUsername(), is(appUserDTO.getUsername()));
    assertTrue(data.isActive());
  }

  @Test
  @DisplayName("Throwing while persist should notAck the message")
  void messagesWithAuthorThatThrowsErrorShouldNotBeAcknowledge() {
    //given
    final var record = sampleIncomingMessage(UUID.randomUUID().toString(),
        sampleAppUser());

    when(this.mockedPostService.persistOrUpdateAuthor(any())).thenThrow(
        new MongoClientException("Forced Error"));

    //then
    Assertions.assertThrows(RecordConsumingException.class, () -> this.appUserConsumer.consume(record));
  }

  @Test
  @DisplayName("Failing to persist a received author should notAck the message")
  void messagesWithAuthorThatFailsShouldNotBeAcknowledge() {
    //given
    final var record = sampleIncomingMessage(UUID.randomUUID().toString(),
        sampleAppUser());

    when(this.mockedPostService.persistOrUpdateAuthor(any())).thenReturn(Optional.empty());

    //then
    Assertions.assertThrows(RecordConsumingException.class, () -> this.appUserConsumer.consume(record));
  }

  private static AppUserDTO sampleAppUser() {
    final var appUserDTO = new AppUserDTO();
    appUserDTO.setUsername(FAKER.name().username());
    appUserDTO.setFirstName(FAKER.name().firstName());
    appUserDTO.setLasName(FAKER.name().lastName());
    appUserDTO.setActive(true);
    return appUserDTO;
  }

  private static Author toEntity(AppUserDTO appUserDTO) {
    final var author = new Author();
    author.setActive(appUserDTO.isActive());
    author.setUsername(appUserDTO.getUsername());
    author.setId(new ObjectId());
    return author;
  }

  private static ConsumerRecord<String, AppUserDTO> sampleIncomingMessage(
      String invalidCorrelationId,
      final AppUserDTO appUserDTO) {
    var record = new ConsumerRecord<String, AppUserDTO>("mock", 1, 1L, null, appUserDTO);
    record.headers().add("correlationId", Optional.ofNullable(invalidCorrelationId)
        .map(String::getBytes).orElse(null));

    return record;
  }
}