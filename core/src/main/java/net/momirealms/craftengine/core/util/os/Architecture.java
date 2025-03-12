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