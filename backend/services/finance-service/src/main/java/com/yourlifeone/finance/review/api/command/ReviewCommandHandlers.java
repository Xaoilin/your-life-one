package com.yourlifeone.finance.review.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.finance.review.api.mapper.ReviewMapper;
import com.yourlifeone.finance.review.api.request.SaveReviewRequest;
import com.yourlifeone.finance.review.application.service.ReviewService;
import com.yourlifeone.finance.shared.CommandHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The banking review write the command queue performs; its type is also the change event's routing key. */
@Configuration(proxyBeanMethods = false)
public class ReviewCommandHandlers {
    public static final String SAVE_REVIEW = "finance.save-review";

    @Bean
    WriteCommandHandler<SaveReviewRequest> saveReviewHandler(ReviewService reviews, ReviewMapper mapper) {
        return CommandHandler.ok(SAVE_REVIEW, SaveReviewRequest.class, (user, request) -> mapper.toEnvelope(
                request.loanProfileChange() == null
                        ? reviews.save(user, request.review(), request.expectedUpdatedAt())
                        : reviews.changeProfile(user, request.loanProfileChange(), request.expectedUpdatedAt())));
    }
}
