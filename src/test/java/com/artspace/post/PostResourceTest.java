package com.artspace.post;


import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.notNullValue;

import com.artspace.post.data.PostRepository;
import com.github.javafaker.Faker;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import org.bson.types.ObjectId;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostResourceTest {

  private static final String JSON = "application/json;charset=UTF-8";

  private static Faker FAKER;

  private static final Duration FIVE_SECONDS = Duration.ofSeconds(5);

  @Inject
  PostService postService;

  @Inject
  PostRepository postRepository;

  @BeforeAll
  static void setup() {
    FAKER = new Faker();
  }

  @BeforeEach
  void before() {
    postRepository.deleteAll().await().atMost(FIVE_SECONDS);
  }


  @Test
  @DisplayName("An OPEN Api resource should be available")
  void shouldPingOpenAPI() {
    given()
        .header(ACCEPT, APPLICATION_JSON)
        .when()
        .get("/q/openapi")
        .then()
        .statusCode(OK.getStatusCode());
  }

  @Test
  @DisplayName("A post should not be persisted if user doesn't exists")
  void registerPostShouldNotContinueIfAuthorDoesNotExists() {
    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, createSampleCorrelationId())
        .body(createSamplePost())
        .when()
        .post("/api/posts")
        .then()
        .statusCode(BAD_REQUEST.getStatusCode());
  }

  @Test
  @DisplayName("A post should not be persisted if user is inactive")
  void registerPostShouldNotContinueIfAuthorIsInactive() {

    final var sampleAuthor = this.createSampleAuthor();
    sampleAuthor.setActive(false);
    this.postService.registerAuthor(sampleAuthor);

    final var samplePost = this.createSamplePost();
    samplePost.setAuthor(sampleAuthor.getUsername());

    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, createSampleCorrelationId())
        .body(samplePost)
        .when()
        .post("/api/posts")
        .then()
        .statusCode(BAD_REQUEST.getStatusCode());
  }

  @Test
  @DisplayName("An empty post should not be processed")
  void registerPostShouldNotAcceptNullPosts() {
    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, createSampleCorrelationId())
        .when()
        .post("/api/posts")
        .then()
        .statusCode(BAD_REQUEST.getStatusCode());
  }

  @Test
  @DisplayName("A Post without author should not be persisted")
  void registerPostShouldAcceptPostsWithoutAuthor() {
    final var samplePost = this.createSamplePost();
    samplePost.setAuthor(null);

    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, createSampleCorrelationId())
        .when()
        .body(samplePost)
        .post("/api/posts")
        .then()
        .statusCode(BAD_REQUEST.getStatusCode());
  }

  @Test
  @DisplayName("Should be able to retrieve a post by its Id if exists")
  void getPostByIdShouldReturnExistentPost() {

    final var sampleAuthor = this.postService.registerAuthor(this.createSampleAuthor()).await()
        .atMost(FIVE_SECONDS);

    final var correlationId =  createSampleCorrelationId();

    final var samplePost = this.createSamplePost();
    samplePost.setAuthor(sampleAuthor.getUsername());
    var persistedPost = this.postService.insertPost(samplePost, correlationId)
        .await().atMost(FIVE_SECONDS);

    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, correlationId)
        .pathParam("postId", persistedPost.getId().toString())
        .when()
        .get("/api/posts/{postId}")
        .then()
        .statusCode(OK.getStatusCode())
        .header(CONTENT_TYPE, "application/json")
        .body("id", notNullValue())
        .body("author", Is.is(samplePost.getAuthor()))
        .body("message", Is.is(samplePost.getMessage()))
        .body("enabled", Is.is(true))
        .body("creationTime", notNullValue());
  }

  @Test
  @DisplayName("A valid post should persisted")
  void registerPostShouldPersistValidPost() {

    final var sampleAuthor = this.postService.registerAuthor(this.createSampleAuthor()).await()
        .atMost(FIVE_SECONDS);

    Boolean aBoolean = this.postService.isAuthorActive(sampleAuthor.getUsername()).await()
        .atMost(FIVE_SECONDS);

    final var samplePost = this.createSamplePost();
    samplePost.setAuthor(sampleAuthor.getUsername());

    var location = given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, createSampleCorrelationId())
        .body(samplePost)
        .when()
        .post("/api/posts")
        .then()
        .statusCode(CREATED.getStatusCode())
        .extract()
        .header("Location");

    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, createSampleCorrelationId())
        .when()
        .get(location)
        .then()
        .statusCode(OK.getStatusCode())
        .header(CONTENT_TYPE, "application/json")
        .body("id", notNullValue())
        .body("author", Is.is(samplePost.getAuthor()))
        .body("message", Is.is(samplePost.getMessage()))
        .body("enabled", Is.is(true))
        .body("creationTime", notNullValue());
  }

  @ParameterizedTest
  @CsvSource({"0,0", "0,-1", "-1,20"})
  @DisplayName("Query Posts fail if pagination settings are invalid. Page:{}, Size:{}")
  void queryPostsShouldReturnEmptyIfAuthorIsInactive(String page, String size) {
    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, createSampleCorrelationId())
        .pathParam("index", page)
        .pathParam("size", size)
        .when()
        .get("/api/posts?index={index}&size={size}")
        .then()
        .statusCode(BAD_REQUEST.getStatusCode());
  }

  @Test
  @DisplayName("Query Posts should return an empty result if filtered author is inactive")
  void queryPostsShouldReturnEmptyIfAuthorIsInactive() {
    final var sampleAuthor = this.postService.registerAuthor(this.createSampleAuthor()).await()
        .atMost(FIVE_SECONDS);

    final var correlationId = createSampleCorrelationId();

    final var samplePost = this.createSamplePost();
    samplePost.setAuthor(sampleAuthor.getUsername());
    this.postService
        .insertPost(samplePost, correlationId)
        .chain(() -> this.postService.updateAuthor(sampleAuthor.withActive(false)))
        .await()
        .atMost(FIVE_SECONDS);

    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, correlationId)
        .pathParam("username", sampleAuthor.getUsername())
        .when()
        .get("/api/posts?author={username}")
        .then()
        .statusCode(OK.getStatusCode())
        .header(CONTENT_TYPE, "application/json")
        .body("", Matchers.empty());
  }

  @Test
  @DisplayName("Query Posts without params should query by all authors paginated")
  void queryPostsShouldFailWithoutParams() {

    List<Uni<Post>> uniPosts = new ArrayList<>();
    for (var index = 0; index < 5; index++) {
      uniPosts.add(this.postService.registerAuthor(this.createSampleAuthor()).chain(author -> {
        final var samplePost = this.createSamplePost();
        samplePost.setAuthor(author.getUsername());
        return this.postService.insertPost(samplePost, createSampleCorrelationId());
      }));
    }

    Uni.combine().all().unis(uniPosts).combinedWith(List::size).await().atMost(FIVE_SECONDS);

    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, createSampleCorrelationId())
        .when()
        .get("/api/posts")
        .then()
        .statusCode(OK.getStatusCode())
        .header(CONTENT_TYPE, "application/json")
        .body("size()", Is.is(5));
  }


  @Test
  @DisplayName("Query Posts must be able to filter enabled posts")
  void queryPostsSMustBeAbleToFilterEnabledPosts() {

    final var expectedObjectId = samplePostFilterTestData("enabled");

    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, createSampleCorrelationId())
        .pathParam("status", "enabled")
        .when()
        .get("/api/posts?status={status}")
        .then()
        .statusCode(OK.getStatusCode())
        .header(CONTENT_TYPE, "application/json")
        .body("size()", Is.is(1))
        .body("id", Is.is(List.of(expectedObjectId.toString())));
  }

  @Test
  @DisplayName("Query Posts must be able to filter disabled posts")
  void queryPostsSMustBeAbleToFilterDisabledPosts() {

    final var expectedObjectId = samplePostFilterTestData("disabled");

    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, createSampleCorrelationId())
        .pathParam("status", "disabled")
        .when()
        .get("/api/posts?status={status}")
        .then()
        .statusCode(OK.getStatusCode())
        .header(CONTENT_TYPE, "application/json")
        .body("size()", Is.is(1))
        .body("id", Is.is(List.of(expectedObjectId.toString())));
  }

  private ObjectId samplePostFilterTestData(String postStatus) {
    var mainStatus = postStatus.equals("enabled");

    for (var index = 0; index < 3; index++) {
      var post = this.postService
          .registerAuthor(this.createSampleAuthor())
          .chain(author -> {
            final var samplePost = this.createSamplePost();
            samplePost.setAuthor(author.getUsername());
            return this.postService.insertPost(samplePost, createSampleCorrelationId());
          })
          .await()
          .atMost(FIVE_SECONDS);

      this.postService.updatePost(post.withEnabled(!mainStatus), createSampleCorrelationId());
    }

    final var sampleAuthor = this.postService.registerAuthor(this.createSampleAuthor()).await()
        .atMost(FIVE_SECONDS);

    final var targetPost = this.createSamplePost();
    targetPost.setAuthor(sampleAuthor.getUsername());
    var post = this.postService
        .insertPost(targetPost, createSampleCorrelationId())
        .await()
        .atMost(FIVE_SECONDS);

    return this.postService
        .updatePost(post.withEnabled(mainStatus), createSampleCorrelationId())
        .map(Post::getId).get();
  }

  @Test
  @DisplayName("Query Posts must be able to return all post no matter what status")
  void queryPostsSMustBeAbleToReturnAllPostsFromUser() {
    final var sampleAuthor = this.postService.registerAuthor(this.createSampleAuthor()).await()
        .atMost(FIVE_SECONDS);

    final var enabledPost = this.createSamplePost();
    enabledPost.setAuthor(sampleAuthor.getUsername());
    this.postService
        .insertPost(enabledPost, createSampleCorrelationId()).map(Post::getId)
        .await()
        .atMost(FIVE_SECONDS);

    final var disabledPost = this.createSamplePost();
    disabledPost.setAuthor(sampleAuthor.getUsername());
    var post = this.postService
        .insertPost(disabledPost, createSampleCorrelationId())
        .await()
        .atMost(FIVE_SECONDS);

    this.postService.updatePost(post.withEnabled(false), createSampleCorrelationId());

    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, createSampleCorrelationId())
        .pathParam("username", sampleAuthor.getUsername())
        .when()
        .get("/api/posts?author={username}&status=all")
        .then()
        .statusCode(OK.getStatusCode())
        .header(CONTENT_TYPE, "application/json")
        .body("size()", Is.is(2));
  }

  @Test
  @DisplayName("Query Posts should paginate results accordingly")
  void queryPostsShouldPaginateResults() {
    final var sampleAuthor = this.postService.registerAuthor(this.createSampleAuthor()).await()
        .atMost(FIVE_SECONDS);

    final var uniPosts = new ArrayList<Uni<Post>>();
    for (int index = 0; index < 5; index++) {
      final var enabledPost = this.createSamplePost();
      enabledPost.setAuthor(sampleAuthor.getUsername());
      uniPosts.add(this.postService.insertPost(enabledPost, createSampleCorrelationId()));
    }

    Uni.combine().all().unis(uniPosts).combinedWith(List::size).await().atMost(FIVE_SECONDS);

    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, createSampleCorrelationId())
        .when()
        .get("/api/posts?index=0&size=3")
        .then()
        .statusCode(OK.getStatusCode())
        .header(CONTENT_TYPE, "application/json")
        .body("size()", Is.is(3));

    given()
        .header(CONTENT_TYPE, JSON)
        .header(ACCEPT, JSON)
        .header(PostResource.CORRELATION_HEADER, createSampleCorrelationId())
        .when()
        .get("/api/posts?index=1&size=4")
        .then()
        .statusCode(OK.getStatusCode())
        .header(CONTENT_TYPE, "application/json")
        .body("size()", Is.is(1));
  }

  private Author createSampleAuthor() {
    var author = new Author();
    author.setUsername(FAKER.name().username());
    author.activate();
    return author;
  }

  private Post createSamplePost() {
    var post = new Post();
    post.setAuthor(createSampleAuthor().getUsername());
    post.setMessage(FAKER.lorem().sentence());
    return post;
  }

  private String createSampleCorrelationId() {
    return UUID.randomUUID().toString();
  }
}