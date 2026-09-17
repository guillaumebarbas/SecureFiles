package com.securefiles.domain.file.port.out;

import com.securefiles.domain.file.model.StoredFile;
import java.util.List;
import java.util.Objects;

public record StoredFilePage(List<StoredFile> content, long totalElements) {

    public StoredFilePage {
        content = List.copyOf(Objects.requireNonNull(content, "content must not be null"));
        if (totalElements < content.size()) {
            throw new IllegalArgumentException("totalElements must include content");
        }
    }
}