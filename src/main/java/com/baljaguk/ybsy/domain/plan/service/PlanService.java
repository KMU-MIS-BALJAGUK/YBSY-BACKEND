package com.baljaguk.ybsy.domain.plan.service;

import com.baljaguk.ybsy.domain.category.service.CategoryOwnershipService;
import com.baljaguk.ybsy.domain.plan.dto.*;
import com.baljaguk.ybsy.domain.plan.entity.Plan;
import com.baljaguk.ybsy.domain.plan.entity.PlanDetails;
import com.baljaguk.ybsy.domain.plan.repository.PlanRepository;
import com.baljaguk.ybsy.domain.semester.service.CurrentSemesterService;
import com.baljaguk.ybsy.global.error.BusinessException;
import com.baljaguk.ybsy.global.error.ErrorCode;
import jakarta.validation.Validator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {
    private static final int SEMESTER_LIMIT = 30;
    private final PlanRepository plans;
    private final CurrentSemesterService semesters;
    private final CategoryOwnershipService categories;
    private final Validator validator;

    public List<PlanResponse> list(long userId) {
        return plans.findByUserIdAndSemesterIdOrderByDetailsStartWeekAscIdAsc(userId, semesters.currentId(userId))
                .stream().map(PlanResponse::from).toList();
    }

    public PlanResponse get(long userId, long id) {
        return PlanResponse.from(find(id, userId, semesters.currentId(userId)));
    }

    @Transactional
    public List<PlanResponse> create(long userId, List<PlanCreateRequest> requests) {
        long semesterId = semesters.lockCurrentId(userId);
        if (plans.countByUserIdAndSemesterId(userId, semesterId) + requests.size() > SEMESTER_LIMIT) {
            throw new BusinessException(ErrorCode.PLAN_LIMIT_EXCEEDED);
        }
        return requests.stream().map(request -> {
            PlanDetails details = validatedDetails(userId, request.details());
            return PlanResponse.from(plans.save(Plan.create(userId, semesterId, details)));
        }).toList();
    }

    @Transactional
    public PlanResponse update(long userId, long id, PlanUpdateRequest request) {
        Plan plan = find(id, userId, semesters.lockCurrentId(userId));
        PlanDetails details = validatedDetails(userId, request.applyTo(plan.getDetails()));
        plan.revise(details, request.stopped());
        return PlanResponse.from(plan);
    }

    private Plan find(long id, long userId, long semesterId) {
        return plans.findByIdAndUserIdAndSemesterId(id, userId, semesterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private PlanDetails validatedDetails(long userId, PlanDetails details) {
        var errors = validator.validate(details);
        if (!errors.isEmpty()) {
            var error = errors.iterator().next();
            throw new BusinessException(ErrorCode.INVALID_INPUT, error.getPropertyPath() + " " + error.getMessage());
        }
        if (details.categoryId() != null) categories.requireOwned(userId, details.categoryId());
        if (details.studyDays() == null) return details;
        return details.toBuilder().studyDays(details.studyDays().stream().distinct().sorted().toList()).build();
    }
}
