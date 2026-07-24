package com.photostorage.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "photos")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private String storageProvider;

    @Column(nullable = false)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReplicationStatus replicationStatus;

    private Instant replicatedAt;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant deletedAt;

    protected Photo() {
    }

    public Photo(
            AppUser user,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String storageProvider,
            String objectKey) {
        this.user = user;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageProvider = storageProvider;
        this.objectKey = objectKey;
        this.replicationStatus = ReplicationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void markReplicationNotRequired() {
        this.replicationStatus = ReplicationStatus.NOT_REQUIRED;
        this.replicatedAt = null;
    }

    public void markReplicationCompleted() {
        this.replicationStatus = ReplicationStatus.COMPLETED;
        this.replicatedAt = Instant.now();
    }

    public void markReplicationFailed() {
        this.replicationStatus = ReplicationStatus.FAILED;
        this.replicatedAt = null;
    }

    public void markDeleted() {
        this.deletedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public ReplicationStatus getReplicationStatus() {
        return replicationStatus;
    }

    public Instant getReplicatedAt() {
        return replicatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
