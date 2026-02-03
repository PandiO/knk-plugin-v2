package net.knightsandkings.knk.core.domain.validation;

import com.google.gson.JsonElement;

/**
 * Represents a validation rule to be executed during WorldTask processing.
 * Maps to FieldValidationRule on the backend.
 */
public class WorldTaskValidationRule {
    private String validationType;
    private String configJson;
    private String errorMessage;
    private boolean isBlocking;
    private JsonElement dependencyFieldValue;

    public WorldTaskValidationRule() {
    }

    public WorldTaskValidationRule(String validationType, String configJson, String errorMessage, 
                                   boolean isBlocking, JsonElement dependencyFieldValue) {
        this.validationType = validationType;
        this.configJson = configJson;
        this.errorMessage = errorMessage;
        this.isBlocking = isBlocking;
        this.dependencyFieldValue = dependencyFieldValue;
    }

    public String getValidationType() {
        return validationType;
    }

    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isBlocking() {
        return isBlocking;
    }

    public void setBlocking(boolean blocking) {
        isBlocking = blocking;
    }

    public JsonElement getDependencyFieldValue() {
        return dependencyFieldValue;
    }

    public void setDependencyFieldValue(JsonElement dependencyFieldValue) {
        this.dependencyFieldValue = dependencyFieldValue;
    }
}
