package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.ClientType;
import com.lawfirm.domain.model.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateClientRequest(
    @NotNull ClientType clientType,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Size(max = 20) @Pattern(regexp = "^\\+?[0-9\\s\\-]{7,20}$", message = "Invalid phone format") String phone,
    @Email @Size(max = 100) String email,
    String address,
    String notes,
    @Size(max = 20) String cin,
    Gender gender,
    LocalDate dateOfBirth,
    @Size(max = 200) String companyName,
    @Size(max = 50) String taxNumber
) {}
