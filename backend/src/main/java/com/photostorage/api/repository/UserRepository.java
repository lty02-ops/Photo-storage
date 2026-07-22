package com.photostorage.api.repository;

import com.photostorage.api.model.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByProviderAndProviderUserId(String provider, String providerUserId);

    boolean existsByEmail(String email);
}
