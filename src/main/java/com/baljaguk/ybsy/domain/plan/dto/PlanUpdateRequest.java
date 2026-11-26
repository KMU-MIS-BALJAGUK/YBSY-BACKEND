package com.baljaguk.ybsy.domain.plan.dto;

import com.baljaguk.ybsy.domain.plan.entity.PlanDetails;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PlanUpdateRequest {
    // Jackson calls a setter only for a supplied property, including explicit null.
    private final List<Consumer<PlanDetails.PlanDetailsBuilder>> changes = new ArrayList<>();
    private Boolean stopped;

    public void setTitle(String value) {
        changes.add(builder -> builder.title(value));
    }

    public void setType(String value) {
        changes.add(builder -> builder.type(value));
    }

    public void setCredits(Integer value) {
        changes.add(builder -> builder.credits(value));
    }

    public void setLoadLevel(String value) {
        changes.add(builder -> builder.loadLevel(value));
    }

    public void setStartWeek(Integer value) {
        changes.add(builder -> builder.startWeek(value));
    }

    public void setEndWeek(Integer value) {
        changes.add(builder -> builder.endWeek(value));
    }

    public void setHoursPerWeek(BigDecimal value) {
        changes.add(builder -> builder.hoursPerWeek(value));
    }

    public void setStartDate(LocalDate value) {
        changes.add(builder -> builder.startDate(value));
    }

    public void setEndDate(LocalDate value) {
        changes.add(builder -> builder.endDate(value));
    }

    public void setPriority(String value) {
        changes.add(builder -> builder.priority(value));
    }

    public void setCategoryId(Long value) {
        changes.add(builder -> builder.categoryId(value));
    }

    public void setNoExam(Boolean value) {
        changes.add(builder -> builder.noExam(value));
    }

    public void setExamOnly(String value) {
        changes.add(builder -> builder.examOnly(value));
    }

    public void setStudyDays(List<Integer> value) {
        changes.add(builder -> builder.studyDays(value));
    }

    @JsonSetter(nulls = Nulls.FAIL)
    @Schema(description = "true이면 중단, false이면 재개, 생략하면 현재 상태 유지")
    public void setStopped(Boolean stopped) {
        this.stopped = stopped;
    }

    public Boolean stopped() {
        return stopped;
    }

    public PlanDetails applyTo(PlanDetails previous) {
        var builder = previous.toBuilder();
        changes.forEach(change -> change.accept(builder));
        return builder.build();
    }
}
