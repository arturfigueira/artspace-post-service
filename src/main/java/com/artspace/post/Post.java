package com.artspace.post;

import io.quarkus.mongodb.panache.common.MongoEntity;
import java.time.Instant;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.With;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Represents a User Post with its message and date of creation. By default, the entity will be
 * created enabled and with the current date and time as created time. Methods to update and change
 * this behaviour is available as well.
 *
 * @since 1.0.0
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@ToString
@Data
@With(AccessLevel.PROTECTED)
@MongoEntity(collection = "post")
@Schema(description = "A user post")
public class Post {

  private ObjectId id;

  @NotNull
  @NotEmpty
  private String message;

  @NotNull
  @PastOrPresent
  private Instant creationTime = Instant.now();

  @NotNull
  @Size(min = 3, max = 50)
  @BsonProperty("username")
  private String author;

  private boolean isEnabled = true;


  /**
   * Clones the current instance into a new instance of {@link Post} where this new instance will be
   * set with being created at this exact instant in time.
   *
   * @return A new instance of the current object with creationTime set to {@code Instant.now()}.
   */
  public Post toToday() {
    return this.withCreationTime(Instant.now());
  }

  /**
   * Enable this post
   */
  public void enableIt() {
    this.isEnabled = true;
  }

  /**
   * Toggle post instance. If it's enabled this method will disable it. It'll work the other way
   * around, if the post is disabled.
   */
  public void toggleIt() {
    this.isEnabled = !this.isEnabled;
  }
}
