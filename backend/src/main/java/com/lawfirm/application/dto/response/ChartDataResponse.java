package com.lawfirm.application.dto.response;

import java.util.List;

public record ChartDataResponse(
    List<String> labels,
    List<Number> values,
    List<Number> secondaryValues
) {
    public ChartDataResponse(List<String> labels, List<Number> values) {
        this(labels, values, List.of());
    }
}
