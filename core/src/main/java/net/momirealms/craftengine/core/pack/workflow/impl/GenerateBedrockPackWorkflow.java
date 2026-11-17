package net.momirealms.craftengine.core.pack.workflow.impl;

import net.momirealms.craftengine.core.pack.workflow.*;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import org.jetbrains.annotations.Nullable;

public record GenerateBedrockPackWorkflow(String input, @Nullable String packOutput, String output) implements PackWorkflow {
    private static final String[] PACK_OUTPUT = ConfigKeys.of("pack_output");
    public static final PackWorkflowFactory<GenerateBedrockPackWorkflow> FACTORY = section -> new GenerateBedrockPackWorkflow(
            section.getNonEmptyString("input"),
            section.containsKey(PACK_OUTPUT) ? section.getNonEmptyString(PACK_OUTPUT) : null,
            section.getNonEmptyString("output")
    );

    @Override
    public PackWorkflowType<GenerateBedrockPackWorkflow> type() {
        return PackWorkflows.GENERATE_BEDROCK_PACK;
    }

    @Override
    public void validate(PackWorkflowValidation validation) {
        validation.generateBedrockPack(this.input, this.packOutput, this.output);
    }

    @Override
    public void execute(PackWorkflowContext context) throws Exception {
        context.generateBedrockPack(this.input, this.packOutput, this.output);
    }
}
