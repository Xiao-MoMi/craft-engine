/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.core.block;

public class VariantState {
    private final String appearance;
    private final BlockSettings settings;
    private final int internalId;

    public VariantState(String appearance, BlockSettings settings, int internalId) {
        this.appearance = appearance;
        this.settings = settings;
        this.internalId = internalId;
    }

    public String appearance() {
        return appearance;
    }

    public BlockSettings settings() {
        return settings;
    }

    public int internalRegistryId() {
        return internalId;
    }
}
