package com.yourlifeone.profile.activity.application.service;

import com.yourlifeone.profile.activity.domain.IngestReceipt;
import com.yourlifeone.profile.activity.domain.ProductUsageEvent;
import com.yourlifeone.profile.activity.infrastructure.persistence.ProductUsageEventRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores a batch of product-usage events; an event the user already has counts as a duplicate, not an error. */
@Service
@RequiredArgsConstructor
public class ActivityIngestService {
    private final ProductUsageEventValidator validator;
    private final ProductUsageEventRepository events;

    @Transactional
    public IngestReceipt ingest(UUID userId, List<ProductUsageEvent> batch) {
        validator.validate(batch);
        var accepted = (int) batch.stream().filter(event -> events.insertIfAbsent(userId, event)).count();
        return new IngestReceipt(accepted, batch.size() - accepted);
    }
}
