package com.artspace.post.incoming;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class AppUserDeserializer extends ObjectMapperDeserializer<AppUserDTO> {

  public AppUserDeserializer() {
    super(AppUserDTO.class);
  }
}
