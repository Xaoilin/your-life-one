package com.yourlifeone.prayer.domain;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

/**
 * Turns counts into whole percentages that add up to exactly 100 (the largest remainder method).
 *
 * <p>Each share is rounded down first; the points still missing go one each to the shares that
 * lost the most in rounding. Ties go to the earlier share, so on-time wins over late over missed.
 */
public final class PercentageSplit {
    private static final int WHOLE = 100;

    private PercentageSplit() {
    }

    public static OutcomePercentages of(int onTime, int late, int missed) {
        int total = onTime + late + missed;
        if (total == 0) return OutcomePercentages.NONE;

        double[] exactShares = exactShares(total, onTime, late, missed);
        int[] roundedShares = roundDown(exactShares);
        int missingPoints = WHOLE - IntStream.of(roundedShares).sum();
        giveMissingPointsToLargestRemainders(exactShares, roundedShares, missingPoints);
        return new OutcomePercentages(roundedShares[0], roundedShares[1], roundedShares[2]);
    }

    private static double[] exactShares(int total, int... counts) {
        return IntStream.of(counts).mapToDouble(count -> count * (double) WHOLE / total).toArray();
    }

    private static int[] roundDown(double[] shares) {
        return Arrays.stream(shares).mapToInt(share -> (int) Math.floor(share)).toArray();
    }

    private static void giveMissingPointsToLargestRemainders(double[] exact, int[] rounded, int missingPoints) {
        Comparator<Integer> largestRemainderFirst =
                Comparator.comparingDouble((Integer index) -> exact[index] - rounded[index]).reversed();
        IntStream.range(0, rounded.length).boxed()
                .sorted(largestRemainderFirst.thenComparing(Comparator.naturalOrder()))
                .limit(missingPoints)
                .forEach(index -> rounded[index]++);
    }
}
