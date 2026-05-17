package net.momirealms.craftengine.core.plugin.network.protocol.dialog.action;

import net.momirealms.sparrow.nbt.CompoundTag;

public final class DynamicRunCommandAction implements DialogAction {
    private final String template;

    public DynamicRunCommandAction(String template) {
        this.template = template;
    }

    public static DynamicRunCommandAction read(CompoundTag tag) {
        return new DynamicRunCommandAction(tag.getString("template"));
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "dynamic/run_command");
        tag.putString("template", this.template);
        return tag;
    }

    public String template() {
        return this.template;
    }
}
