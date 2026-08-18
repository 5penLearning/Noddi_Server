package com._penLearning.Noddi.domain.user.storage;

import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.global.exception.GeneralException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(
        prefix = "profile-image",
        name = "storage-type",
        havingValue = "s3"
)
public class S3ProfileImageStorage implements ProfileImageStorage {

    /*
     * DB에 저장되는 키 형식을 제한한다.
     *
     * 사용자가 전달한 파일명을 S3 Key로 사용하지 않고
     * 서버가 생성한 UUID만 사용한다.
     */
    private static final Pattern SAFE_KEY_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-"
                    + "[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$"
    );

    private final S3Client s3Client;
    private final String bucket;
    private final String prefix;
    private final long maxFileSizeBytes;

    public S3ProfileImageStorage(
            S3Client s3Client,

            @Value("${profile-image.s3.bucket}")
            String bucket,

            @Value("${profile-image.s3.prefix:profile-images}")
            String prefix,

            @Value("${profile-image.max-file-size-bytes:5242880}")
            long maxFileSizeBytes
    ) {
        /*
         * storage-type=s3인데 버킷 환경변수가 비어 있으면
         * 요청 시점이 아닌 서버 시작 시점에 바로 실패시킨다.
         */
        Assert.hasText(
                bucket,
                "profile-image.s3.bucket must not be blank"
        );

        this.s3Client = s3Client;
        this.bucket = bucket;
        this.prefix = normalizePrefix(prefix);
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    public StoredProfileImage store(MultipartFile image) {
        validateUpload(image);

        byte[] bytes = readBytes(image);
        ImageType imageType = detectImageType(bytes);

        /*
         * 원본 파일명은 사용하지 않는다.
         * 동일 파일명 충돌과 경로 조작을 방지하기 위해 UUID를 사용한다.
         */
        String key = UUID.randomUUID() + imageType.extension();
        String objectKey = toObjectKey(key);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)

                // 브라우저에 반환할 때 사용할 실제 이미지 타입
                .contentType(imageType.mediaType().toString())

                // 업로드 크기를 명시해서 전송 크기를 예측 가능하게 한다.
                .contentLength((long) bytes.length)
                .build();

        try {
            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(bytes)
            );

            /*
             * DB에는 prefix가 없는 UUID 키만 저장한다.
             *
             * LocalProfileImageStorage와 동일한 키 형식을 유지하므로
             * 서비스와 DTO는 저장소 종류를 알 필요가 없다.
             */
            return new StoredProfileImage(key);

        } catch (SdkException exception) {
            throw new GeneralException(
                    UserErrorCode.PROFILE_IMAGE_STORAGE_FAILED
            );
        }
    }

    @Override
    public ProfileImageResource load(String key) {
        validateSafeKey(key);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(toObjectKey(key))
                .build();

        try {
            /*
             * 프로필 이미지는 최대 5MB로 제한되어 있으므로
             * 현재는 전체 바이트를 메모리로 읽어도 부담이 크지 않다.
             *
             * 더 큰 파일을 지원하게 되면 스트리밍 응답으로 변경해야 한다.
             */
            ResponseBytes<GetObjectResponse> response =
                    s3Client.getObject(
                            request,
                            ResponseTransformer.toBytes()
                    );

            ImageType imageType = ImageType.fromKey(key);

            return new ProfileImageResource(
                    new ByteArrayResource(response.asByteArray()),
                    imageType.mediaType()
            );

        } catch (S3Exception exception) {
            /*
             * DB에는 키가 있지만 S3 Object가 없는 경우다.
             * 저장소 장애와 구분해 404로 응답한다.
             */
            if (exception.statusCode() == 404) {
                throw new GeneralException(
                        UserErrorCode.PROFILE_IMAGE_NOT_FOUND
                );
            }

            throw new GeneralException(
                    UserErrorCode.PROFILE_IMAGE_STORAGE_FAILED
            );

        } catch (SdkException exception) {
            /*
             * 네트워크 오류, IAM 자격 증명 오류 등
             * S3 서비스 응답 이전에 발생한 SDK 오류다.
             */
            throw new GeneralException(
                    UserErrorCode.PROFILE_IMAGE_STORAGE_FAILED
            );
        }
    }

    @Override
    public void delete(String key) {
        if (key == null) {
            return;
        }

        validateSafeKey(key);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(toObjectKey(key))
                .build();

        try {
            /*
             * S3 DeleteObject는 대상 Object가 없어도 성공처럼 처리되므로
             * 별도의 존재 확인 요청은 필요하지 않다.
             */
            s3Client.deleteObject(request);

        } catch (SdkException exception) {
            throw new GeneralException(
                    UserErrorCode.PROFILE_IMAGE_STORAGE_FAILED
            );
        }
    }

    private void validateUpload(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new GeneralException(
                    UserErrorCode.INVALID_PROFILE_IMAGE
            );
        }

        if (image.getSize() > maxFileSizeBytes) {
            throw new GeneralException(
                    UserErrorCode.PROFILE_IMAGE_TOO_LARGE
            );
        }
    }

    private byte[] readBytes(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException exception) {
            throw new GeneralException(
                    UserErrorCode.PROFILE_IMAGE_STORAGE_FAILED
            );
        }
    }

    private String toObjectKey(String key) {
        validateSafeKey(key);

        if (prefix.isBlank()) {
            return key;
        }

        return prefix + "/" + key;
    }

    private void validateSafeKey(String key) {
        if (key == null || !SAFE_KEY_PATTERN.matcher(key).matches()) {
            throw new GeneralException(
                    UserErrorCode.INVALID_PROFILE_IMAGE
            );
        }
    }

    private String normalizePrefix(String rawPrefix) {
        if (rawPrefix == null) {
            return "";
        }

        String normalized = rawPrefix.strip();

        /*
         * 환경변수에 /profile-images/처럼 입력해도
         * 최종 Object Key가 /profile-images//uuid가 되지 않게 한다.
         */
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - 1
            );
        }

        if (normalized.contains("..")) {
            throw new IllegalArgumentException(
                    "profile-image.s3.prefix must not contain '..'"
            );
        }

        return normalized;
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

        throw new GeneralException(
                UserErrorCode.INVALID_PROFILE_IMAGE
        );
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && unsigned(bytes[0]) == 0xFF
                && unsigned(bytes[1]) == 0xD8
                && unsigned(bytes[2]) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        int[] signature = {
                0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        if (bytes.length < signature.length) {
            return false;
        }

        for (int index = 0; index < signature.length; index++) {
            if (unsigned(bytes[index]) != signature[index]) {
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

    private boolean asciiEquals(
            byte[] bytes,
            int offset,
            String expected
    ) {
        for (int index = 0; index < expected.length(); index++) {
            if (bytes[offset + index]
                    != (byte) expected.charAt(index)) {
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
        WEBP(
                ".webp",
                MediaType.parseMediaType("image/webp")
        );

        private final String extension;
        private final MediaType mediaType;

        ImageType(
                String extension,
                MediaType mediaType
        ) {
            this.extension = extension;
            this.mediaType = mediaType;
        }

        private String extension() {
            return extension;
        }

        private MediaType mediaType() {
            return mediaType;
        }

        private static ImageType fromKey(String key) {
            for (ImageType type : values()) {
                if (key.endsWith(type.extension)) {
                    return type;
                }
            }

            throw new GeneralException(
                    UserErrorCode.INVALID_PROFILE_IMAGE
            );
        }
    }
}