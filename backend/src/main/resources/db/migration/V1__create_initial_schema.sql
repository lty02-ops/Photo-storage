CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    provider VARCHAR(255),
    provider_user_id VARCHAR(255),
    role VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE photos (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_provider VARCHAR(255) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    replication_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    replicated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_photos_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT photos_replication_status_check
        CHECK (
            replication_status IN (
                'NOT_REQUIRED',
                'PENDING',
                'COMPLETED',
                'FAILED'
            )
        )
);

CREATE TABLE share_links (
    id BIGSERIAL PRIMARY KEY,
    photo_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_share_links_photo
        FOREIGN KEY (photo_id) REFERENCES photos (id)
);

CREATE TABLE replication_jobs (
    id BIGSERIAL PRIMARY KEY,
    photo_id BIGINT NOT NULL UNIQUE,
    source_provider VARCHAR(255) NOT NULL,
    target_provider VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    attempts INTEGER NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_replication_jobs_photo
        FOREIGN KEY (photo_id) REFERENCES photos (id)
);

CREATE INDEX idx_photos_user_deleted
    ON photos (user_id, deleted_at);

CREATE INDEX idx_replication_jobs_ready
    ON replication_jobs (status, next_attempt_at, created_at);
