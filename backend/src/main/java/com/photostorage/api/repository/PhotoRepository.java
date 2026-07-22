package com.photostorage.api.repository;

import com.photostorage.api.model.AppUser;
import com.photostorage.api.model.Photo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(AppUser user);

    Optional<Photo> findByIdAndUserAndDeletedAtIsNull(Long id, AppUser user);

    long countByDeletedAtIsNull();
}
