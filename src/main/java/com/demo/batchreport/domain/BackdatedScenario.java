package com.demo.batchreport.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * Represents a scenario that was loaded recently but has a batch date older than the current processing date.
 * This helps analysts track catch-up processing and delayed data delivery.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackdatedScenario {
    private String assetClass;
    private String product;
    private String scenario;
    private String entity;
    private LocalDate batchDate;     // The original batch date (historical)
    private LocalDate loadedDate;    // When it was actually loaded into the system

    /**
     * Calculate how many days late this scenario was loaded
     */
    public long getDaysLate() {
        return java.time.temporal.ChronoUnit.DAYS.between(batchDate, loadedDate);
    }

    /**
     * Get a severity level based on how late the scenario was loaded
     */
    public LateSeverity getLateSeverity() {
        long days = getDaysLate();
        if (days <= 1) {
            return LateSeverity.MINIMAL;
        } else if (days <= 3) {
            return LateSeverity.MODERATE;
        } else if (days <= 7) {
            return LateSeverity.SIGNIFICANT;
        } else {
            return LateSeverity.CRITICAL;
        }
    }

    public enum LateSeverity {
        MINIMAL("Minimal", "info", "1 day late"),
        MODERATE("Moderate", "warning", "2-3 days late"),
        SIGNIFICANT("Significant", "warning", "4-7 days late"),
        CRITICAL("Critical", "danger", "8+ days late");

        private final String displayName;
        private final String cssClass;
        private final String description;

        LateSeverity(String displayName, String cssClass, String description) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssClass() {
            return cssClass;
        }

        public String getDescription() {
            return description;
        }
    }
}