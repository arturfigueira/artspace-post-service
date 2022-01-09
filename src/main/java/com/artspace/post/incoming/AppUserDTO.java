package com.artspace.post.incoming;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@EqualsAndHashCode
/**
 * POJO class to receive user data from external sources
 */
class AppUserDTO {
  @NotNull @NotEmpty
  private String username;

  @NotNull @NotEmpty
  private String firstName;

  private String lasName;

  private boolean isActive;
}
