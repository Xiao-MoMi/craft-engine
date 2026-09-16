package net.momirealms.craftengine.core.pack.workflow.impl;

import net.momirealms.craftengine.core.pack.workflow.*;

public record UploadPackWorkflow(String pack, String path) implements PackWorkflow {
    public static final PackWorkflowFactory<UploadPackWorkflow> FACTORY = section -> new UploadPackWorkflow(section.getNonEmptyString(new String[]{"pack", "host"}), section.getNonEmptyString("path"));

    @Override
    public PackWorkflowType<UploadPackWorkflow> type() {
        return PackWorkflows.UPLOAD;
    }

    @Override
    public void validate(PackWorkflowValidation validation) {
        validation.upload(this.pack);
    }

    @Override
    public void execute(PackWorkflowContext context) throws Exception {
        context.upload(this.pack, this.path);
    }
}
