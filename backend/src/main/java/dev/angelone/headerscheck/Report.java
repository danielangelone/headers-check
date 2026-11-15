package dev.angelone.headerscheck;

import java.util.List;

record Report(String url, String finalUrl, int status, String grade, List<Check> checks, Wordpress wordpress) {

    enum Result { PASS, WARN, FAIL }

    record Check(String id, String label, Result result, String detail) {
    }

    record Wordpress(boolean detected, String version, List<String> findings) {
    }
}
