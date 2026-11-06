package com.yourlifeone.calendar.infrastructure.persistence.repository;

import com.yourlifeone.calendar.domain.AccountProvider;
import com.yourlifeone.calendar.infrastructure.persistence.entity.AccountEntity;
import com.yourlifeone.calendar.infrastructure.persistence.entity.UserScopedId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, UserScopedId> {
    List<AccountEntity> findByUserIdOrderByCreatedAtAscIdAsc(UUID userId);

    Optional<AccountEntity> findByUserIdAndId(UUID userId, String id);

    Optional<AccountEntity> findByUserIdAndProviderAndEmail(UUID userId, AccountProvider provider, String email);

    boolean existsByUserIdAndPrimaryTrue(UUID userId);
}
