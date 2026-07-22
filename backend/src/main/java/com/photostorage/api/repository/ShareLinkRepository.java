package com.photostorage.api.repository;

import com.photostorage.api.model.ShareLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    Optional<ShareLink> findByToken(String token);
}
