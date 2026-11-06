package com.yourlifeone.knowledge.progress.api.mapper;

import com.yourlifeone.knowledge.progress.api.response.ProgressResponse;
import com.yourlifeone.knowledge.progress.domain.KnowledgeProgress;
import org.mapstruct.Mapper;

@Mapper
public interface ProgressMapper {
    ProgressResponse toResponse(KnowledgeProgress progress);
}
