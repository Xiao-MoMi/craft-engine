package net.momirealms.craftengine.bedrock.geyser.scoreboard;

import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SidebarCommand {
    private final Map<GeyserSession, SidebarTestBoard> tests = new ConcurrentHashMap<>();
    private final CraftEngineGeyserExtension extension;

    public SidebarCommand(CraftEngineGeyserExtension extension) {
        this.extension = extension;
    }

    public void register(GeyserDefineCommandsEvent event) {
        event.register(Command.<CommandSource>builder(this.extension)
                .name("sidebar")
                .description("Inspect or test CraftEngine sidebar rendering")
                .permission("craftengine.command.sidebar")
                .source(CommandSource.class)
                .executor((source, command, args) -> this.execute(source, args))
                .build());
    }

    private void execute(CommandSource source, String[] args) {
        if (this.extension.sidebarManager() == null) {
            source.sendMessage("Sidebar support is disabled. Regenerate the pack and cedata, then restart Geyser.");
            return;
        }
        String action = args.length == 0 ? "inspect" : args[0];
        GeyserConnection target = source.connection();
        if (args.length > 1) {
            target = null;
            for (GeyserConnection connection : this.extension.geyserApi().onlineConnections()) {
                if (connection.name().equalsIgnoreCase(args[1])) {
                    target = connection;
                    break;
                }
            }
        }
        if (!(target instanceof GeyserSession session)) {
            source.sendMessage("Usage: /craftengine sidebar <inspect|test|update|clear> [Bedrock player]");
            return;
        }
        session.executeInEventLoop(() -> {
            try {
                switch (action) {
                    case "inspect" -> source.sendMessage(this.extension.sidebarManager().session(session).describe());
                    case "test" -> {
                        if (this.tests.containsKey(session)) {
                            source.sendMessage("Clear the active test first.");
                            return;
                        }
                        SidebarTestBoard test = new SidebarTestBoard(session);
                        this.tests.put(session, test);
                        test.show(session);
                        source.sendMessage("Test sidebar shown. Custom graphics require the scoreboard-test fixture pack.");
                    }
                    case "update" -> {
                        SidebarTestBoard test = this.tests.get(session);
                        if (test != null) {
                            test.update(session);
                        }
                    }
                    case "clear" -> {
                        SidebarTestBoard test = this.tests.remove(session);
                        if (test != null) {
                            test.clear(session);
                        }
                    }
                    default -> source.sendMessage("Usage: /craftengine sidebar <inspect|test|update|clear> [Bedrock player]");
                }
            } catch (RuntimeException exception) {
                source.sendMessage("Sidebar command failed: " + exception.getMessage());
            }
        });
    }

    public void disconnect(Object session) {
        this.tests.remove(session);
    }
}
