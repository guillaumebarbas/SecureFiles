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

    @Column(name = "reserved_bytes", nullable = false)
    private long reservedBytes;

    protected StoredFileQuotaEntity() {
    }

    public StoredFileQuotaEntity(String ownerId, long quotaBytes, long usedBytes) {
        this(ownerId, quotaBytes, usedBytes, 0L);
    }

    public StoredFileQuotaEntity(String ownerId, long quotaBytes, long usedBytes, long reservedBytes) {
        this.ownerId = ownerId;
        this.quotaBytes = quotaBytes;
        this.usedBytes = usedBytes;
        this.reservedBytes = reservedBytes;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public long getQuotaBytes() {
        return quotaBytes;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public long getReservedBytes() {
        return reservedBytes;
    }
}