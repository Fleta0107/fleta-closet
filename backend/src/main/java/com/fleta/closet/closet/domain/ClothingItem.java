package com.fleta.closet.closet.domain;

import com.fleta.closet.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "clothing_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class ClothingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    private String brand;
    private String color;
    private String imagePath;
    private String memo;

    // @ElementCollection: clothing_item_tags 별도 테이블로 관리
    // Set 사용: DB 복합 PK (clothing_item_id, tag) — List는 중복 허용으로 제약 위반
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "clothing_item_tags",
            joinColumns = @JoinColumn(name = "clothing_item_id")
    )
    @Column(name = "tag", length = 100)
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void update(Category category, String brand, String color,
                       Set<String> tags, String memo, String imagePath) {
        this.category = category;
        this.brand = brand;
        this.color = color;
        this.memo = memo;
        this.tags.clear();
        this.tags.addAll(tags != null ? tags : Set.of());
        if (imagePath != null) {
            this.imagePath = imagePath;
        }
    }
}
