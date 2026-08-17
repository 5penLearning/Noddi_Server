package com._penLearning.Noddi.domain.user.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageStorage {

    StoredProfileImage store(MultipartFile image);

    ProfileImageResource load(String key);

    void delete(String key);
}
