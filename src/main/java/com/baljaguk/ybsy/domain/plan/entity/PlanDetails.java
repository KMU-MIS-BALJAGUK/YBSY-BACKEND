package com.baljaguk.ybsy.domain.plan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Embeddable
@lombok.Builder(toBuilder = true)
public record PlanDetails(
        @NotBlank @Size(max = 30) String title,
        @NotNull @Pattern(regexp = "class|activity|project|custom") String type,
        @Min(1) @Max(12) Integer credits,
        @NotNull @Pattern(regexp = "low|normal|high|custom") String loadLevel,
        @NotNull @Min(1) @Max(16) Integer startWeek,
        @NotNull @Min(1) @Max(16) Integer endWeek,
        @NotNull @DecimalMin("0.5") @DecimalMax("60") @Column(precision = 4, scale = 1) BigDecimal hoursPerWeek,
        LocalDate startDate, LocalDate endDate,
        @NotNull @Pattern(regexp = "low|normal|high") String priority,
        Long categoryId, @NotNull Boolean noExam,
        @Pattern(regexp = "midterm|final") String examOnly,
        @Convert(converter = StudyDaysConverter.class) @Column(name = "study_days_mask")
        @Size(min = 1, max = 7) List<@NotNull @Min(0) @Max(6) Integer> studyDays
) {
    @JsonIgnore
    @AssertTrue(message = "시작 주차와 날짜는 종료보다 늦을 수 없습니다.")
    public boolean isPeriodValid() {
        return (startWeek == null || endWeek == null || startWeek <= endWeek)
                && (startDate == null || endDate == null || !startDate.isAfter(endDate));
    }

    @JsonIgnore
    @AssertTrue(message = "학점과 시험 설정은 수업 유형에 맞아야 합니다.")
    public boolean isClassValid() {
        boolean course = "class".equals(type);
        return course == (credits != null) && (!Boolean.TRUE.equals(noExam) || course)
                && (examOnly == null || (course && !Boolean.TRUE.equals(noExam)));
    }

    @JsonIgnore
    @AssertTrue(message = "주당 시간은 0.5 단위여야 합니다.")
    public boolean isTimeValid() {
        return hoursPerWeek == null || hoursPerWeek.remainder(new BigDecimal("0.5")).signum() == 0;
    }
}
