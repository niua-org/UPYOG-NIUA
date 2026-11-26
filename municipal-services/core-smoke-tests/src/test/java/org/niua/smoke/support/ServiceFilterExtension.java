package org.niua.smoke.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ServiceFilterExtension implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String filter = SmokeConfig.normalizedServiceName(SmokeConfig.serviceFilter());
        if ("all".equals(filter)) {
            return ConditionEvaluationResult.enabled("Running all smoke services");
        }
        SmokeService annotation = context.getRequiredTestClass().getAnnotation(SmokeService.class);
        if (annotation == null) {
            return ConditionEvaluationResult.enabled("No @SmokeService — always run");
        }
        boolean match = filter.equals(SmokeConfig.normalizedServiceName(annotation.value()));
        return match
                ? ConditionEvaluationResult.enabled("Matched service filter " + filter)
                : ConditionEvaluationResult.disabled("Skipped by -Dservice=" + filter);
    }
}
