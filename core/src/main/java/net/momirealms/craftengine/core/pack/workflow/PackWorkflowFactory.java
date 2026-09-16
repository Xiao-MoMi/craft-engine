package net.momirealms.craftengine.core.pack.workflow;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

@FunctionalInterface
public interface PackWorkflowFactory<T extends PackWorkflow> {

    T create(ConfigSection section);
}
