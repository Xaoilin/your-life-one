package com.yourlifeone.calendar.infrastructure.persistence.repository;

import com.yourlifeone.calendar.infrastructure.persistence.entity.GoogleCredentialEntity;
import com.yourlifeone.calendar.infrastructure.persistence.entity.GoogleCredentialId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleCredentialRepository extends JpaRepository<GoogleCredentialEntity, GoogleCredentialId> {
}
