package com.demo.batchreport.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents detailed scenario information including expected vs actual status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioDetail {
    private String assetClass;
    private String product;
    private String scenario;
    private String entity;
    private boolean isLoaded;
    private boolean isExpected;

    /**
     * Get the status of this scenario
     */
    public ScenarioStatus getStatus() {
        if (!isExpected && isLoaded) {
            return ScenarioStatus.UNEXPECTED;
        } else if (isExpected && isLoaded) {
            return ScenarioStatus.LOADED;
        } else if (isExpected && !isLoaded) {
            return ScenarioStatus.MISSING;
        } else {
            return ScenarioStatus.NOT_APPLICABLE;
        }
    }

    public enum ScenarioStatus {
        LOADED("Loaded", "success", "✓"),
        MISSING("Missing", "danger", "✗"),
        UNEXPECTED("Unexpected", "warning", "!"),
        NOT_APPLICABLE("N/A", "neutral", "-");

        private final String displayName;
        private final String cssClass;
        private final String symbol;

        ScenarioStatus(String displayName, String cssClass, String symbol) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.symbol = symbol;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssClass() {
            return cssClass;
        }

        public String getSymbol() {
            return symbol;
        }
    }
}