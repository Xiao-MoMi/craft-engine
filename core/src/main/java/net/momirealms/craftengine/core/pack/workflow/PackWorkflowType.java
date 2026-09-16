package net.momirealms.craftengine.core.pack.workflow;

import net.momirealms.craftengine.core.util.Key;

public record PackWorkflowType<T extends PackWorkflow>(Key id, PackWorkflowFactory<T> factory) {
}
