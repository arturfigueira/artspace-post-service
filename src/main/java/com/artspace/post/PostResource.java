package com.artspace.post;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.smallrye.mutiny.Uni;
import java.net.URI;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import lombok.AllArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestPath;

@Path("/api/posts")
@Tag(name = "posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AllArgsConstructor
public class PostResource {

  final PostService postService;
  final Logger logger;

  @Operation(summary = "Returns a post by it's id")
  @GET
  @Path("/{postId}")
  @APIResponse(
      responseCode = "200",
      content =
      @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Post.class)))
  @APIResponse(responseCode = "204", description = "Post not found for a given postId")
  @APIResponse(responseCode = "400", description = "postId is invalid")
  public Uni<Response> getPostById(@NotEmpty @NotNull @RestPath String postId) {

    return this.postService.retrievePostById(postId).map(optionalPost -> {
      var response = Response.noContent().build();

      if (optionalPost.isPresent()) {
        logger.tracef("Found post %s", optionalPost.get());
        response = Response.ok(optionalPost.get()).build();
      } else {
        logger.tracef("Post not found with postId %s", postId);
      }

      return response;
    });
  }

  @Operation(summary = "Saves a post")
  @POST
  @APIResponse(
      responseCode = "201",
      description = "Saved post's URI",
      content =
      @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = URI.class)))
  @APIResponse(
      responseCode = "400",
      description = "Post not persisted due to invalid data")
  public Uni<Response> savePost(@Valid final Post post, @Context UriInfo uriInfo) {
    var persistedPost = postService.insertPost(post);
    return persistedPost.map(entity -> {
      final var builder = uriInfo.getAbsolutePathBuilder().path(entity.getId().toString());
      final var postURI = builder.build();
      logger.tracef("New Post saved with URI %s", postURI);
      return Response.created(postURI).build();
    });
  }

  @Operation(summary = "Query posts")
  @GET
  @APIResponse(
      responseCode = "200",
      content =
      @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Post.class)))
  @APIResponse(responseCode = "400", description = "Query params contains invalid data")
  public Uni<Response> queryPosts(@NotNull @NotEmpty @QueryParam("author") String username) {
    var postsByAuthor = postService.retrievePostsByAuthor(username);
    return postsByAuthor.map(entities -> {
      logger.tracef("Found %s posts for %s", entities.size(), username);
      return Response.ok(entities).build();
    });
  }
}