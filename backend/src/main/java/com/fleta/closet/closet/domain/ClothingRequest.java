package com.fleta.closet.closet.domain;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record ClothingRequest(
        @NotNull(message = "카테고리를 선택해주세요")
        Category category,
        String brand,
        String color,
        Set<String> tags,
        String memo
) {}
