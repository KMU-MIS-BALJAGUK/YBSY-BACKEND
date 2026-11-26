package com.baljaguk.ybsy.domain.plan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PlanBulkCreateRequest(@NotEmpty List<@NotNull @Valid PlanCreateRequest> plans) {}
