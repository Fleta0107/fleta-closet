package com.fleta.closet.closet;

import com.fleta.closet.auth.domain.User;
import com.fleta.closet.auth.repository.UserRepository;
import com.fleta.closet.closet.domain.*;
import com.fleta.closet.closet.repository.ClothingRepository;
import com.fleta.closet.closet.service.ClothingService;
import com.fleta.closet.common.exception.AppException;
import com.fleta.closet.common.storage.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClothingServiceTest {

    @Mock ClothingRepository clothingRepository;
    @Mock UserRepository userRepository;
    @Mock FileStorageService fileStorageService;
    @InjectMocks ClothingService clothingService;

    // ─── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("의류 등록 성공 (이미지 없음)")
    void create_success() throws Exception {
        User user = User.builder().id(1L).email("u@t.com").password("pw").nickname("닉").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ClothingRequest request = new ClothingRequest(Category.TOPS, "Nike", "White", Set.of("Casual"), null);
        ClothingItem saved = ClothingItem.builder().id(10L).user(user).category(Category.TOPS).build();
        when(clothingRepository.save(any(ClothingItem.class))).thenReturn(saved);

        ClothingResponse response = clothingService.create(1L, request, null);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.category()).isEqualTo(Category.TOPS);
        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("의류 등록 성공 (이미지 포함) — imageUrl 생성 확인")
    void create_withImage_success() throws Exception {
        User user = User.builder().id(1L).email("u@t.com").password("pw").nickname("닉").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(fileStorageService.store(1L, image)).thenReturn("/uploads/1/abc.jpg");

        ClothingRequest request = new ClothingRequest(Category.TOPS, "Nike", "White", Set.of(), null);
        ClothingItem saved = ClothingItem.builder().id(10L).user(user).category(Category.TOPS)
                .imagePath("/uploads/1/abc.jpg").build();
        when(clothingRepository.save(any())).thenReturn(saved);

        ClothingResponse response = clothingService.create(1L, request, image);

        verify(fileStorageService).store(1L, image);
        assertThat(response.imageUrl()).isEqualTo("/api/closet/items/10/image");
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 의류 등록 → USER_NOT_FOUND 예외")
    void create_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        ClothingRequest request = new ClothingRequest(Category.TOPS, null, null, null, null);

        assertThatThrownBy(() -> clothingService.create(99L, request, null))
            .isInstanceOf(AppException.class)
            .extracting("code").isEqualTo("USER_NOT_FOUND");
    }

    // ─── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("카테고리 필터 없이 목록 조회")
    void findAll_noFilter() {
        User user = User.builder().id(1L).password("pw").build();
        ClothingItem item = ClothingItem.builder().id(1L).user(user).category(Category.TOPS).build();
        when(clothingRepository.findByUserId(1L)).thenReturn(List.of(item));

        List<ClothingResponse> result = clothingService.findAll(1L, null);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("카테고리 필터로 목록 조회")
    void findAll_withCategory() {
        User user = User.builder().id(1L).password("pw").build();
        ClothingItem item = ClothingItem.builder().id(1L).user(user).category(Category.TOPS).build();
        when(clothingRepository.findByUserIdAndCategory(1L, Category.TOPS)).thenReturn(List.of(item));

        List<ClothingResponse> result = clothingService.findAll(1L, Category.TOPS);

        assertThat(result).hasSize(1);
    }

    // ─── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("타인 소유 또는 존재하지 않는 의류 조회 → CLOTHING_NOT_FOUND (소유자 정보 비노출)")
    void findById_notOwnedOrNotFound_throws() {
        when(clothingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clothingService.findById(1L, 1L))
            .isInstanceOf(AppException.class)
            .extracting("code").isEqualTo("CLOTHING_NOT_FOUND");
    }

    @Test
    @DisplayName("존재하지 않는 의류 조회 → CLOTHING_NOT_FOUND 예외")
    void findById_notFound_throws() {
        when(clothingRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clothingService.findById(1L, 99L))
            .isInstanceOf(AppException.class)
            .extracting("code").isEqualTo("CLOTHING_NOT_FOUND");
    }

    // ─── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("이미지 교체 수정 — 신규 파일 저장 후 기존 파일 삭제")
    void update_withNewImage_replacesImage() throws Exception {
        User user = User.builder().id(1L).password("pw").build();
        ClothingItem item = ClothingItem.builder().id(1L).user(user).category(Category.TOPS)
                .imagePath("/old/img.jpg").build();
        when(clothingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));

        MultipartFile newImage = mock(MultipartFile.class);
        when(newImage.isEmpty()).thenReturn(false);
        when(fileStorageService.store(1L, newImage)).thenReturn("/new/img.jpg");
        when(clothingRepository.save(any())).thenReturn(item);

        ClothingRequest request = new ClothingRequest(Category.BOTTOMS, "Adidas", "Black", Set.of(), null);
        clothingService.update(1L, 1L, request, newImage);

        verify(fileStorageService).store(1L, newImage);
        verify(fileStorageService).delete("/old/img.jpg");
    }

    @Test
    @DisplayName("이미지 없이 수정 — 기존 이미지 유지, 파일 조작 없음")
    void update_withoutNewImage_preservesExisting() throws Exception {
        User user = User.builder().id(1L).password("pw").build();
        ClothingItem item = ClothingItem.builder().id(1L).user(user).category(Category.TOPS)
                .imagePath("/old/img.jpg").build();
        when(clothingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));
        when(clothingRepository.save(any())).thenReturn(item);

        ClothingRequest request = new ClothingRequest(Category.BOTTOMS, "Adidas", "Black", Set.of(), null);
        clothingService.update(1L, 1L, request, null);

        verifyNoInteractions(fileStorageService);
    }

    // ─── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("의류 삭제 성공 (이미지 있음)")
    void delete_success() throws Exception {
        User user = User.builder().id(1L).password("pw").build();
        ClothingItem item = ClothingItem.builder().id(1L).user(user).category(Category.TOPS)
                .imagePath("/img/1.jpg").build();
        when(clothingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));

        clothingService.delete(1L, 1L);

        verify(fileStorageService).delete("/img/1.jpg");
        verify(clothingRepository).delete(item);
    }

    @Test
    @DisplayName("이미지 없는 의류 삭제 — 파일 삭제 미호출")
    void delete_withoutImage_noFileDelete() throws Exception {
        User user = User.builder().id(1L).password("pw").build();
        ClothingItem item = ClothingItem.builder().id(1L).user(user).category(Category.TOPS).build();
        when(clothingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));

        clothingService.delete(1L, 1L);

        verifyNoInteractions(fileStorageService);
        verify(clothingRepository).delete(item);
    }
}
