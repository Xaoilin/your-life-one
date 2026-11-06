package com.yourlifeone.access.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountIdentityRepository extends JpaRepository<AccountIdentity, UUID> {
    Optional<AccountIdentity> findByProviderAndSubjectAndEnabledTrue(String provider, String subject);
}
