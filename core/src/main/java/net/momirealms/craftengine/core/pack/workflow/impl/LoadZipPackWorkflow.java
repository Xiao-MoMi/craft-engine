package net.momirealms.craftengine.core.pack.workflow.impl;

import net.momirealms.craftengine.core.pack.workflow.*;

public record LoadZipPackWorkflow(String path) implements PackWorkflow {
    public static final PackWorkflowFactory<LoadZipPackWorkflow> FACTORY = section -> new LoadZipPackWorkflow(section.getNonEmptyString("path"));

    @Override
    public PackWorkflowType<LoadZipPackWorkflow> type() {
        return PackWorkflows.LOAD_ZIP;
    }

    @Override
    public void validate(PackWorkflowValidation validation) {
        validation.loadZip(this.path);
    }

    @Override
    public void execute(PackWorkflowContext context) throws Exception {
        context.loadZip(this.path);
    }
}
