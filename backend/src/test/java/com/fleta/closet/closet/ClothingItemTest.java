package com.fleta.closet.closet;

import com.fleta.closet.closet.domain.Category;
import com.fleta.closet.closet.domain.ClothingItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClothingItemTest {

    @Test
    @DisplayName("builder로 ClothingItem 생성")
    void builder_createsItem() {
        ClothingItem item = ClothingItem.builder()
                .category(Category.TOPS)
                .brand("Nike")
                .color("White")
                .memo("좋아하는 티셔츠")
                .build();

        assertThat(item.getCategory()).isEqualTo(Category.TOPS);
        assertThat(item.getBrand()).isEqualTo("Nike");
        assertThat(item.getColor()).isEqualTo("White");
        assertThat(item.getMemo()).isEqualTo("좋아하는 티셔츠");
        assertThat(item.getTags()).isEmpty();  // @Builder.Default → 빈 Set
    }

    @Test
    @DisplayName("update() — 카테고리/브랜드/컬러/태그/메모 수정")
    void update_modifiesFields() {
        ClothingItem item = ClothingItem.builder()
                .category(Category.TOPS)
                .brand("Old Brand")
                .color("Black")
                .build();

        item.update(Category.BOTTOMS, "New Brand", "Blue", Set.of("casual", "summer"), "여름용", null);

        assertThat(item.getCategory()).isEqualTo(Category.BOTTOMS);
        assertThat(item.getBrand()).isEqualTo("New Brand");
        assertThat(item.getColor()).isEqualTo("Blue");
        assertThat(item.getTags()).containsExactlyInAnyOrder("casual", "summer");
        assertThat(item.getMemo()).isEqualTo("여름용");
        assertThat(item.getImagePath()).isNull();  // imagePath null → 기존값 유지
    }

    @Test
    @DisplayName("update() — imagePath 전달 시 업데이트됨")
    void update_updatesImagePath() {
        ClothingItem item = ClothingItem.builder()
                .category(Category.SHOES)
                .build();

        item.update(Category.SHOES, null, null, null, null, "/uploads/1/abc.jpg");

        assertThat(item.getImagePath()).isEqualTo("/uploads/1/abc.jpg");
    }

    @Test
    @DisplayName("tags는 Set — 중복 허용 안 함")
    void tags_areSet_noDuplicates() {
        ClothingItem item = ClothingItem.builder()
                .category(Category.ACCESSORIES)
                .build();

        // HashSet 사용: Set.of()는 중복 원소 시 IllegalArgumentException
        Set<String> tagsWithDuplicate = new HashSet<>(Set.of("casual", "summer"));
        tagsWithDuplicate.add("casual");  // 중복 추가 시도

        item.update(Category.ACCESSORIES, null, null, tagsWithDuplicate, null, null);

        assertThat(item.getTags()).hasSize(2);  // Set이므로 "casual" 하나만 저장
        assertThat(item.getTags()).containsExactlyInAnyOrder("casual", "summer");
    }
}
