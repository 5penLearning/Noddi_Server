package com._penLearning.Noddi.domain.user.storage;

import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.global.exception.GeneralException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class LocalProfileImageStorage implements ProfileImageStorage {

    private static final Pattern SAFE_KEY_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$"
    );

    private final Path storageRoot;
    private final long maxFileSizeBytes;

    public LocalProfileImageStorage(
            @Value("${profile-image.storage-path:./uploads/profile-images}") String storagePath,
            @Value("${profile-image.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @PostConstruct
    void initializeStorageDirectory() {
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException exception) {
            throw new GeneralException(UserErrorCode.PROFILE_IMAGE_STORAGE_FAILED);
        }
    }

    @Override
    public StoredProfileImage store(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new GeneralException(UserErrorCode.INVALID_PROFILE_IMAGE);
        }
        if (image.getSize() > maxFileSizeBytes) {
            throw new GeneralException(UserErrorCode.PROFILE_IMAGE_TOO_LARGE);
        }

        try {
            byte[] bytes = image.getBytes();
            ImageType imageType = detectImageType(bytes);
            String key = UUID.randomUUID() + imageType.extension;
            Path target = resolveSafeKey(key);

            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            return new StoredProfileImage(key);
        } catch (GeneralException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new GeneralException(UserErrorCode.PROFILE_IMAGE_STORAGE_FAILED);
        }
    }

    @Override
    public ProfileImageResource load(String key) {
        Path imagePath = resolveSafeKey(key);
        if (!Files.isRegularFile(imagePath)) {
            throw new GeneralException(UserErrorCode.PROFILE_IMAGE_NOT_FOUND);
        }

        ImageType imageType = ImageType.fromExtension(key);
        return new ProfileImageResource(
                new FileSystemResource(imagePath),
                imageType.mediaType
        );
    }

    @Override
    public void delete(String key) {
        if (key == null) {
            return;
        }

        try {
            Files.deleteIfExists(resolveSafeKey(key));
        } catch (IOException exception) {
            throw new GeneralException(UserErrorCode.PROFILE_IMAGE_STORAGE_FAILED);
        }
    }

    private Path resolveSafeKey(String key) {
        if (key == null || !SAFE_KEY_PATTERN.matcher(key).matches()) {
            throw new GeneralException(UserErrorCode.INVALID_PROFILE_IMAGE);
        }

        Path resolved = storageRoot.resolve(key).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new GeneralException(UserErrorCode.INVALID_PROFILE_IMAGE);
        }
        return resolved;
    }

    private ImageType detectImageType(byte[] bytes) {
        if (isJpeg(bytes)) {
            return ImageType.JPEG;
        }
        if (isPng(bytes)) {
            return ImageType.PNG;
        }
        if (isWebp(bytes)) {
            return ImageType.WEBP;
        }
        throw new GeneralException(UserErrorCode.INVALID_PROFILE_IMAGE);
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && unsigned(bytes[0]) == 0xFF
                && unsigned(bytes[1]) == 0xD8
                && unsigned(bytes[2]) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (unsigned(bytes[i]) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && asciiEquals(bytes, 0, "RIFF")
                && asciiEquals(bytes, 8, "WEBP");
    }

    private boolean asciiEquals(byte[] bytes, int offset, String expected) {
        for (int i = 0; i < expected.length(); i++) {
            if (bytes[offset + i] != (byte) expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private enum ImageType {
        JPEG(".jpg", MediaType.IMAGE_JPEG),
        PNG(".png", MediaType.IMAGE_PNG),
        WEBP(".webp", MediaType.parseMediaType("image/webp"));

        private final String extension;
        private final MediaType mediaType;

        ImageType(String extension, MediaType mediaType) {
            this.extension = extension;
            this.mediaType = mediaType;
        }

        private static ImageType fromExtension(String key) {
            for (ImageType type : values()) {
                if (key.endsWith(type.extension)) {
                    return type;
                }
            }
            throw new GeneralException(UserErrorCode.INVALID_PROFILE_IMAGE);
        }
    }
}
