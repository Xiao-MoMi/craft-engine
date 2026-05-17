package net.momirealms.craftengine.core.item.setting.value;

import java.util.List;

public record AnvilRepairItem(List<String> targets, int amount, double percent) {
}
