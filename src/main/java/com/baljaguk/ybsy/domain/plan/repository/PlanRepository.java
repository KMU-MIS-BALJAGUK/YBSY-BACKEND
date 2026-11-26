package com.baljaguk.ybsy.domain.plan.repository;

import com.baljaguk.ybsy.domain.plan.entity.Plan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByUserIdAndSemesterIdOrderByDetailsStartWeekAscIdAsc(long userId, long semesterId);
    Optional<Plan> findByIdAndUserIdAndSemesterId(long id, long userId, long semesterId);
    long countByUserIdAndSemesterId(long userId, long semesterId);
}
