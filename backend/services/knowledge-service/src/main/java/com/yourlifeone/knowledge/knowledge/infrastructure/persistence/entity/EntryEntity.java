package com.yourlifeone.knowledge.knowledge.infrastructure.persistence.entity;

import com.yourlifeone.knowledge.knowledge.domain.KnowledgeSource;
import com.yourlifeone.knowledge.shared.UserScopedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "knowledge", name = "entry")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntryEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String topicId;
    private String title;
    private String content;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<KnowledgeSource> sources;
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
}
