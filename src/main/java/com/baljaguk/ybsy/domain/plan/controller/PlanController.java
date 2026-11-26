package com.baljaguk.ybsy.domain.plan.controller;

import com.baljaguk.ybsy.domain.plan.dto.*;
import com.baljaguk.ybsy.domain.plan.service.PlanService;
import com.baljaguk.ybsy.global.error.BusinessException;
import com.baljaguk.ybsy.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "계획")
@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanController {
    private final PlanService plans;

    @GetMapping
    @Operation(summary = "현재 학기 계획 목록 조회")
    public List<PlanResponse> list(Principal principal) {
        return plans.list(userId(principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "현재 학기 계획 한 건 조회")
    public PlanResponse get(Principal principal, @PathVariable long id) {
        return plans.get(userId(principal), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "현재 학기에 계획 등록")
    public PlanResponse create(Principal principal, @Valid @RequestBody PlanCreateRequest request) {
        return plans.create(userId(principal), List.of(request)).getFirst();
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "시간표 계획 일괄 등록")
    public List<PlanResponse> bulk(Principal principal, @Valid @RequestBody PlanBulkCreateRequest request) {
        return plans.create(userId(principal), request.plans());
    }

    @PatchMapping("/{id}")
    @Operation(summary = "계획 부분 수정 및 중단·재개")
    public PlanResponse update(Principal principal, @PathVariable long id, @Valid @RequestBody PlanUpdateRequest request) {
        return plans.update(userId(principal), id, request);
    }

    private long userId(Principal principal) {
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
