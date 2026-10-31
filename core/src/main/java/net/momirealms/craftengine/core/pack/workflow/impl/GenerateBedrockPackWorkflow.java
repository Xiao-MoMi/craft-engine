package net.momirealms.craftengine.core.pack.workflow.impl;

import net.momirealms.craftengine.core.pack.workflow.*;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;

public record GenerateBedrockPackWorkflow(String input, String packOutput, String dataOutput) implements PackWorkflow {
    private static final String[] PACK_OUTPUT = ConfigKeys.of("pack_output");
    private static final String[] DATA_OUTPUT = ConfigKeys.of("data_output");
    public static final PackWorkflowFactory<GenerateBedrockPackWorkflow> FACTORY = section -> new GenerateBedrockPackWorkflow(
            section.getNonEmptyString("input"),
            section.getNonEmptyString(PACK_OUTPUT),
            section.getNonEmptyString(DATA_OUTPUT)
    );

    @Override
    public PackWorkflowType<GenerateBedrockPackWorkflow> type() {
        return PackWorkflows.GENERATE_BEDROCK_PACK;
    }

    @Override
    public void validate(PackWorkflowValidation validation) {
        validation.generateBedrockPack(this.input, this.packOutput, this.dataOutput);
    }

    @Override
    public void execute(PackWorkflowContext context) throws Exception {
        context.generateBedrockPack(this.input, this.packOutput, this.dataOutput);
    }
}