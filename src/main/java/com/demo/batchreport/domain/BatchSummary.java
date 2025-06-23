package com.demo.batchreport.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchSummary {
    private String assetClass;
    private String product;
    private String entity;
    private Long loadCount;
    private Long expectedCount;
    private CompletionStatus status;

    /**
     * Constructor for backward compatibility
     */
    public BatchSummary(String assetClass, String product, String entity, Long loadCount) {
        this.assetClass = assetClass;
        this.product = product;
        this.entity = entity;
        this.loadCount = loadCount;
        this.expectedCount = 0L;
        this.status = CompletionStatus.UNKNOWN;
    }

    /**
     * Get completion percentage as a formatted string
     */
    public String getCompletionPercentage() {
        if (expectedCount == 0) {
            return "N/A";
        }
        double percentage = (loadCount.doubleValue() / expectedCount.doubleValue()) * 100;
        return String.format("%.0f%%", percentage);
    }

    /**
     * Check if this summary represents a complete load
     */
    public boolean isComplete() {
        return status == CompletionStatus.COMPLETE;
    }

    /**
     * Get the number of missing scenarios
     */
    public Long getMissingCount() {
        return Math.max(0, expectedCount - loadCount);
    }

    public enum CompletionStatus {
        COMPLETE("Complete", "success"),
        INCOMPLETE("Incomplete", "warning"),
        EXCESS("Excess Data", "info"),
        UNKNOWN("Unknown", "neutral");

        private final String displayName;
        private final String cssClass;

        CompletionStatus(String displayName, String cssClass) {
            this.displayName = displayName;
            this.cssClass = cssClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssClass() {
            return cssClass;
        }

        /**
         * Determine completion status based on loaded vs expected counts
         */
        public static CompletionStatus fromCounts(Long loadCount, Long expectedCount) {
            if (expectedCount == 0) {
                return UNKNOWN;
            }
            if (loadCount.equals(expectedCount)) {
                return COMPLETE;
            } else if (loadCount > expectedCount) {
                return EXCESS;
            } else {
                return INCOMPLETE;
            }
        }
    }
}