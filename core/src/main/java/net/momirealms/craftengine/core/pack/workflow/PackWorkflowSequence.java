package net.momirealms.craftengine.core.pack.workflow;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record PackWorkflowSequence(String name, Set<String> triggers, List<PackWorkflow> workflows, boolean protection) {
    public static final String RELOAD_PACK = "reload_pack";

    public PackWorkflowSequence {
        triggers = Set.copyOf(triggers);
        workflows = List.copyOf(workflows);
        if (!name.matches("[A-Za-z0-9_-]+") || workflows.isEmpty()) {
            throw new IllegalArgumentException("Invalid or empty resource pack workflow: " + name);
        }
    }

    public static PackWorkflowSequence fromConfig(String name, ConfigValue value, PackWorkflowValidation validation) {
        ConfigSection section = value.getAsSection();
        Set<String> triggers = Set.copyOf(section.getStringList("trigger"));
        List<PackWorkflow> workflows = section.getList("steps", entry -> PackWorkflows.fromConfig(
                entry.value() instanceof String ? ConfigSection.of(entry.path(), Map.of("type", entry.getAsString())) : entry.getAsSection()
        ));
        for (PackWorkflow workflow : workflows) {
            workflow.validate(validation);
        }
        return new PackWorkflowSequence(name, triggers, workflows, validation.usesProtection());
    }

    public void execute(PackWorkflowContext context) throws Exception {
        for (PackWorkflow workflow : this.workflows) {
            workflow.execute(context);
        }
    }
}
