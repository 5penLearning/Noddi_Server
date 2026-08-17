package com._penLearning.Noddi.domain.user.storage;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record ProfileImageResource(Resource resource, MediaType mediaType) {
}
