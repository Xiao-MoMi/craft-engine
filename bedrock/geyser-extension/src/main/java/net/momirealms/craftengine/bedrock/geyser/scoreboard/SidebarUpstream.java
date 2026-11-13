package net.momirealms.craftengine.bedrock.geyser.scoreboard;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.matcher.ElementMatchers;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveObjectivePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetDisplayObjectivePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetScorePacket;
import org.geysermc.geyser.session.UpstreamSession;

import java.lang.reflect.Constructor;
import java.util.function.Consumer;

public final class SidebarUpstream {
    private static final Constructor<? extends UpstreamSession> CONSTRUCTOR = constructor();
    private final UpstreamSession original;
    private final SidebarSession sidebar;
    private final Consumer<BedrockPacket> queued;
    private final Consumer<BedrockPacket> immediate;

    public SidebarUpstream(UpstreamSession original, SidebarSession sidebar) {
        this.original = original;
        this.sidebar = sidebar;
        this.queued = original::sendPacket;
        this.immediate = original::sendPacketImmediately;
    }

    public UpstreamSession wrap() {
        try {
            return CONSTRUCTOR.newInstance(this.original.getSession(), this.original, this);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot install sidebar packet output", exception);
        }
    }

    public void send(@Argument(0) BedrockPacket packet) {
        if (packet instanceof SetScorePacket || packet instanceof SetDisplayObjectivePacket || packet instanceof RemoveObjectivePacket) {
            this.sidebar.send(packet, this.queued);
        } else {
            this.original.sendPacket(packet);
        }
    }

    public void sendImmediately(@Argument(0) BedrockPacket packet) {
        if (packet instanceof SetScorePacket || packet instanceof SetDisplayObjectivePacket || packet instanceof RemoveObjectivePacket) {
            this.sidebar.send(packet, this.immediate);
        } else {
            this.original.sendPacketImmediately(packet);
        }
    }

    private static Constructor<? extends UpstreamSession> constructor() {
        try {
            return new ByteBuddy(ClassFileVersion.JAVA_V21)
                    .subclass(UpstreamSession.class)
                    .name(SidebarUpstream.class.getName() + "$Generated")
                    .defineField("original", UpstreamSession.class, Visibility.PRIVATE)
                    .defineField("sidebar", SidebarUpstream.class, Visibility.PRIVATE)
                    .defineConstructor(Visibility.PUBLIC)
                    .withParameters(BedrockServerSession.class, UpstreamSession.class, SidebarUpstream.class)
                    .intercept(MethodCall.invoke(UpstreamSession.class.getConstructor(BedrockServerSession.class)).withArgument(0)
                            .andThen(FieldAccessor.ofField("original").setsArgumentAt(1))
                            .andThen(FieldAccessor.ofField("sidebar").setsArgumentAt(2)))
                    .method(ElementMatchers.isDeclaredBy(UpstreamSession.class))
                    .intercept(MethodDelegation.toField("original"))
                    .method(ElementMatchers.named("sendPacket"))
                    .intercept(MethodDelegation.withDefaultConfiguration().filter(ElementMatchers.named("send")).toField("sidebar"))
                    .method(ElementMatchers.named("sendPacketImmediately"))
                    .intercept(MethodDelegation.withDefaultConfiguration().filter(ElementMatchers.named("sendImmediately")).toField("sidebar"))
                    .make().load(SidebarUpstream.class.getClassLoader()).getLoaded()
                    .getConstructor(BedrockServerSession.class, UpstreamSession.class, SidebarUpstream.class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
