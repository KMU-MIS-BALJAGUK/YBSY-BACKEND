package com.baljaguk.ybsy.domain.plan.entity;

import com.baljaguk.ybsy.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private long userId;
    private long semesterId;
    @Embedded
    private PlanDetails details;
    private Instant stoppedAt;

    public static Plan create(long userId, long semesterId, PlanDetails details) {
        Plan plan = new Plan();
        plan.userId = userId;
        plan.semesterId = semesterId;
        plan.details = details;
        return plan;
    }

    public void revise(PlanDetails details, Boolean stopped) {
        this.details = details;
        if (Boolean.TRUE.equals(stopped) && stoppedAt == null) stoppedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        if (Boolean.FALSE.equals(stopped)) stoppedAt = null;
    }
}
