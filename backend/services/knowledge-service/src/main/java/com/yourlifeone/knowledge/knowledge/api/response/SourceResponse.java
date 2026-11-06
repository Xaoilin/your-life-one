package com.yourlifeone.knowledge.knowledge.api.response;

public record SourceResponse(
        String type,
        Integer surah,
        Integer ayahStart,
        Integer ayahEnd,
        String collection,
        String hadithNumber,
        String author,
        String title,
        String url) {
}
