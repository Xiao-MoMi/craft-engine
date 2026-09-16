package net.momirealms.craftengine.core.pack.workflow.impl;

import net.momirealms.craftengine.core.pack.PackGenerationOptions;
import net.momirealms.craftengine.core.pack.workflow.*;

public record GeneratePackWorkflow(PackGenerationOptions options) implements PackWorkflow {
    public static final PackWorkflowFactory<GeneratePackWorkflow> FACTORY = section -> new GeneratePackWorkflow(
            PackGenerationOptions.defaults().override(section));

    @Override
    public PackWorkflowType<GeneratePackWorkflow> type() {
        return PackWorkflows.GENERATE;
    }

    @Override
    public void validate(PackWorkflowValidation validation) {
        validation.generate();
    }

    @Override
    public void execute(PackWorkflowContext context) throws Exception {
        context.generate(this.options);
    }
}
