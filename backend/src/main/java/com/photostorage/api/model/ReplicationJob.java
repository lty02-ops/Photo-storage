package com.photostorage.api.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "replication_jobs")
public class ReplicationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "photo_id", nullable = false, unique = true)
    private Photo photo;

    @Column(nullable = false)
    private String sourceProvider;

    @Column(nullable = false)
    private String targetProvider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReplicationJobStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column(length = 1000)
    private String lastError;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant completedAt;

    protected ReplicationJob() {
    }

    public ReplicationJob(
        Photo photo,
        String sourceProvider,
        String targetProvider
    ) {
        this.photo = photo;
        this.sourceProvider = sourceProvider;
        this.targetProvider = targetProvider;
        this.status = ReplicationJobStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Photo getPhoto() {
        return photo;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public String getTargetProvider() {
        return targetProvider;
    }

    public ReplicationJobStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void markProcessing() {
        this.status = ReplicationJobStatus.PROCESSING;
        this.attempts++;
    }

    public void markCompleted() {
        this.status = ReplicationJobStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.lastError = null;
    }

    public void markFailed(String errorMessage) {
        this.status = ReplicationJobStatus.FAILED;
        this.lastError = errorMessage;
        this.nextAttemptAt = Instant.now().plusSeconds(60);
    }
}
