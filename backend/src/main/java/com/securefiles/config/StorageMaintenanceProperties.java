package com.securefiles.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "securefiles.storage.maintenance")
public record StorageMaintenanceProperties(int deletionRecoveryBatchSize) {

    public StorageMaintenanceProperties {
        if (deletionRecoveryBatchSize < 1) {
            throw new IllegalArgumentException("deletionRecoveryBatchSize must be positive");
        }
    }
}