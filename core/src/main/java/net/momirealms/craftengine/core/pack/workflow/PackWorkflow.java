package net.momirealms.craftengine.core.pack.workflow;

public interface PackWorkflow {

    PackWorkflowType<? extends PackWorkflow> type();

    default void validate(PackWorkflowValidation validation) {}

    void execute(PackWorkflowContext context) throws Exception;
}
