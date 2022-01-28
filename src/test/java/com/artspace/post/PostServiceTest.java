package com.artspace.post;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artspace.post.data.PostDataAccess;
import com.artspace.post.outgoing.DataEmitter;
import com.artspace.post.outgoing.PostDTO;
import com.github.javafaker.Faker;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

  private static final Duration ONE_SECOND = Duration.ofSeconds(1L);

  @Mock
  PostDataAccess postDataAccess;

  @Mock
  DataEmitter<PostDTO> emitter;

  PostMapper postMapper = Mappers.getMapper(PostMapper.class);

  PostService postService;

  static Faker FAKER;

  @BeforeEach
  public void setup() {
    this.postService = new PostService(postDataAccess, postMapper, emitter);
  }

  @BeforeAll
  public static void setupBefore() {
    FAKER = new Faker(Locale.ENGLISH);
  }

  @Test
  @DisplayName("Registered Author should be normalized")
  void registerAuthorShouldNormalizeData() {
    //given
    var author = getSampleAuthor();
    author.setUsername("John.Doe ");

    when(postDataAccess.persist(any(Author.class))).thenAnswer(
        a -> Uni.createFrom().item(a.getArguments()[0]));

    //when
    var persistedAuthor = this.postService.registerAuthor(author).await()
        .atMost(ONE_SECOND);

    //then
    assertThat(persistedAuthor.getUsername(), is("john.doe"));
  }

  @Test
  @DisplayName("Updated Authors should be normalized")
  void updateAuthorShouldNormalizeData() {
    //given
    var author = getSampleAuthor();
    author.setUsername("John.Doe ");

    when(postDataAccess.merge(any(Author.class))).thenAnswer(
        a -> Uni.createFrom().item(Optional.of(a.getArguments()[0])));

    //when
    var updatedAuthor = this.postService.updateAuthor(author).await()
        .atMost(ONE_SECOND);

    //then
    assertThat(updatedAuthor.get().getUsername(), is("john.doe"));
  }

  @ParameterizedTest
  @EmptySource
  @NullSource
  @ValueSource(strings = "   ")
  @DisplayName("find authors by name should return empty if arguments are invalid")
  void findAuthorByUsernameShouldReturnEmptyIfUsernameIsInvalid(String username) {
    //when
    final var result = this.postService.findAuthorByUsername(username).await().atMost(ONE_SECOND);

    //then
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Find author by username should be case insensitive")
  void findAuthorByUsernameShouldBeCaseInsensitive() {
    //given
    var sample = getSampleAuthor();
    when(postDataAccess.findAuthorByUsername(eq("john.doe"))).thenReturn(
        Uni.createFrom().item(Optional.of(sample)));

    //when
    final var result = this.postService.findAuthorByUsername("JoHn.DoE").await().atMost(ONE_SECOND);

    //then
    assertTrue(result.isPresent());
    assertThat(result.get(), is(sample));
  }

  @Test
  @DisplayName("PersistOrUpdate should persist if author is new")
  void persistOrUpdateAuthorShouldPersistIfIsNew() {
    //given
    var sample = getSampleAuthor();
    when(postDataAccess.findAuthorByUsername(anyString())).thenReturn(
        Uni.createFrom().item(Optional.empty()));

    when(postDataAccess.persist(any(Author.class))).thenReturn(
        Uni.createFrom().item(sample));

    //when
    final var result = this.postService.persistOrUpdateAuthor(sample);

    //then
    assertTrue(result.isPresent());
    assertThat(result.get(), is(sample));
  }

  @Test
  @DisplayName("PersistOrUpdate should merge if author is already persisted")
  void persistOrUpdateAuthorShouldMergeIfAuthorExists() {
    //given
    var sample = getSampleAuthor();
    final var optionalAuthor = Optional.of(sample);
    when(postDataAccess.findAuthorByUsername(anyString())).thenReturn(
        Uni.createFrom().item(optionalAuthor));

    when(postDataAccess.merge(any(Author.class))).thenReturn(
        Uni.createFrom().item(optionalAuthor));

    //when
    final var result = this.postService.persistOrUpdateAuthor(sample);

    //then
    assertTrue(result.isPresent());
    assertThat(result.get(), is(sample));
  }

  @ParameterizedTest
  @EmptySource
  @NullSource
  @ValueSource(strings = "   ")
  @DisplayName("Is Authors active  should return false  if arguments are invalid")
  void isAuthorActiveShouldReturnFalseIfUsernameIsInvalid(String username) {
    //when
    final var result = this.postService.isAuthorActive(username).await().atMost(ONE_SECOND);

    //then
    assertFalse(result);
  }

  @DisplayName("IsAuthorActive should reflect current author active status")
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void isAuthorActiveShouldReturnAuthorStatus(boolean isActive) {
    //given
    when(this.postDataAccess.isAuthorActive(anyString())).thenReturn(
        Uni.createFrom().item(isActive));

    //when
    final var result = this.postService.isAuthorActive("john.doe").await()
        .atMost(ONE_SECOND);

    //then
    assertEquals(isActive, result);
  }

  @Test
  @DisplayName("InsertPost should notify the persistence")
  void persistPostShouldEmit() {
    //given
    var post = getSamplePost();
    when(postDataAccess.persist(any(Post.class))).thenReturn(
        Uni.createFrom().item(post));

    var correlationId = createSampleCorrelationId();

    //when
    this.postService.insertPost(post, correlationId).await().atMost(ONE_SECOND);

    //then
    verify(this.emitter, times(1)).emit(eq(correlationId), any(PostDTO.class));
  }

  @Test
  @DisplayName("UpdatePost should notify the update")
  void updatePostShouldEmit() {
    //given
    var post = getSamplePost();
    post.setId(new ObjectId());
    when(postDataAccess.findById(any(ObjectId.class))).thenReturn(
        Uni.createFrom().item(Optional.of(post)));

    when(postDataAccess.merge(post)).thenReturn(
        Uni.createFrom().item(Optional.of(post)));

    var correlationId = createSampleCorrelationId();

    //when
    this.postService.updatePost(post, correlationId);

    //then
    verify(this.emitter, times(1)).emit(eq(correlationId), any(PostDTO.class));
  }


  private static Author getSampleAuthor() {
    final var author = new Author();
    author.setActive(true);
    author.setUsername(FAKER.name().username());
    author.setId(new ObjectId());
    return author;
  }

  private static String createSampleCorrelationId() {
    return UUID.randomUUID().toString();
  }

  private static Post getSamplePost() {
    final var post = new Post();
    post.setAuthor(FAKER.name().username());
    post.setMessage(FAKER.lorem().sentence());
    return post;
  }
}