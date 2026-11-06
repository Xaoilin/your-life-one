package com.yourlifeone.profile.activity.api.mapper;

import com.yourlifeone.profile.activity.api.request.ProductUsageEventRequest;
import com.yourlifeone.profile.activity.api.response.ActivityIngestReceiptResponse;
import com.yourlifeone.profile.activity.api.response.ActivityInsightsResponse;
import com.yourlifeone.profile.activity.domain.ActivityInsights;
import com.yourlifeone.profile.activity.domain.IngestReceipt;
import com.yourlifeone.profile.activity.domain.ProductUsageEvent;
import java.util.List;
import org.mapstruct.Mapper;

/** Maps product-usage requests to the domain, and receipts and insights to responses. */
@Mapper
public interface ActivityMapper {
    ProductUsageEvent toDomain(ProductUsageEventRequest request);

    List<ProductUsageEvent> toDomain(List<ProductUsageEventRequest> requests);

    ActivityIngestReceiptResponse toResponse(IngestReceipt receipt);

    ActivityInsightsResponse toResponse(ActivityInsights insights);
}
