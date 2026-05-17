package net.momirealms.craftengine.core.plugin.config.yaml;

import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.resolver.ScalarResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class DoubleSensitiveScalarResolver implements ScalarResolver {
    public static final Pattern EMPTY = Pattern.compile("^$");
    public static final Pattern ENV_FORMAT = Pattern.compile("^\\$\\{\\s*(?:(\\w+)(?:(:?[-?])(\\w+)?)?)\\s*\\}$");
    public static final Pattern BOOL = Pattern.compile("^(?:true|True|TRUE|false|False|FALSE)$");
    public static final Pattern FLOAT =
            Pattern.compile("^([-+]?(\\.[0-9]+|[0-9]+(\\.[0-9]*)?)([eE][-+]?[0-9]+)?)" +
                    "|([-+]?\\.(?:inf|Inf|INF))" +
                    "|(\\.(?:nan|NaN|NAN))$");
    public static final Pattern MERGE = Pattern.compile("^(?:<<)$");
    public static final Pattern INT = Pattern.compile("^([-+]?[0-9]+)" +
            "|(0o[0-7]+)" +
            "|(0x[0-9a-fA-F]+)$"
    );
    public static final Pattern NULL = Pattern.compile("^(?:~|null|Null|NULL| )$");
    public static final Tag TAG_DOUBLE = new Tag(Tag.PREFIX + "double");
    public static final DoubleSensitiveScalarResolver SUPPORT_MERGE = new DoubleSensitiveScalarResolver(true);

    private final Map<Character, List<ResolverTuple>> yamlImplicitResolvers = new HashMap<>();

    public DoubleSensitiveScalarResolver(boolean supportMerge) {
        if (supportMerge) {
            addImplicitResolver(Tag.MERGE, MERGE, "<");
        }
        addImplicitResolver(Tag.NULL, EMPTY, null);
        addImplicitResolver(Tag.BOOL, BOOL, "tfTF");
        addImplicitResolver(Tag.INT, INT, "-+0123456789");
        addImplicitResolver(TAG_DOUBLE, FLOAT, "-+0123456789.");
        addImplicitResolver(Tag.NULL, NULL, "n\u0000");
        addImplicitResolver(Tag.ENV_TAG, ENV_FORMAT, "$");
    }

    public void addImplicitResolver(Tag tag, Pattern regexp, String first) {
        if (regexp == null) {
            System.out.println(first);
        }
        if (first == null) {
            List<ResolverTuple> curr =
                    yamlImplicitResolvers.computeIfAbsent(null, c -> new ArrayList<>());
            curr.add(new ResolverTuple(tag, regexp));
        } else {
            char[] chrs = first.toCharArray();
            for (int i = 0, j = chrs.length; i < j; i++) {
                Character theC = Character.valueOf(chrs[i]);
                if (theC == 0) {
                    theC = null;
                }
                List<ResolverTuple> curr = yamlImplicitResolvers.computeIfAbsent(theC, k -> new ArrayList<>());
                curr.add(new ResolverTuple(tag, regexp));
            }
        }
    }

    @Override
    public Tag resolve(String value, Boolean implicit) {
        if (!implicit) {
            return Tag.STR;
        }
        final List<ResolverTuple> resolvers;
        if (value.isEmpty()) {
            resolvers = yamlImplicitResolvers.get('\0');
        } else {
            resolvers = yamlImplicitResolvers.get(value.charAt(0));
        }
        if (resolvers != null) {
            for (ResolverTuple v : resolvers) {
                Tag tag = v.tag();
                Pattern regexp = v.regexp();
                if (regexp.matcher(value).matches()) {
                    return tag;
                }
            }
        }
        if (yamlImplicitResolvers.containsKey(null)) {
            for (ResolverTuple v : yamlImplicitResolvers.get(null)) {
                Tag tag = v.tag();
                Pattern regexp = v.regexp();
                if (regexp.matcher(value).matches()) {
                    return tag;
                }
            }
        }
        return Tag.STR;
    }

    public record ResolverTuple(Tag tag, Pattern regexp) {
    }
}