package com.boilerplate.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchRequest {

    private String search;
    private String role;
    private Boolean enabled;
    private Boolean showDeleted;
}
