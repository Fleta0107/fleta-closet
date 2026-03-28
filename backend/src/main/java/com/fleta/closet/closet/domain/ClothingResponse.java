package com.fleta.closet.closet.domain;

import java.time.LocalDateTime;
import java.util.Set;

public record ClothingResponse(
        Long id,
        Category category,
        String brand,
        String color,
        Set<String> tags,
        String memo,
        String imageUrl,
        LocalDateTime createdAt
) {
    private static final String IMAGE_URL_TEMPLATE = "/api/closet/items/%d/image";

    public static ClothingResponse from(ClothingItem item) {
        String imageUrl = item.getImagePath() != null
                ? IMAGE_URL_TEMPLATE.formatted(item.getId())
                : null;
        return new ClothingResponse(
                item.getId(),
                item.getCategory(),
                item.getBrand(),
                item.getColor(),
                item.getTags() != null ? Set.copyOf(item.getTags()) : Set.of(),  // 방어 복사: 영속성 컬렉션 참조 직접 노출 방지
                item.getMemo(),
                imageUrl,
                item.getCreatedAt()
        );
    }
}
