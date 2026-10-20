package net.momirealms.craftengine.core.pack.workflow.impl;

import net.momirealms.craftengine.core.pack.workflow.PackWorkflow;
import net.momirealms.craftengine.core.pack.workflow.PackWorkflowContext;
import net.momirealms.craftengine.core.pack.workflow.PackWorkflowFactory;
import net.momirealms.craftengine.core.pack.workflow.PackWorkflowType;
import net.momirealms.craftengine.core.pack.workflow.PackWorkflowValidation;
import net.momirealms.craftengine.core.pack.workflow.PackWorkflows;

public record GenerateBedrockPackWorkflow(String input, String output) implements PackWorkflow {
    public static final PackWorkflowFactory<GenerateBedrockPackWorkflow> FACTORY = section -> new GenerateBedrockPackWorkflow(section.getNonEmptyString("input"), section.getNonEmptyString("output"));

    @Override
    public PackWorkflowType<GenerateBedrockPackWorkflow> type() {
        return PackWorkflows.GENERATE_BEDROCK_PACK;
    }

    @Override
    public void validate(PackWorkflowValidation validation) {
        validation.generateBedrockPack(this.input, this.output);
    }

    @Override
    public void execute(PackWorkflowContext context) throws Exception {
        context.generateBedrockPack(this.input, this.output);
    }
}
