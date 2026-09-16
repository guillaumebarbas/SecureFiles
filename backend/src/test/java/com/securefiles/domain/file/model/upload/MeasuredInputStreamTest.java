package com.securefiles.domain.file.model.upload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeasuredInputStreamTest {

    private static final String SHA_256 = "99d5e9e0dc50e56ad7c9ecd0a0feea56fcb81d0ba7a27b7fa1e971c5dadd452b";

    @Test
    void readAllBytes_shouldMeasureContentAndCalculateHash_whenStreamReachesEnd() throws IOException {
        MeasuredInputStream content = new MeasuredInputStream(
                new ByteArrayInputStream("safe content".getBytes(StandardCharsets.UTF_8)));

        byte[] bytes = content.readAllBytes();

        assertThat(bytes).isEqualTo("safe content".getBytes(StandardCharsets.UTF_8));
        assertThat(content.hasReachedEnd()).isTrue();
        assertThat(content.measuredSizeBytes()).isEqualTo(12L);
        assertThat(content.sha256()).isEqualTo(SHA_256);
    }

    @Test
    void sha256_shouldReject_whenStreamHasNotReachedEnd() throws IOException {
        MeasuredInputStream content = new MeasuredInputStream(
                new ByteArrayInputStream("safe content".getBytes(StandardCharsets.UTF_8)));
        content.read();

        assertThatThrownBy(content::sha256)
                .isInstanceOf(UploadException.class)
                .extracting(exception -> ((UploadException) exception).code())
                .isEqualTo("INCOMPLETE_STREAM");
    }

    @Test
    void read_shouldRejectWhenLimitIsExceeded_duringStreaming() {
        MeasuredInputStream content = new MeasuredInputStream(
                new ByteArrayInputStream("safe content".getBytes(StandardCharsets.UTF_8)),
                5L);

        assertThatThrownBy(content::readAllBytes)
                .isInstanceOf(UploadException.class)
                .extracting(exception -> ((UploadException) exception).code())
                .isEqualTo("MAX_SIZE_EXCEEDED");
    }
}