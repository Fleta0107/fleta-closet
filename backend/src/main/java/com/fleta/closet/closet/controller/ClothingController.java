package com.fleta.closet.closet.controller;

import com.fleta.closet.closet.domain.Category;
import com.fleta.closet.closet.domain.ClothingRequest;
import com.fleta.closet.closet.domain.ClothingResponse;
import com.fleta.closet.closet.service.ClothingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api/closet/items")
@RequiredArgsConstructor
public class ClothingController {

    private final ClothingService clothingService;

    @GetMapping
    public ResponseEntity<List<ClothingResponse>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Category category) {
        return ResponseEntity.ok(clothingService.findAll(userId, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClothingResponse> get(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(clothingService.findById(userId, id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClothingResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestPart("data") ClothingRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(clothingService.create(userId, request, image));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClothingResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestPart("data") ClothingRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
        return ResponseEntity.ok(clothingService.update(userId, id, request, image));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) throws IOException {
        clothingService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) throws IOException {
        Resource image = clothingService.getImage(userId, id);

        String contentType = Files.probeContentType(image.getFile().toPath());
        MediaType mediaType = (contentType != null)
            ? MediaType.parseMediaType(contentType)
            : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFilename() + "\"")
            .contentType(mediaType)
            .body(image);
    }
}
