package net.momirealms.craftengine.core.pack.workflow.impl;

import net.momirealms.craftengine.core.pack.workflow.*;

public record ZipPackWorkflow(String path, boolean protection, boolean storePng) implements PackWorkflow {
    public static final PackWorkflowFactory<ZipPackWorkflow> FACTORY = section -> new ZipPackWorkflow(section.getNonEmptyString("path"), section.getBoolean("protection", false), section.getBoolean("store-png", false));

    public ZipPackWorkflow(String path, boolean protection) {
        this(path, protection, false);
    }

    @Override
    public PackWorkflowType<ZipPackWorkflow> type() {
        return PackWorkflows.ZIP;
    }

    @Override
    public void validate(PackWorkflowValidation validation) {
        validation.zip(this.path, this.protection);
    }

    @Override
    public void execute(PackWorkflowContext context) throws Exception {
        context.zip(this.path, this.protection, this.storePng);
    }
}
