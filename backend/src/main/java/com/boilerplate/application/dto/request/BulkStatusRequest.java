package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkStatusRequest {

    @NotEmpty(message = "User IDs are required")
    private List<Long> userIds;

    @NotNull(message = "Enabled status is required")
    private Boolean enabled;
}
