package net.knightsandkings.knk.core.domain.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validation context embedded in WorldTask.InputJson.
 * Contains all validation rules and form data needed for validation.
 */
public class WorldTaskValidationContext {
    private List<WorldTaskValidationRule> validationRules;
    private Map<String, Object> formContext;

    public WorldTaskValidationContext() {
        this.validationRules = new ArrayList<>();
        this.formContext = new HashMap<>();
    }

    public WorldTaskValidationContext(List<WorldTaskValidationRule> validationRules, 
                                     Map<String, Object> formContext) {
        this.validationRules = validationRules != null ? validationRules : new ArrayList<>();
        this.formContext = formContext != null ? formContext : new HashMap<>();
    }

    public List<WorldTaskValidationRule> getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(List<WorldTaskValidationRule> validationRules) {
        this.validationRules = validationRules;
    }

    public Map<String, Object> getFormContext() {
        return formContext;
    }

    public void setFormContext(Map<String, Object> formContext) {
        this.formContext = formContext;
    }
}
