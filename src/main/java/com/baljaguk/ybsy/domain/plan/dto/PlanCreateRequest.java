package com.baljaguk.ybsy.domain.plan.dto;

import com.baljaguk.ybsy.domain.plan.entity.PlanDetails;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PlanCreateRequest(
        @NotBlank @Size(max = 30) String title,
        @NotNull @Pattern(regexp = "class|activity|project|custom") String type,
        @Min(1) @Max(12) Integer credits,
        @Pattern(regexp = "low|normal|high|custom") String loadLevel,
        @NotNull @Min(1) @Max(16) Integer startWeek,
        @NotNull @Min(1) @Max(16) Integer endWeek,
        @NotNull @DecimalMin("0.5") @DecimalMax("60") BigDecimal hoursPerWeek,
        LocalDate startDate, LocalDate endDate,
        @Pattern(regexp = "low|normal|high") String priority,
        Long categoryId, Boolean noExam,
        @Pattern(regexp = "midterm|final") String examOnly,
        @Size(min = 1, max = 7) List<@NotNull @Min(0) @Max(6) Integer> studyDays) {

    public PlanDetails details() {
        return new PlanDetails(title, type, credits, loadLevel == null ? "normal" : loadLevel,
                startWeek, endWeek, hoursPerWeek, startDate, endDate, priority == null ? "normal" : priority,
                categoryId, Boolean.TRUE.equals(noExam), examOnly, studyDays);
    }
}
