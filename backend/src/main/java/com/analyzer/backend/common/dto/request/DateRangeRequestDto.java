package com.analyzer.backend.common.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class DateRangeRequestDto {

    @NotNull(message = "Start date is required")
    private Instant from;

    @NotNull(message = "End time is required")
    private Instant to;

    public void validate() {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "'from' must be before 'to'. Got from=" + from + " to=" + to);
        }
        if (java.time.Duration.between(from, to).toDays() > 90) {
            throw new IllegalArgumentException(
                    "Date range cannot exceed 90 days");
        }
    }

}
