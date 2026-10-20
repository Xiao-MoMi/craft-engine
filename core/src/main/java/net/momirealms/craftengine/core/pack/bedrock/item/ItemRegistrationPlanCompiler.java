package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemBehaviorDescription.Component;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemBehaviorDescription.ComponentSource;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationPlan.Diagnostic;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationPlan.Entry;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationPlan.Severity;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.Key;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ItemRegistrationPlanCompiler {
    public static final String TARGET_API = "2.12.0-SNAPSHOT";
    // 与 Geyser 适配层可转换的组件保持一致，用于提前发现无法注册的组件修改。
    private static final Set<Key> SUPPORTED_COMPONENTS = Set.of(
            DataComponentKeys.CONSUMABLE,
            DataComponentKeys.FOOD,
            DataComponentKeys.EQUIPPABLE,
            DataComponentKeys.MAX_DAMAGE,
            DataComponentKeys.MAX_STACK_SIZE,
            DataComponentKeys.USE_COOLDOWN,
            DataComponentKeys.ENCHANTABLE,
            DataComponentKeys.TOOL,
            DataComponentKeys.REPAIRABLE,
            DataComponentKeys.ENCHANTMENT_GLINT_OVERRIDE,
            DataComponentKeys.ATTACK_RANGE,
            DataComponentKeys.KINETIC_WEAPON,
            DataComponentKeys.PIERCING_WEAPON,
            DataComponentKeys.SWING_ANIMATION,
            DataComponentKeys.ATTACK_ANIMATION,
            DataComponentKeys.USE_EFFECTS,
            DataComponentKeys.COMPOSTABLE,
            DataComponentKeys.COOKING_FUEL
    );

    private ItemRegistrationPlanCompiler() {
    }

    public static ItemRegistrationPlan compile(Collection<ResolvedItemBinding> bindings) {
        // 当前以一个物品模型定义关联一个实际物品为前提，每个绑定直接生成一条注册记录。
        List<Entry> entries = new ArrayList<>(bindings.size());
        List<Diagnostic> diagnostics = new ObjectArrayList<>();
        for (ResolvedItemBinding binding : bindings) {
            if (binding.definitionId() == null || binding.definition() == null) {
                diagnostics.add(diagnostic(binding, Severity.INFO, "UNBOUND_DEFINITION", List.of("No collected definition was matched")));
                continue;
            }
            Map<Key, JsonElement> overrides = new LinkedHashMap<>();
            Set<Key> removed = new LinkedHashSet<>();
            // 完整快照保留在 properties 中；另外提取相对原版默认值的覆写和显式移除。
            for (Map.Entry<Key, Component> component : binding.clientProperties().components().entrySet()) {
                if (component.getValue().source() == ComponentSource.OVERRIDE) {
                    overrides.put(component.getKey(), component.getValue().value().deepCopy());
                } else if (component.getValue().source() == ComponentSource.REMOVED) {
                    removed.add(component.getKey());
                }
            }
            // 固定签名字段顺序，让相同注册属性生成稳定的 Bedrock 标识符。
            String signature = GsonHelper.sortObjectKeys(signature(binding)).toString();
            String token = HashUtils.sha1(signature.getBytes(StandardCharsets.UTF_8));
            Key bedrockId = Key.ce("item_" + token);
            Entry entry = new Entry(bedrockId, "craftengine.item_" + token, binding.clientItemType(), binding.definitionId(), binding.definition(), binding.clientProperties(), overrides, removed, binding);
            entries.add(entry);
            validate(entry, diagnostics);
        }
        // 固定输出顺序，便于比较多次生成的计划和诊断。
        entries.sort(Comparator.comparing(entry -> entry.bedrockId().asString()));
        diagnostics.sort(Comparator.comparing(Diagnostic::code)
                .thenComparing(diagnostic -> diagnostic.javaItemType().asString())
                .thenComparing(diagnostic -> Objects.toString(diagnostic.definitionId(), "")));
        return new ItemRegistrationPlan(entries, diagnostics);
    }

    private static JsonObject signature(ResolvedItemBinding binding) {
        // 使用 toClientSide 后的类型、组件和行为；CE 物品 ID 只作为来源，不参与注册身份。
        JsonObject signature = new JsonObject();
        signature.addProperty("java_item_type", binding.clientItemType().asString());
        signature.addProperty("definition_id", binding.definitionId().asString());
        // 同时保留组件值和来源状态，区分默认、覆写、移除与缺失。
        for (Map.Entry<Key, Component> component : binding.clientProperties().components().entrySet()) {
            signature.add("components." + component.getKey().asString(), GsonHelper.get().toJsonTree(component.getValue()));
        }
        signature.add("behavior", staticBehavior(binding.clientBehavior()));
        return signature;
    }

    private static JsonObject staticBehavior(ItemBehaviorDescription behavior) {
        // 只提取静态使用和挥动行为，使用时长、蓄力时长等运行时描述不参与此比较。
        JsonObject result = new JsonObject();
        result.addProperty("use_animation", behavior.use().animation().name());
        result.addProperty("native_use_on_release", behavior.use().nativeUseOnRelease());
        result.add("attack", GsonHelper.get().toJsonTree(behavior.attack()));
        result.add("interact", GsonHelper.get().toJsonTree(behavior.interact()));
        return result;
    }

    private static void validate(Entry entry, List<Diagnostic> diagnostics) {
        ResolvedItemBinding binding = entry.source();
        // 未支持的原版默认组件可继续继承；显式修改必须能够转换，交互动画则保留为资源需求警告。
        for (Map.Entry<Key, Component> component : entry.properties().components().entrySet()) {
            ComponentSource source = component.getValue().source();
            if (source != ComponentSource.OVERRIDE && source != ComponentSource.REMOVED) {
                continue;
            }
            if (!SUPPORTED_COMPONENTS.contains(component.getKey())) {
                boolean animation = component.getKey().equals(DataComponentKeys.INTERACT_ANIMATION);
                diagnostics.add(diagnostic(
                                binding,
                                animation ? Severity.WARNING : Severity.ERROR,
                                animation ? "ANIMATION_RESOURCE_REQUIRED" : "UNSUPPORTED_COMPONENT",
                                List.of(component.getKey().asString(), source.name())
                ));
            }
        }
        ItemBehaviorDescription client = binding.clientBehavior();
        if (!client.attack().equals(client.interact()) || client.attack().animation() != ItemBehaviorDescription.SwingAnimation.WHACK) {
            diagnostics.add(diagnostic(
                    binding,
                    Severity.WARNING,
                    "SWING_NOT_FULLY_EXPRESSIBLE",
                    List.of("The target API exposes attack duration, not attack types or independent interact animations")
            ));
        }
        // 保留类型投影提示，便于追踪客户端最终使用的原版物品类型。
        if (!binding.serverItemType().equals(binding.clientItemType())) {
            diagnostics.add(diagnostic(
                    binding,
                    Severity.WARNING,
                    "CLIENT_ITEM_TYPE_CHANGED",
                    List.of(binding.serverItemType().asString(), binding.clientItemType().asString())
            ));
        }
    }

    private static Diagnostic diagnostic(ResolvedItemBinding binding, Severity severity, String code, List<String> details) {
        return new Diagnostic(severity, code, binding.clientItemType(), binding.definitionId(), List.of(binding.itemId()), details);
    }
}
