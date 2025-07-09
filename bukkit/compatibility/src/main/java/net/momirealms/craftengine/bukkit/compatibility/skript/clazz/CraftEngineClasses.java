package net.momirealms.craftengine.bukkit.compatibility.skript.clazz;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.yggdrasil.Fields;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UnsafeBlockStateMatcher;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;
import org.jetbrains.annotations.Nullable;

import java.io.StreamCorruptedException;

public class CraftEngineClasses {

    public static void register() {
        Classes.registerClass(new ClassInfo<>(ImmutableBlockState.class, "customblockstate")
                .user("custom block state")
                .name("Custom Block State")
                .serializer(new Serializer<>() {
                    @Override
                    public Fields serialize(ImmutableBlockState o) {
                        Fields f = new Fields();
                        f.putObject("customblockstate", o.toString());
                        return f;
                    }

                    @Override
                    public void deserialize(ImmutableBlockState o, Fields f) {
                    }

                    @Override
                    public ImmutableBlockState deserialize(Fields f) throws StreamCorruptedException {
                        String data = f.getObject("customblockstate", String.class);
                        assert data != null;
                        try {
                            return BlockStateParser.deserialize(data);
                        } catch (IllegalArgumentException ex) {
                            throw new StreamCorruptedException("Invalid block data: " + data);
                        }
                    }

                    @Override
                    public boolean mustSyncDeserialization() {
                        return true;
                    }

                    @Override
                    protected boolean canBeInstantiated() {
                        return false;
                    }
                })
                .parser(new Parser<>() {
                    @Override
                    public String toString(ImmutableBlockState o, int flags) {
                        return o.toString();
                    }

                    @Override
                    public String toVariableNameString(ImmutableBlockState o) {
                        return "customblockstate:" + o.toString();
                    }

                    @Override
                    public @Nullable ImmutableBlockState parse(String s, ParseContext context) {
                        return BlockStateParser.deserialize(s);
                    }
                })
        );

        Classes.registerClass(new ClassInfo<>(UnsafeBlockStateMatcher.class, "unsafeblockstatematcher")
                .user("unsafe block state matcher")
                .name("Unsafe Block State Matcher")
                .serializer(new Serializer<>() {
                    @Override
                    public Fields serialize(UnsafeBlockStateMatcher o) {
                        Fields f = new Fields();
                        f.putObject("unsafeblockstatematcher", o.toString());
                        return f;
                    }

                    @Override
                    public void deserialize(UnsafeBlockStateMatcher o, Fields f) {
                    }

                    @Override
                    public UnsafeBlockStateMatcher deserialize(Fields f) throws StreamCorruptedException {
                        String data = f.getObject("unsafeblockstatematcher", String.class);
                        assert data != null;
                        try {
                            return UnsafeBlockStateMatcher.deserialize(data);
                        } catch (IllegalArgumentException ex) {
                            throw new StreamCorruptedException("Invalid block matcher: " + data);
                        }
                    }

                    @Override
                    public boolean mustSyncDeserialization() {
                        return true;
                    }

                    @Override
                    protected boolean canBeInstantiated() {
                        return false;
                    }
                })
                .parser(new Parser<>() {
                    @Override
                    public String toString(UnsafeBlockStateMatcher o, int flags) {
                        return o.toString();
                    }

                    @Override
                    public String toVariableNameString(UnsafeBlockStateMatcher o) {
                        return "unsafeblockstatematcher:" + o.toString();
                    }

                    @Override
                    public @Nullable UnsafeBlockStateMatcher parse(String s, ParseContext context) {
                        return UnsafeBlockStateMatcher.deserialize(s);
                    }
                })
        );
    }
}
