package com.securefiles.domain.file.model;

import java.util.Objects;

public record AntivirusScanResult(AntivirusVerdict verdict, String failureCode) {

    public AntivirusScanResult {
        Objects.requireNonNull(verdict, "verdict must not be null");
        if ((verdict == AntivirusVerdict.CLEAN || verdict == AntivirusVerdict.INFECTED)
                && failureCode != null) {
            throw new IllegalArgumentException("clean and infected results must not contain a failure code");
        }
        if ((verdict == AntivirusVerdict.RETRYABLE_FAILURE || verdict == AntivirusVerdict.TERMINAL_FAILURE)
                && (failureCode == null || failureCode.isBlank())) {
            throw new IllegalArgumentException("failed scan results must contain a failure code");
        }
    }

    public static AntivirusScanResult clean() {
        return new AntivirusScanResult(AntivirusVerdict.CLEAN, null);
    }

    public static AntivirusScanResult infected() {
        return new AntivirusScanResult(AntivirusVerdict.INFECTED, null);
    }

    public static AntivirusScanResult retryableFailure(String failureCode) {
        return new AntivirusScanResult(AntivirusVerdict.RETRYABLE_FAILURE, failureCode);
    }

    public static AntivirusScanResult terminalFailure(String failureCode) {
        return new AntivirusScanResult(AntivirusVerdict.TERMINAL_FAILURE, failureCode);
    }
}
