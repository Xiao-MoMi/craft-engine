package net.momirealms.craftengine.core.pack.workflow.impl;

import net.momirealms.craftengine.core.pack.workflow.*;

public record ExportPackWorkflow(String path) implements PackWorkflow {
    public static final PackWorkflowFactory<ExportPackWorkflow> FACTORY = section -> new ExportPackWorkflow(section.getNonEmptyString("path"));

    @Override
    public PackWorkflowType<ExportPackWorkflow> type() {
        return PackWorkflows.EXPORT;
    }

    @Override
    public void validate(PackWorkflowValidation validation) {
        validation.export(this.path);
    }

    @Override
    public void execute(PackWorkflowContext context) throws Exception {
        context.export(this.path);
    }
}
