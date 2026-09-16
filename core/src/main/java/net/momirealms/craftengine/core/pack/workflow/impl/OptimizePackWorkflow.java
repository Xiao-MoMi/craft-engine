package net.momirealms.craftengine.core.pack.workflow.impl;

import net.momirealms.craftengine.core.pack.workflow.*;

public record OptimizePackWorkflow() implements PackWorkflow {
    public static final PackWorkflowFactory<OptimizePackWorkflow> FACTORY = section -> new OptimizePackWorkflow();

    @Override
    public PackWorkflowType<OptimizePackWorkflow> type() {
        return PackWorkflows.OPTIMIZE;
    }

    @Override
    public void validate(PackWorkflowValidation validation) {
        validation.requireGeneratedPack();
    }

    @Override
    public void execute(PackWorkflowContext context) throws Exception {
        context.optimize();
    }
}
