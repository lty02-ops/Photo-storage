package com.photostorage.api.repository;

import com.photostorage.api.model.ReplicationJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReplicationJobRepository
    extends JpaRepository<ReplicationJob, Long> {

    @Query(
        value = """
            SELECT *
            FROM replication_jobs
            WHERE status IN ('PENDING', 'FAILED')
              AND next_attempt_at <= :currentTime
            ORDER BY created_at
            LIMIT 10
            FOR UPDATE SKIP LOCKED
            """,
        nativeQuery = true
    )
    List<ReplicationJob> findReadyJobs(
        @Param("currentTime") Instant currentTime
    );

    Optional<ReplicationJob> findByPhoto_Id(Long photoId);
}