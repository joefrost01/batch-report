package com.demo.batchreport.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Static configuration for expected batch scenarios.
 * Updated approximately twice yearly when new scenarios are introduced.
 * Flat structure for easy maintenance and Excel generation.
 */
@Getter
public class ExpectedScenariosConfig {

    /**
     * Represents an expected scenario configuration
     */
    @Data
    @AllArgsConstructor
    public static class ExpectedScenario {
        private String assetClass;
        private String product;
        private String entity;
        private String scenario;

        /**
         * Get unique key for grouping (asset class + product + entity)
         */
        public String getGroupKey() {
            return String.format("%s|%s|%s", assetClass, product, entity);
        }

        /**
         * Get full unique key including scenario
         */
        public String getFullKey() {
            return String.format("%s|%s|%s|%s", assetClass, product, entity, scenario);
        }
    }

    /**
     * Complete list of expected scenarios.
     * Flat structure: asset-class, product, entity, scenario
     */
    private static final List<ExpectedScenario> EXPECTED_SCENARIOS = List.of(
            // Equity - US Large Cap
            new ExpectedScenario("Equity", "US Large Cap", "Entity A", "Base"),
            new ExpectedScenario("Equity", "US Large Cap", "Entity A", "Stress"),
            new ExpectedScenario("Equity", "US Large Cap", "Entity A", "Adverse"),
            new ExpectedScenario("Equity", "US Large Cap", "Entity B", "Base"),
            new ExpectedScenario("Equity", "US Large Cap", "Entity B", "Stress"),
            new ExpectedScenario("Equity", "US Large Cap", "Entity B", "Adverse"),

            // Equity - US Small Cap
            new ExpectedScenario("Equity", "US Small Cap", "Entity A", "Base"),
            new ExpectedScenario("Equity", "US Small Cap", "Entity A", "Stress"),
            new ExpectedScenario("Equity", "US Small Cap", "Entity B", "Base"),
            new ExpectedScenario("Equity", "US Small Cap", "Entity B", "Stress"),

            // Equity - International
            new ExpectedScenario("Equity", "International", "Entity A", "Base"),
            new ExpectedScenario("Equity", "International", "Entity A", "Stress"),
            new ExpectedScenario("Equity", "International", "Entity A", "Adverse"),
            new ExpectedScenario("Equity", "International", "Entity C", "Base"),
            new ExpectedScenario("Equity", "International", "Entity C", "Stress"),
            new ExpectedScenario("Equity", "International", "Entity C", "Adverse"),

            // Equity - Emerging Markets
            new ExpectedScenario("Equity", "Emerging Markets", "Entity B", "Base"),
            new ExpectedScenario("Equity", "Emerging Markets", "Entity B", "Stress"),
            new ExpectedScenario("Equity", "Emerging Markets", "Entity C", "Base"),
            new ExpectedScenario("Equity", "Emerging Markets", "Entity C", "Stress"),

            // Fixed Income - Corporate Bonds
            new ExpectedScenario("Fixed Income", "Corporate Bonds", "Entity A", "Base"),
            new ExpectedScenario("Fixed Income", "Corporate Bonds", "Entity A", "Stress"),
            new ExpectedScenario("Fixed Income", "Corporate Bonds", "Entity A", "Adverse"),
            new ExpectedScenario("Fixed Income", "Corporate Bonds", "Entity A", "Severe"),
            new ExpectedScenario("Fixed Income", "Corporate Bonds", "Entity B", "Base"),
            new ExpectedScenario("Fixed Income", "Corporate Bonds", "Entity B", "Stress"),
            new ExpectedScenario("Fixed Income", "Corporate Bonds", "Entity B", "Adverse"),
            new ExpectedScenario("Fixed Income", "Corporate Bonds", "Entity B", "Severe"),

            // Fixed Income - Government Bonds
            new ExpectedScenario("Fixed Income", "Government Bonds", "Entity A", "Base"),
            new ExpectedScenario("Fixed Income", "Government Bonds", "Entity A", "Stress"),
            new ExpectedScenario("Fixed Income", "Government Bonds", "Entity A", "Adverse"),
            new ExpectedScenario("Fixed Income", "Government Bonds", "Entity C", "Base"),
            new ExpectedScenario("Fixed Income", "Government Bonds", "Entity C", "Stress"),
            new ExpectedScenario("Fixed Income", "Government Bonds", "Entity C", "Adverse"),

            // Fixed Income - Municipal Bonds
            new ExpectedScenario("Fixed Income", "Municipal Bonds", "Entity A", "Base"),
            new ExpectedScenario("Fixed Income", "Municipal Bonds", "Entity A", "Stress"),
            new ExpectedScenario("Fixed Income", "Municipal Bonds", "Entity C", "Base"),
            new ExpectedScenario("Fixed Income", "Municipal Bonds", "Entity C", "Stress"),

            // Fixed Income - High Yield
            new ExpectedScenario("Fixed Income", "High Yield", "Entity B", "Base"),
            new ExpectedScenario("Fixed Income", "High Yield", "Entity B", "Stress"),
            new ExpectedScenario("Fixed Income", "High Yield", "Entity B", "Adverse"),

            // Derivatives - Interest Rate
            new ExpectedScenario("Derivatives", "Interest Rate", "Entity A", "Base"),
            new ExpectedScenario("Derivatives", "Interest Rate", "Entity A", "Stress"),
            new ExpectedScenario("Derivatives", "Interest Rate", "Entity A", "Adverse"),
            new ExpectedScenario("Derivatives", "Interest Rate", "Entity A", "Severe"),
            new ExpectedScenario("Derivatives", "Interest Rate", "Entity B", "Base"),
            new ExpectedScenario("Derivatives", "Interest Rate", "Entity B", "Stress"),
            new ExpectedScenario("Derivatives", "Interest Rate", "Entity B", "Adverse"),
            new ExpectedScenario("Derivatives", "Interest Rate", "Entity B", "Severe"),

            // Derivatives - Credit
            new ExpectedScenario("Derivatives", "Credit", "Entity A", "Base"),
            new ExpectedScenario("Derivatives", "Credit", "Entity A", "Stress"),
            new ExpectedScenario("Derivatives", "Credit", "Entity A", "Adverse"),
            new ExpectedScenario("Derivatives", "Credit", "Entity B", "Base"),
            new ExpectedScenario("Derivatives", "Credit", "Entity B", "Stress"),
            new ExpectedScenario("Derivatives", "Credit", "Entity B", "Adverse"),

            // Derivatives - Equity
            new ExpectedScenario("Derivatives", "Equity", "Entity A", "Base"),
            new ExpectedScenario("Derivatives", "Equity", "Entity A", "Stress"),
            new ExpectedScenario("Derivatives", "Equity", "Entity B", "Base"),
            new ExpectedScenario("Derivatives", "Equity", "Entity B", "Stress"),

            // Cash - Money Market
            new ExpectedScenario("Cash", "Money Market", "Entity A", "Base"),
            new ExpectedScenario("Cash", "Money Market", "Entity B", "Base"),
            new ExpectedScenario("Cash", "Money Market", "Entity C", "Base"),

            // Alternative - Real Estate
            new ExpectedScenario("Alternative", "Real Estate", "Entity B", "Base"),
            new ExpectedScenario("Alternative", "Real Estate", "Entity B", "Stress"),
            new ExpectedScenario("Alternative", "Real Estate", "Entity C", "Base"),
            new ExpectedScenario("Alternative", "Real Estate", "Entity C", "Stress"),

            // Alternative - Commodities
            new ExpectedScenario("Alternative", "Commodities", "Entity B", "Base"),
            new ExpectedScenario("Alternative", "Commodities", "Entity B", "Stress"),
            new ExpectedScenario("Alternative", "Commodities", "Entity B", "Adverse"),
            new ExpectedScenario("Alternative", "Commodities", "Entity C", "Base"),
            new ExpectedScenario("Alternative", "Commodities", "Entity C", "Stress"),
            new ExpectedScenario("Alternative", "Commodities", "Entity C", "Adverse")
    );

    /**
     * Get all expected scenarios
     */
    public static List<ExpectedScenario> getAllExpectedScenarios() {
        return EXPECTED_SCENARIOS;
    }

    /**
     * Get expected scenarios for a specific asset class, product, and entity combination
     */
    public static List<ExpectedScenario> getExpectedScenarios(String assetClass, String product, String entity) {
        return EXPECTED_SCENARIOS.stream()
                .filter(es -> es.getAssetClass().equals(assetClass)
                        && es.getProduct().equals(product)
                        && es.getEntity().equals(entity))
                .collect(Collectors.toList());
    }

    /**
     * Get expected scenario count for a specific asset class, product, and entity combination
     */
    public static int getExpectedScenarioCount(String assetClass, String product, String entity) {
        return getExpectedScenarios(assetClass, product, entity).size();
    }

    /**
     * Check if a specific scenario is expected
     */
    public static boolean isScenarioExpected(String assetClass, String product, String entity, String scenario) {
        return EXPECTED_SCENARIOS.stream()
                .anyMatch(es -> es.getAssetClass().equals(assetClass)
                        && es.getProduct().equals(product)
                        && es.getEntity().equals(entity)
                        && es.getScenario().equals(scenario));
    }

    /**
     * Get all unique asset class, product, entity combinations
     */
    public static Set<String> getAllGroupKeys() {
        return EXPECTED_SCENARIOS.stream()
                .map(ExpectedScenario::getGroupKey)
                .collect(Collectors.toSet());
    }

    /**
     * Get all unique asset classes
     */
    public static Set<String> getAllAssetClasses() {
        return EXPECTED_SCENARIOS.stream()
                .map(ExpectedScenario::getAssetClass)
                .collect(Collectors.toSet());
    }

    /**
     * Get all unique entities
     */
    public static Set<String> getAllEntities() {
        return EXPECTED_SCENARIOS.stream()
                .map(ExpectedScenario::getEntity)
                .collect(Collectors.toSet());
    }

    /**
     * Get all unique products for a specific asset class
     */
    public static Set<String> getProductsForAssetClass(String assetClass) {
        return EXPECTED_SCENARIOS.stream()
                .filter(es -> es.getAssetClass().equals(assetClass))
                .map(ExpectedScenario::getProduct)
                .collect(Collectors.toSet());
    }

    /**
     * Get total expected scenarios count
     */
    public static int getTotalExpectedScenarios() {
        return EXPECTED_SCENARIOS.size();
    }

    /**
     * Get scenarios grouped by asset class, product, entity
     */
    public static Map<String, List<ExpectedScenario>> getScenariosByGroup() {
        return EXPECTED_SCENARIOS.stream()
                .collect(Collectors.groupingBy(ExpectedScenario::getGroupKey));
    }
}