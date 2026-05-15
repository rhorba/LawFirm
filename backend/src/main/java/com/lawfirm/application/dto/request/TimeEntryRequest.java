package com.lawfirm.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeEntryRequest(
    @NotNull Long lawyerId,
    @NotNull LocalDate entryDate,
    @NotNull @DecimalMin("0.01") BigDecimal hours,
    @NotBlank String description,
    @NotNull @DecimalMin("0") BigDecimal hourlyRate,
    boolean billable
) {}
