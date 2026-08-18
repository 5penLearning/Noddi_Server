package com._penLearning.Noddi.domain.user.storage;

import com._penLearning.Noddi.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalProfileImageStorageTest {

    @TempDir
    Path tempDirectory;

    private LocalProfileImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalProfileImageStorage(tempDirectory.toString(), 20);
        storage.initializeStorageDirectory();
    }

    @Test
    void storesLoadsAndDeletesPngByFileSignature() throws Exception {
        byte[] pngBytes = {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.txt",
                MediaType.TEXT_PLAIN_VALUE,
                pngBytes
        );

        StoredProfileImage stored = storage.store(image);
        ProfileImageResource loaded = storage.load(stored.key());

        assertThat(stored.key()).endsWith(".png");
        assertThat(loaded.mediaType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(loaded.resource().getContentAsByteArray()).containsExactly(pngBytes);

        storage.delete(stored.key());
        assertThatThrownBy(() -> storage.load(stored.key()))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    void rejectsUnsupportedFileContent() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                "not-an-image".getBytes()
        );

        assertThatThrownBy(() -> storage.store(image))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    void rejectsFileLargerThanConfiguredLimit() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[21]
        );

        assertThatThrownBy(() -> storage.store(image))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    void rejectsUnsafeStorageKey() {
        assertThatThrownBy(() -> storage.load("../secret.png"))
                .isInstanceOf(GeneralException.class);
    }
}
