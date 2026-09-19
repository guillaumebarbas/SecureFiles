package com.securefiles.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stored_file_quota")
public class StoredFileQuotaEntity {

    @Id
    @Column(name = "owner_id", nullable = false, length = 255)
    private String ownerId;

    @Column(name = "quota_bytes", nullable = false)
    private long quotaBytes;

    @Column(name = "used_bytes", nullable = false)
    private long usedBytes;

    protected StoredFileQuotaEntity() {
    }

    public StoredFileQuotaEntity(String ownerId, long quotaBytes, long usedBytes) {
        this.ownerId = ownerId;
        this.quotaBytes = quotaBytes;
        this.usedBytes = usedBytes;
    }
}