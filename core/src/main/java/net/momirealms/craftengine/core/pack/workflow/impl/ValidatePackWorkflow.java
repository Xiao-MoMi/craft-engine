package net.momirealms.craftengine.core.pack.workflow.impl;

import net.momirealms.craftengine.core.pack.workflow.*;

public record ValidatePackWorkflow() implements PackWorkflow {
    public static final PackWorkflowFactory<ValidatePackWorkflow> FACTORY = section -> new ValidatePackWorkflow();

    @Override
    public PackWorkflowType<ValidatePackWorkflow> type() {
        return PackWorkflows.VALIDATE;
    }

    @Override
    public void validate(PackWorkflowValidation validation) {
        validation.requireGeneratedPack();
    }

    @Override
    public void execute(PackWorkflowContext context) throws Exception {
        context.validatePack();
    }
}
