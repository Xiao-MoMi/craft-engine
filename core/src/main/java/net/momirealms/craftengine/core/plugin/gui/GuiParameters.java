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

package net.momirealms.craftengine.core.plugin.gui;

import net.momirealms.craftengine.core.util.context.ContextKey;

public class GuiParameters {
    public static final ContextKey<String> MAX_PAGE = ContextKey.of("max_page");
    public static final ContextKey<String> CURRENT_PAGE = ContextKey.of("current_page");
    public static final ContextKey<String> COOKING_TIME = ContextKey.of("cooking_time");
    public static final ContextKey<String> COOKING_EXPERIENCE = ContextKey.of("cooking_experience");
}
