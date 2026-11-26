package com.baljaguk.ybsy.domain.plan.dto;

import com.baljaguk.ybsy.domain.plan.entity.Plan;
import com.baljaguk.ybsy.domain.plan.entity.PlanDetails;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.Instant;

public record PlanResponse(long id, long semesterId, @JsonUnwrapped PlanDetails details, Instant stoppedAt) {
    public static PlanResponse from(Plan plan) {
        return new PlanResponse(plan.getId(), plan.getSemesterId(), plan.getDetails(), plan.getStoppedAt());
    }
}
