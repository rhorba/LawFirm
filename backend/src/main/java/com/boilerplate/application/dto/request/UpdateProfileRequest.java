package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateProfileRequest {
    
    @Size(max = 100)
    private String firstName;
    
    @Size(max = 100)
    private String lastName;
    
    @Size(max = 20)
    private String phoneNumber;
    
    private String bio;
}
