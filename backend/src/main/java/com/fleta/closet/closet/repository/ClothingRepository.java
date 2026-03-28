package com.fleta.closet.closet.repository;

import com.fleta.closet.closet.domain.Category;
import com.fleta.closet.closet.domain.ClothingItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClothingRepository extends JpaRepository<ClothingItem, Long> {
    List<ClothingItem> findByUserId(Long userId);
    List<ClothingItem> findByUserIdAndCategory(Long userId, Category category);
    Optional<ClothingItem> findByIdAndUserId(Long id, Long userId);  // 소유권 검증 단건 조회
}
