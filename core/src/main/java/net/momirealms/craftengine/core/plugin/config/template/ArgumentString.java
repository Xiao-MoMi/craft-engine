package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.plugin.config.template.argument.TemplateArgument;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.TagParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ArgumentString {
    String rawValue();

    Object get(Map<String, TemplateArgument> arguments);

    final class Literal implements ArgumentString {
        private final String value;

        public Literal(String value) {
            this.value = value;
        }

        public static Literal literal(String value) {
            return new Literal(value);
        }

        @Override
        public String rawValue() {
            return this.value;
        }

        @Override
        public Object get(Map<String, TemplateArgument> arguments) {
            return this.value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Literal literal)) return false;
            return this.value.equals(literal.value);
        }

        @Override
        public int hashCode() {
            return this.value.hashCode();
        }

        @Override
        public String toString() {
            return "Literal(" + this.value + ")";
        }
    }

    final class Placeholder implements ArgumentString {
        private final String placeholder;
        private final String rawText;
        private final Object defaultValue;
        private final boolean hasDefaultValue;

        public Placeholder(String placeholderContent) {
            this.rawText = "${" + placeholderContent + "}";
            int separatorIndex = placeholderContent.indexOf(":-");
            if (separatorIndex == -1) {
                this.placeholder = placeholderContent;
                this.defaultValue = null;
                this.hasDefaultValue = false;
            } else {
                this.placeholder = placeholderContent.substring(0, separatorIndex);
                String defaultValueString = placeholderContent.substring(separatorIndex + 2);
                Object parsed;
                try {
                    parsed = TagParser.parseObjectFully(defaultValueString);
                } catch (Exception e) {
                    throw new LocalizedResourceConfigException("warning.config.type.snbt.invalid_syntax", e.getMessage());
                }
                try {
                    this.defaultValue = ((TemplateManagerImpl) TemplateManager.INSTANCE).preprocessUnknownValue(parsed);
                } catch (LocalizedResourceConfigException e) {
                    e.appendTailArgument(this.placeholder);
                    throw e;
                }
                this.hasDefaultValue = true;
            }
        }

        public static Placeholder placeholder(String placeholder) {
            return new Placeholder(placeholder);
        }

        @Override
        public Object get(Map<String, TemplateArgument> arguments) {
            TemplateArgument replacement = arguments.get(this.placeholder);
            if (replacement != null) {
                return replacement.get(arguments);
            }
            if (this.hasDefaultValue) {
                if (this.defaultValue == null) {
                    return null;
                }
                return ((TemplateManagerImpl) TemplateManager.INSTANCE).processUnknownValue(this.defaultValue, arguments);
            }
            throw new LocalizedResourceConfigException("warning.config.template.argument.missing_value", this.rawText);
        }

        @Override
        public String rawValue() {
            return this.rawText;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Placeholder that)) return false;
            return this.placeholder.equals(that.placeholder);
        }

        @Override
        public int hashCode() {
            return this.placeholder.hashCode();
        }

        @Override
        public String toString() {
            return "Placeholder(" + this.placeholder + ")";
        }
    }

    final class Complex2 implements ArgumentString {
        private final String rawText;
        private final ArgumentString arg1;
        private final ArgumentString arg2;

        public Complex2(String rawText, ArgumentString arg1, ArgumentString arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.rawText = rawText;
        }

        @Override
        public Object get(Map<String, TemplateArgument> arguments) {
            Object arg1 = this.arg1.get(arguments);
            Object arg2 = this.arg2.get(arguments);
            if (arg1 == null && arg2 == null) return null;
            if (arg1 == null) return String.valueOf(arg2);
            if (arg2 == null) return String.valueOf(arg1);
            return String.valueOf(arg1) + arg2;
        }

        @Override
        public String rawValue() {
            return this.rawText;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Complex2 that)) return false;
            return this.rawText.equals(that.rawText);
        }

        @Override
        public int hashCode() {
            return this.rawText.hashCode();
        }

        @Override
        public String toString() {
            return "Complex2(" + this.rawText + ")";
        }
    }

    final class Complex3 implements ArgumentString {
        private final String rawText;
        private final ArgumentString arg1;
        private final ArgumentString arg2;
        private final ArgumentString arg3;

        public Complex3(String rawText, ArgumentString arg1, ArgumentString arg2, ArgumentString arg3) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.rawText = rawText;
        }

        @Override
        public Object get(Map<String, TemplateArgument> arguments) {
            Object arg1 = this.arg1.get(arguments);
            Object arg2 = this.arg2.get(arguments);
            Object arg3 = this.arg3.get(arguments);
            StringBuilder builder = new StringBuilder();
            if (arg1 != null) {
                builder.append(arg1);
            }
            if (arg2 != null) {
                builder.append(arg2);
            }
            if (arg3 != null) {
                builder.append(arg3);
            }
            return builder.toString();
        }

        @Override
        public String rawValue() {
            return this.rawText;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Complex2 that)) return false;
            return this.rawText.equals(that.rawText);
        }

        @Override
        public int hashCode() {
            return this.rawText.hashCode();
        }

        @Override
        public String toString() {
            return "Complex2(" + this.rawText + ")";
        }
    }

    final class Complex implements ArgumentString {
        private final ArgumentString[] parts;
        private final String rawText;

        public Complex(String rawText, ArgumentString[] parts) {
            this.parts = parts;
            this.rawText = rawText;
        }

        @Override
        public Object get(Map<String, TemplateArgument> arguments) {
            StringBuilder result = new StringBuilder();
            boolean hasValue = false;
            for (ArgumentString part : this.parts) {
                Object arg = part.get(arguments);
                if (arg != null) {
                    result.append(arg);
                    hasValue = true;
                }
            }
            if (!hasValue) return null;
            return result.toString();
        }

        @Override
        public String rawValue() {
            return this.rawText;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Complex that)) return false;
            return this.rawText.equals(that.rawText);
        }

        @Override
        public int hashCode() {
            return this.rawText.hashCode();
        }

        @Override
        public String toString() {
            return "Complex(" + this.rawText + ")";
        }
    }

    static ArgumentString preParse(String input) {
        if (input == null || input.isEmpty()) {
            return Literal.literal("");
        }

        List<ArgumentString> arguments = new ArrayList<>();
        StringBuilder currentLiteral = new StringBuilder();
        final int n = input.length();
        int i = 0;

        while (i < n) {
            char c = input.charAt(i);

            // --- 1. 优先检测占位符触发器 ---
            if (c == '$' && i + 1 < n && input.charAt(i + 1) == '{') {

                // a. 提交之前的普通文本
                if (!currentLiteral.isEmpty()) {
                    arguments.add(Literal.literal(currentLiteral.toString()));
                    currentLiteral.setLength(0);
                }

                // b. 解析占位符内部，此处的逻辑拥有自己的转义规则
                int contentStartIndex = i + 2;
                StringBuilder keyBuilder = new StringBuilder();
                int depth = 1;
                int j = contentStartIndex;
                boolean foundMatch = false;

                while (j < n) {
                    char innerChar = input.charAt(j);

                    // --- 占位符内部的转义逻辑 ---
                    if (innerChar == '\\') {
                        if (j + 1 < n && (input.charAt(j + 1) == '{' || input.charAt(j + 1) == '}')) {
                            keyBuilder.append(input.charAt(j + 1));
                            j += 2;
                        } else {
                            // 在占位符内部，一个无法识别的转义\依旧被当作普通\处理
                            keyBuilder.append(innerChar);
                            j++;
                        }
                    } else if (innerChar == '{') {
                        depth++;
                        keyBuilder.append(innerChar);
                        j++;
                    } else if (innerChar == '}') {
                        depth--;
                        if (depth == 0) { // 找到匹配的闭合括号
                            arguments.add(Placeholder.placeholder(keyBuilder.toString()));
                            i = j + 1;
                            foundMatch = true;
                            break;
                        }
                        keyBuilder.append(innerChar);
                        j++;
                    } else {
                        keyBuilder.append(innerChar);
                        j++;
                    }
                }

                if (foundMatch) {
                    continue;
                } else {
                    // 未找到闭合括号，将 '$' 视为普通字符
                    currentLiteral.append(c);
                    i++;
                }
            }
            // --- 2. 其次，只处理对触发器'$'的转义 ---
            else if (c == '\\' && i + 1 < n && input.charAt(i + 1) == '$') {
                currentLiteral.append('$'); // 直接添加 '$'
                i += 2; // 跳过 '\' 和 '$'
            }
            // --- 3. 处理所有其他字符（包括独立的'\'和'{'）为普通文本 ---
            else {
                currentLiteral.append(c);
                i++;
            }
        }

        if (!currentLiteral.isEmpty()) {
            arguments.add(Literal.literal(currentLiteral.toString()));
        }

        return switch (arguments.size()) {
            case 0 -> Literal.literal("");
            case 1 -> arguments.getFirst();
            case 2 -> new Complex2(input, arguments.get(0), arguments.get(1));
            case 3 -> new Complex3(input, arguments.get(0), arguments.get(1), arguments.get(2));
            default -> new Complex(input, arguments.toArray(new ArgumentString[0]));
        };
    }
}
