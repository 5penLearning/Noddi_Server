package com._penLearning.Noddi.domain.user.storage;

import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ProfileImageStorageTest {

    private static final String BUCKET = "noddi-profile-images";
    private static final String PREFIX = "profile-images";
    private static final String IMAGE_KEY =
            "11111111-1111-1111-1111-111111111111.png";

    /*
     * 실제 PNG 전체 파일 대신 PNG 시그니처만 사용한다.
     * 현재 저장소는 파일 확장자나 Content-Type이 아닌 실제 바이트 시그니처를 검사한다.
     */
    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };

    @Mock
    private S3Client s3Client;

    private S3ProfileImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new S3ProfileImageStorage(
                s3Client,
                BUCKET,
                PREFIX,
                5 * 1024 * 1024
        );
    }

    @Test
    void uploadsPngToConfiguredBucketAndPrefix() {
        /*
         * MultipartFile을 검증한 뒤 UUID.png 키를 만들고,
         * 설정한 버킷의 profile-images 하위로 업로드하는지 확인한다.
         */
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "original-file.txt",
                MediaType.TEXT_PLAIN_VALUE,
                PNG_BYTES
        );

        StoredProfileImage result = storage.store(image);

        ArgumentCaptor<PutObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client).putObject(
                requestCaptor.capture(),
                any(RequestBody.class)
        );

        PutObjectRequest request = requestCaptor.getValue();

        assertThat(result.key())
                .matches("^[0-9a-f-]{36}\\.png$");
        assertThat(request.bucket()).isEqualTo(BUCKET);
        assertThat(request.key()).isEqualTo(PREFIX + "/" + result.key());
        assertThat(request.contentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(request.contentLength()).isEqualTo((long) PNG_BYTES.length);
    }

    @Test
    void loadsS3ObjectAsProfileImageResource() throws Exception {
        /*
         * DB에 저장된 UUID 키에 prefix를 붙여 S3에서 조회하고,
         * 응답 바이트와 확장자에 맞는 MediaType을 Resource로 반환하는지 확인한다.
         */
        ResponseBytes<GetObjectResponse> responseBytes =
                ResponseBytes.fromByteArray(
                        GetObjectResponse.builder()
                                .contentType(MediaType.IMAGE_PNG_VALUE)
                                .build(),
                        PNG_BYTES
                );

        when(s3Client.getObject(
                any(GetObjectRequest.class),
                org.mockito.ArgumentMatchers
                        .<ResponseTransformer<
                                GetObjectResponse,
                                ResponseBytes<GetObjectResponse>
                                >>any()
        )).thenReturn(responseBytes);

        ProfileImageResource result = storage.load(IMAGE_KEY);

        ArgumentCaptor<GetObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(GetObjectRequest.class);

        verify(s3Client).getObject(
                requestCaptor.capture(),
                org.mockito.ArgumentMatchers
                        .<ResponseTransformer<
                                GetObjectResponse,
                                ResponseBytes<GetObjectResponse>
                                >>any()
        );

        assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(requestCaptor.getValue().key()).isEqualTo(PREFIX + "/" + IMAGE_KEY);
        assertThat(result.mediaType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(result.resource().getContentAsByteArray()).containsExactly(PNG_BYTES);
    }

    @Test
    void deletesObjectUsingConfiguredPrefix() {
        // DB에는 UUID 키만 저장되지만 실제 삭제 요청에는 S3 prefix가 붙어야 한다.
        storage.delete(IMAGE_KEY);

        ArgumentCaptor<DeleteObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);

        verify(s3Client).deleteObject(requestCaptor.capture());

        assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(requestCaptor.getValue().key()).isEqualTo(PREFIX + "/" + IMAGE_KEY);
    }

    @Test
    void rejectsInvalidImageBeforeCallingS3() {
        /*
         * 파일 이름과 Content-Type만 PNG인 위장 파일은 거부하고,
         * 검증 실패 시 S3 요청을 전혀 보내지 않는지 확인한다.
         */
        MockMultipartFile invalidImage = new MockMultipartFile(
                "image",
                "fake.png",
                MediaType.IMAGE_PNG_VALUE,
                "not-an-image".getBytes()
        );

        GeneralException exception = catchThrowableOfType(
                GeneralException.class,
                () -> storage.store(invalidImage)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_PROFILE_IMAGE);
        verifyNoInteractions(s3Client);
    }

    @Test
    void convertsS3NotFoundResponseToProfileImageNotFound() {
        /*
         * DB에는 이미지 키가 있지만 실제 S3 Object가 없을 때,
         * 일반 저장소 장애가 아닌 프로필 이미지 없음 예외로 변환하는지 확인한다.
         */
        when(s3Client.getObject(
                any(GetObjectRequest.class),
                org.mockito.ArgumentMatchers
                        .<ResponseTransformer<
                                GetObjectResponse,
                                ResponseBytes<GetObjectResponse>
                                >>any()
        )).thenThrow(
                S3Exception.builder()
                        .statusCode(404)
                        .message("No such key")
                        .build()
        );

        GeneralException exception = catchThrowableOfType(
                GeneralException.class,
                () -> storage.load(IMAGE_KEY)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.PROFILE_IMAGE_NOT_FOUND);
    }

    @Test
    void convertsS3UploadFailureToStorageFailure() {
        /*
         * 이미지 자체는 정상이어도 S3가 500을 반환할 수 있다.
         * AWS SDK 예외가 컨트롤러까지 노출되지 않고 서비스 공통 저장 실패로 변환되는지 확인한다.
         */
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                PNG_BYTES
        );

        when(s3Client.putObject(
                any(PutObjectRequest.class),
                any(RequestBody.class)
        )).thenThrow(
                S3Exception.builder()
                        .statusCode(500)
                        .message("S3 internal error")
                        .build()
        );

        GeneralException exception = catchThrowableOfType(
                GeneralException.class,
                () -> storage.store(image)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.PROFILE_IMAGE_STORAGE_FAILED);
    }

    @Test
    void convertsNon404S3LoadFailureToStorageFailure() {
        // 404가 아닌 권한·네트워크·S3 서버 오류는 이미지 없음이 아니라 저장소 장애로 처리한다.
        when(s3Client.getObject(
                any(GetObjectRequest.class),
                org.mockito.ArgumentMatchers
                        .<ResponseTransformer<
                                GetObjectResponse,
                                ResponseBytes<GetObjectResponse>
                                >>any()
        )).thenThrow(
                S3Exception.builder()
                        .statusCode(500)
                        .message("S3 internal error")
                        .build()
        );

        GeneralException exception = catchThrowableOfType(
                GeneralException.class,
                () -> storage.load(IMAGE_KEY)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.PROFILE_IMAGE_STORAGE_FAILED);
    }

    @Test
    void ignoresDeleteWhenImageKeyIsNull() {
        // 프로필 이미지가 없는 사용자의 삭제 요청은 S3를 호출하지 않는 멱등 동작이어야 한다.
        storage.delete(null);

        verifyNoInteractions(s3Client);
    }
}
