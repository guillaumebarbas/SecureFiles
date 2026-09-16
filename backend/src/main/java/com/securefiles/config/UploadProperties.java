package com.securefiles.config;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "securefiles.upload")
public record UploadProperties(DataSize maximumSize) {

	private static final long MAXIMUM_SAFE_JAVASCRIPT_INTEGER = 9_007_199_254_740_991L;

	public UploadProperties {
		Objects.requireNonNull(maximumSize, "maximumSize must not be null");
		long maximumSizeBytes = maximumSize.toBytes();
		if (maximumSizeBytes <= 0 || maximumSizeBytes > MAXIMUM_SAFE_JAVASCRIPT_INTEGER) {
			throw new IllegalArgumentException("maximumSize must be a positive JavaScript-safe byte value");
		}
	}
}
