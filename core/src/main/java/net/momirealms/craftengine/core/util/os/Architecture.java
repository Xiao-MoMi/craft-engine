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

package net.momirealms.craftengine.core.util.os;

import java.util.Locale;

public enum Architecture {
    X64(true),
    X86(false),
    ARM64(true),
    ARM32(false),
    PPC64LE(true),
    RISCV64(true);

    static final Architecture current;
    final boolean is64Bit;

    Architecture(boolean is64Bit) {
        this.is64Bit = is64Bit;
    }

    public String getNativePath() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    public static Architecture get() {
        return current;
    }

    static {
        String osArch = System.getProperty("os.arch");
        boolean is64Bit = osArch.contains("64") || osArch.startsWith("armv8");
        if (!osArch.startsWith("arm") && !osArch.startsWith("aarch")) {
            if (osArch.startsWith("ppc")) {
                if (!"ppc64le".equals(osArch)) {
                    throw new UnsupportedOperationException("Only PowerPC 64 LE is supported.");
                }
                current = PPC64LE;
            } else if (osArch.startsWith("riscv")) {
                if (!"riscv64".equals(osArch)) {
                    throw new UnsupportedOperationException("Only RISC-V 64 is supported.");
                }
                current = RISCV64;
            } else {
                current = is64Bit ? X64 : X86;
            }
        } else {
            current = is64Bit ? ARM64 : ARM32;
        }
    }
}