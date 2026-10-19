package net.momirealms.craftengine.core.plugin.network.protocol.recipe.legacy;

/**
 * 配方序列化器不在 CraftEngine 已知列表里（mod 自己注册的配方类型），
 * 此时无法继续读取后续的字节，只能把整个数据包原样发给客户端。
 */
public final class UnknownRecipeTypeException extends RuntimeException {
    public UnknownRecipeTypeException(String message) {
        super(message);
    }
}
