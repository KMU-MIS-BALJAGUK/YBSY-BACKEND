package com.baljaguk.ybsy.domain.category.service;

import com.baljaguk.ybsy.global.error.BusinessException;
import com.baljaguk.ybsy.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryOwnershipService {
    private final JdbcClient jdbc;

    public void requireOwned(long userId, long categoryId) {
        jdbc.sql("select id from plan_categories where user_id = ? and id = ? for update")
                .params(userId, categoryId).query(Long.class).optional()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
