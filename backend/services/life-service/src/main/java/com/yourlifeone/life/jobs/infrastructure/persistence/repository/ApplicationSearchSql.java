package com.yourlifeone.life.jobs.infrastructure.persistence.repository;

/** Shared by the bounded ID read and exact count so their matching semantics cannot diverge. */
final class ApplicationSearchSql {
    static final String MATCHING = """
            FROM jobs.application a
            WHERE a.user_id = :userId
              AND (:status IS NULL OR a.status = :status)
              AND (:workType IS NULL OR a.work_type = :workType)
              AND (:remoteStatus IS NULL OR a.remote_status = :remoteStatus)
              AND (:needle = '' OR strpos(lower(concat(E'\\n', concat_ws(E'\\n',
                a.company,a.role,a.url,a.status,a.work_type,a.remote_region,a.remote_evidence,
                a.remote_caveat,a.compensation,a.next_action,a.notes,
                (SELECT string_agg(concat_ws(E'\\n',h.kind,h.summary,h.details,h.evidence_url),
                    E'\\n' ORDER BY h.position)
                 FROM jobs.history_entry h WHERE h.user_id=a.user_id AND h.application_id=a.id)
              )) COLLATE "und-x-icu"), :needle) > 0)
            """;

    private ApplicationSearchSql() {
    }
}
