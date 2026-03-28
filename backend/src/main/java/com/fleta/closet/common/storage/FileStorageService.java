package com.fleta.closet.common.storage;

import com.fleta.closet.common.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    private final Path basePath;

    public FileStorageService(@Value("${app.file-storage.base-path}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    /**
     * 이미지 파일을 {basePath}/{userId}/{uuid}.ext 경로에 저장 후 절대 경로 반환
     */
    public String store(Long userId, MultipartFile file) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw AppException.invalidFileType();
        }

        Path userDir = basePath.resolve(userId.toString());
        Files.createDirectories(userDir);

        String filename = UUID.randomUUID() + extension;
        Path   target   = userDir.resolve(filename);

        file.transferTo(target);
        return target.toAbsolutePath().toString();
    }

    /**
     * 저장된 경로에서 Resource 로드
     */
    public Resource load(String absolutePath) throws MalformedURLException {
        Path resolved = Path.of(absolutePath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw AppException.invalidFilePath();
        }

        Resource resource = new UrlResource(resolved.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw AppException.fileNotFound();
        }
        return resource;
    }

    /**
     * 저장된 파일 삭제 (의류 삭제 시 호출)
     */
    public void delete(String absolutePath) throws IOException {
        Path resolved = Path.of(absolutePath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw AppException.invalidFilePath();
        }
        Files.deleteIfExists(resolved);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
