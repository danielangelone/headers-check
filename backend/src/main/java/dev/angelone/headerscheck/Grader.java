package dev.angelone.headerscheck;

import dev.angelone.headerscheck.Report.Check;
import dev.angelone.headerscheck.Report.Result;
import java.util.List;
import java.util.Map;

final class Grader {

    // Points taken off a score of 100 for each check with that result.
    private static final Map<Result, Integer> PENALTY = Map.of(Result.PASS, 0, Result.WARN, 4, Result.FAIL, 12);

    private Grader() {
    }

    static String grade(List<Check> checks) {
        int score = 100;
        for (Check c : checks) {
            score -= PENALTY.get(c.result());
        }
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}
