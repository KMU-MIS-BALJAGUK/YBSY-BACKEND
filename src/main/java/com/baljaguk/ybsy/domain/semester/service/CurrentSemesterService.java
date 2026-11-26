package com.baljaguk.ybsy.domain.semester.service;

import com.baljaguk.ybsy.global.error.BusinessException;
import com.baljaguk.ybsy.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentSemesterService {
    private static final String CURRENT =
            "select current_semester_id from profiles where user_id = ? and current_semester_id is not null";
    private final JdbcClient jdbc;

    public long currentId(long userId) {
        return lookup(CURRENT, userId);
    }

    // Lock the same profile even when the semester has no plans yet.
    public long lockCurrentId(long userId) {
        return lookup(CURRENT + " for update", userId);
    }

    private long lookup(String sql, long userId) {
        return jdbc.sql(sql).param(userId).query(Long.class).optional()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "현재 학기가 없습니다."));
    }
}
