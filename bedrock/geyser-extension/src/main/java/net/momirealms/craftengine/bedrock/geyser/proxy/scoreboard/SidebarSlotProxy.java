package net.momirealms.craftengine.bedrock.geyser.proxy.scoreboard;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.geysermc.geyser.scoreboard.display.score.SidebarDisplayScore;

import java.util.List;

@ReflectionProxy(name = "org.geysermc.geyser.scoreboard.display.slot.SidebarDisplaySlot")
public interface SidebarSlotProxy {
    SidebarSlotProxy INSTANCE = ASMProxyFactory.create(SidebarSlotProxy.class);

    @FieldGetter(name = "displayScores")
    List<SidebarDisplayScore> scores(Object slot);
}
