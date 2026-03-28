package com.fleta.closet.closet;

import com.fleta.closet.closet.domain.Category;
import com.fleta.closet.closet.domain.ClothingItem;
import com.fleta.closet.closet.domain.ClothingRequest;
import com.fleta.closet.closet.domain.ClothingResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClothingDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("ClothingRequest — category null 시 검증 실패")
    void request_nullCategory_fails() {
        ClothingRequest request = new ClothingRequest(null, "Nike", "White", Set.of("casual"), "메모");
        Set<ConstraintViolation<ClothingRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("카테고리를 선택해주세요");
    }

    @Test
    @DisplayName("ClothingRequest — 정상 입력 시 검증 통과")
    void request_validInput_passes() {
        ClothingRequest request = new ClothingRequest(Category.TOPS, "Nike", "White", Set.of("casual"), "메모");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("ClothingResponse.from() — ClothingItem에서 변환, imageUrl 생성")
    void response_from_withImagePath() {
        ClothingItem item = ClothingItem.builder()
                .category(Category.TOPS)
                .brand("Nike")
                .color("White")
                .memo("메모")
                .build();
        // id 설정 (JPA @GeneratedValue 대신)
        org.springframework.test.util.ReflectionTestUtils.setField(item, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(item, "imagePath", "/storage/1/abc.jpg");

        ClothingResponse response = ClothingResponse.from(item);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.category()).isEqualTo(Category.TOPS);
        assertThat(response.brand()).isEqualTo("Nike");
        assertThat(response.color()).isEqualTo("White");
        assertThat(response.memo()).isEqualTo("메모");
        assertThat(response.imageUrl()).isEqualTo("/api/closet/items/1/image");
    }

    @Test
    @DisplayName("ClothingResponse.from() — imagePath null 시 imageUrl null")
    void response_from_withoutImagePath() {
        ClothingItem item = ClothingItem.builder()
                .category(Category.BOTTOMS)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(item, "id", 2L);

        ClothingResponse response = ClothingResponse.from(item);

        assertThat(response.imageUrl()).isNull();
    }
}
