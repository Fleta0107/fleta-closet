package com.fleta.closet.closet.service;

import com.fleta.closet.auth.domain.User;
import com.fleta.closet.auth.repository.UserRepository;
import com.fleta.closet.closet.domain.*;
import com.fleta.closet.closet.repository.ClothingRepository;
import com.fleta.closet.common.exception.AppException;
import com.fleta.closet.common.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ClothingService {

    private final ClothingRepository clothingRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional(rollbackFor = IOException.class)
    public ClothingResponse create(Long userId, ClothingRequest request, MultipartFile image) throws IOException {
        User user = userRepository.findById(userId)
            .orElseThrow(AppException::userNotFound);

        String imagePath = null;
        if (image != null && !image.isEmpty()) {
            imagePath = fileStorageService.store(userId, image);
        }

        try {
            ClothingItem item = ClothingItem.builder()
                .user(user)
                .category(request.category())
                .brand(request.brand())
                .color(request.color())
                .tags(request.tags() != null ? request.tags() : Set.of())
                .memo(request.memo())
                .imagePath(imagePath)
                .build();
            return ClothingResponse.from(clothingRepository.save(item));
        } catch (Exception e) {
            // DB 저장 실패 시 업로드된 파일 정리
            if (imagePath != null) {
                try { fileStorageService.delete(imagePath); } catch (IOException ignored) {}
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<ClothingResponse> findAll(Long userId, Category category) {
        List<ClothingItem> items = category != null
            ? clothingRepository.findByUserIdAndCategory(userId, category)
            : clothingRepository.findByUserId(userId);

        return items.stream().map(ClothingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ClothingResponse findById(Long userId, Long itemId) {
        return ClothingResponse.from(getItemForUser(userId, itemId));
    }

    @Transactional(rollbackFor = IOException.class)
    public ClothingResponse update(Long userId, Long itemId,
                                    ClothingRequest request, MultipartFile image) throws IOException {
        ClothingItem item = getItemForUser(userId, itemId);
        String oldImagePath = item.getImagePath();

        String newImagePath = null;
        if (image != null && !image.isEmpty()) {
            newImagePath = fileStorageService.store(userId, image);
        }

        try {
            item.update(request.category(), request.brand(), request.color(),
                        request.tags(), request.memo(), newImagePath);
            ClothingResponse response = ClothingResponse.from(clothingRepository.save(item));

            // 저장 성공 후 기존 이미지 삭제 (실패 시 무시 — DB는 이미 신규 경로로 갱신됨)
            if (newImagePath != null && oldImagePath != null) {
                try { fileStorageService.delete(oldImagePath); } catch (IOException ignored) {}
            }
            return response;
        } catch (Exception e) {
            // 저장 실패 시 신규 파일 정리
            if (newImagePath != null) {
                try { fileStorageService.delete(newImagePath); } catch (IOException ignored) {}
            }
            throw e;
        }
    }

    @Transactional(rollbackFor = IOException.class)
    public void delete(Long userId, Long itemId) throws IOException {
        ClothingItem item = getItemForUser(userId, itemId);
        if (item.getImagePath() != null) {
            fileStorageService.delete(item.getImagePath());
        }
        clothingRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public Resource getImage(Long userId, Long itemId) throws IOException {
        ClothingItem item = getItemForUser(userId, itemId);
        if (item.getImagePath() == null) {
            throw AppException.fileNotFound();
        }
        return fileStorageService.load(item.getImagePath());
    }

    // findByIdAndUserId: 타인 아이템 조회 시 소유자 정보 비노출 (404 반환)
    private ClothingItem getItemForUser(Long userId, Long itemId) {
        return clothingRepository.findByIdAndUserId(itemId, userId)
            .orElseThrow(AppException::clothingNotFound);
    }
}
