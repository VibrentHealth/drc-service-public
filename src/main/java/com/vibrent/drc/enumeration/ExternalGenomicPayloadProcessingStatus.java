package com.vibrent.drc.enumeration;

public enum ExternalGenomicPayloadProcessingStatus {

    PENDING("pending"), COMPLETE("complete"), RETRY("retry"), ERROR("error"), PROCESSING("processing");

    private String externalReportPayloadStatus;

    ExternalGenomicPayloadProcessingStatus(String externalReportPayloadStatus) {
        this.externalReportPayloadStatus = externalReportPayloadStatus;
    }

    public static ExternalGenomicPayloadProcessingStatus getExternalReportPayloadStatusFromValue(String value) {
        for (ExternalGenomicPayloadProcessingStatus externalReportPayloadsStatus : ExternalGenomicPayloadProcessingStatus.values()) {
            if (externalReportPayloadsStatus.externalReportPayloadStatus.equalsIgnoreCase(value)) {
                return externalReportPayloadsStatus;
            }
        }
        return null;
    }
}
