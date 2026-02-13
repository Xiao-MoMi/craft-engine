package net.momirealms.craftengine.proxy;

import java.util.List;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MinecraftPredicate implements Predicate<String> {
    // [修改点 1] 正则表达式：增加了 "!" 作为捕获组，并在非操作符匹配中排除了 "!"
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\s*(\\(|\\)|&&|\\|\\||!|[^\\s()&|!]+)\\s*");
    private final Context context;

    public MinecraftPredicate(String version, List<String> patches) {
        this.context = new Context(parseVersionToInteger(version), patches);
    }

    @Override
    public boolean test(String expression) {
        if (expression == null || expression.isEmpty()) return true;
        return compile(expression).test(this.context);
    }

    private Condition compile(String expression) {
        Matcher matcher = TOKEN_PATTERN.matcher(expression);
        Stack<Condition> nodes = new Stack<>();
        Stack<String> ops = new Stack<>();
        while (matcher.find()) {
            String token = matcher.group(1);
            if (token.isEmpty()) continue;
            switch (token) {
                case "(", "!" -> ops.push(token);
                case ")" -> {
                    while (!ops.isEmpty() && !ops.peek().equals("(")) {
                        processOperator(nodes, ops.pop());
                    }
                    if (!ops.isEmpty()) ops.pop(); // 弹出 "("
                }
                case "&&", "||" -> {
                    while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(token)) {
                        processOperator(nodes, ops.pop());
                    }
                    ops.push(token);
                }
                default -> nodes.push(compileLeaf(token));
            }
        }
        while (!ops.isEmpty()) {
            processOperator(nodes, ops.pop());
        }
        return nodes.isEmpty() ? ctx -> true : nodes.pop();
    }

    private static void processOperator(Stack<Condition> nodes, String op) {
        if ("!".equals(op)) {
            if (nodes.isEmpty()) throw new IllegalArgumentException("Invalid syntax: '!' used without operand");
            Condition node = nodes.pop();
            nodes.push(ctx -> !node.test(ctx));
        } else {
            if (nodes.size() < 2) throw new IllegalArgumentException("Invalid syntax: missing operands for " + op);
            Condition right = nodes.pop();
            Condition left = nodes.pop();
            if ("&&".equals(op)) {
                nodes.push(ctx -> left.test(ctx) && right.test(ctx));
            } else if ("||".equals(op)) {
                nodes.push(ctx -> left.test(ctx) || right.test(ctx));
            }
        }
    }

    private static int precedence(String op) {
        if ("!".equals(op)) return 3;
        if ("&&".equals(op)) return 2;
        if ("||".equals(op)) return 1;
        return 0;
    }

    private static Condition compileLeaf(String token) {
        String[] parts = token.split("=", 2);
        if (parts.length != 2) return ctx -> false;
        String type = parts[0].trim();
        String param = parts[1].trim();
        return switch (type) {
            case "min_version" -> new VersionCheck(param, true);
            case "max_version" -> new VersionCheck(param, false);
            case "version" -> new ExactVersionCheck(param);
            case "has_patch" -> new PatchCheck(param);
            default -> throw new IllegalArgumentException("Invalid predicate: " + token);
        };
    }

    public static int parseVersionToInteger(String versionString) {
        int v1 = 0;
        int v2 = 0;
        int v3 = 0;
        int currentNumber = 0;
        int part = 0;
        for (int i = 0; i < versionString.length(); i++) {
            char c = versionString.charAt(i);
            if (c >= '0' && c <= '9') {
                currentNumber = currentNumber * 10 + (c - '0');
            } else if (c == '.') {
                if (part == 0) {
                    v1 = currentNumber;
                }
                if (part == 1) {
                    v2 = currentNumber;
                }
                part++;
                currentNumber = 0;
                if (part > 2) {
                    break;
                }
            }
        }
        if (part == 0) {
            v1 = currentNumber;
        } else if (part == 1) {
            v2 = currentNumber;
        } else if (part == 2) {
            v3 = currentNumber;
        }
        return v1 * 10000 + v2 * 100 + v3;
    }

    public interface Condition {
        boolean test(Context predicate);
    }

    public record Context(int version, List<String> patches) {
    }

    private static class ExactVersionCheck implements Condition {
        private final int targetVersion;

        public ExactVersionCheck(String version) {
            this.targetVersion = parseVersionToInteger(version);
        }

        @Override
        public boolean test(Context predicate) {
            return predicate.version == targetVersion;
        }
    }

    private static class VersionCheck implements Condition {
        private final int targetVersion;
        private final boolean minOrMax;

        public VersionCheck(String targetVersion, boolean minOrMax) {
            this.targetVersion = parseVersionToInteger(targetVersion);
            this.minOrMax = minOrMax;
        }

        @Override
        public boolean test(Context context) {
            if (this.minOrMax) {
                return context.version >= this.targetVersion;
            } else {
                return context.version <= this.targetVersion;
            }
        }
    }

    private record PatchCheck(String patch) implements Condition {

        @Override
        public boolean test(Context predicate) {
            return predicate.patches().contains(this.patch);
        }
    }
}