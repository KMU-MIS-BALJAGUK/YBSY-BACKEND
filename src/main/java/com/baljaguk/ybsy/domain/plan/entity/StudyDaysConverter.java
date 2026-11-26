package com.baljaguk.ybsy.domain.plan.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import java.util.stream.IntStream;

@Converter
public class StudyDaysConverter implements AttributeConverter<List<Integer>, Integer> {
    @Override
    public Integer convertToDatabaseColumn(List<Integer> days) {
        if (days == null) return null;
        int mask = 0;
        for (int day : days) mask |= 1 << day;
        return mask;
    }

    @Override
    public List<Integer> convertToEntityAttribute(Integer mask) {
        if (mask == null) return null;
        return IntStream.range(0, 7).filter(day -> (mask & (1 << day)) != 0).boxed().toList();
    }
}
