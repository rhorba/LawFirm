package com.boilerplate.application.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String bio;
}
