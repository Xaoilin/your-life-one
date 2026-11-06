package com.yourlifeone.prayer.domain;

/** Counts prayer statuses one by one, then produces an {@link OutcomeTally}. */
public final class OutcomeCounter {
    private int onTime;
    private int late;
    private int missed;
    private int inferredMissed;
    private int unclassified;
    private int pending;

    /** @param inferred true when a miss was deduced from a closed window rather than recorded */
    public void count(DayStatus status, boolean inferred) {
        switch (status) {
            case ON_TIME -> onTime++;
            case LATE -> late++;
            case MISSED -> {
                missed++;
                if (inferred) inferredMissed++;
            }
            case UNCLASSIFIED -> unclassified++;
            case PENDING, NOT_TRACKED -> pending++;
        }
    }

    public OutcomeTally tally() {
        int classifiedTotal = onTime + late + missed;
        return OutcomeTally.builder()
                .onTime(onTime)
                .late(late)
                .missed(missed)
                .inferredMissed(inferredMissed)
                .unclassified(unclassified)
                .pending(pending)
                .classifiedTotal(classifiedTotal)
                .opportunities(classifiedTotal + unclassified + pending)
                .percentages(PercentageSplit.of(onTime, late, missed))
                .build();
    }
}
