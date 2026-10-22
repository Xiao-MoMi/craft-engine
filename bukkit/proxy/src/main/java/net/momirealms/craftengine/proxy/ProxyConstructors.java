package net.momirealms.craftengine.proxy;

import java.lang.invoke.MethodHandle;
import net.momirealms.sparrow.reflection.constructor.SConstructor2;
import net.momirealms.sparrow.reflection.constructor.SparrowConstructor;
import net.momirealms.sparrow.reflection.exception.SparrowReflectionException;

public final class ProxyConstructors {

    private ProxyConstructors() {
    }

    // sparrow 的 ASM 访问器定义在目标类的类加载器里，混合端（Mohist/Youer）的 NMS 加载器看不见插件私有的
    // sparrow，这类目标只能退回 MethodHandle
    public static SConstructor2 asm$2(final SparrowConstructor<?> constructor) {
        if (!constructor.isAsmSupported()) {
            MethodHandle methodHandle = constructor.unreflect();
            return new SConstructor2() {
                @Override
                public Object newInstance(Object arg0, Object arg1) {
                    try {
                        return methodHandle.invoke(arg0, arg1);
                    } catch (Throwable throwable) {
                        throw new SparrowReflectionException("Failed to call constructor", throwable);
                    }
                }
            };
        }
        return constructor.asm$2();
    }
}
