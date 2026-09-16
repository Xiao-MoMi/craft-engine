package net.momirealms.craftengine.core.pack.workflow.impl;

import net.momirealms.craftengine.core.pack.workflow.*;

public record SendPackWorkflow(String pack) implements PackWorkflow {
    public static final PackWorkflowFactory<SendPackWorkflow> FACTORY = section -> new SendPackWorkflow(section.getNonEmptyString(new String[]{"pack", "host"}));

    @Override
    public PackWorkflowType<SendPackWorkflow> type() {
        return PackWorkflows.SEND_PACK;
    }

    @Override
    public void validate(PackWorkflowValidation validation) {
        validation.requireHost(this.pack);
    }

    @Override
    public void execute(PackWorkflowContext context) throws Exception {
        context.sendPack(this.pack);
    }
}
