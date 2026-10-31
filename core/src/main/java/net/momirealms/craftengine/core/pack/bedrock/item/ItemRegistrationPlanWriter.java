package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class ItemRegistrationPlanWriter {

    private ItemRegistrationPlanWriter() {
    }

    public static void write(ItemRegistrationPlan plan, Path path, BedrockItemIcons.Result generated) throws IOException {
        Files.writeString(path, toJson(plan, generated).toString());
    }

    public static JsonObject toJson(ItemRegistrationPlan plan, BedrockItemIcons.Result generated) {
        JsonObject result = new JsonObject();
        result.addProperty(KIND, RegistrationPlanKind.ITEM.id());
        result.addProperty(FORMAT_VERSION, RegistrationPlanKind.ITEM.formatVersion()); // 计划格式版本
        result.addProperty(SOURCE_VERSION, VersionHelper.MINECRAFT_VERSION.version()); // 来源 Java 版本
        JsonArray entries = new JsonArray();
        for (ItemRegistrationPlan.Entry entry : plan.entries()) {
            JsonObject json = new JsonObject();
            ResolvedItemBinding binding = entry.source(); // 物品关联结果
            json.addProperty(BEDROCK_IDENTIFIER, entry.bedrockId().asString()); // 基岩物品 ID
            json.addProperty(ITEM_TYPE, entry.javaItemType().asString()); // 基础 Java 物品类型
            json.addProperty(ITEM_ID, binding.itemId().asString()); // 来源 CE 物品 ID
            json.addProperty(MODEL, entry.definitionId().asString()); // 匹配的 item_model ID
            JsonObject components = new JsonObject(); // 相对原版的组件差异
            entry.properties().components().forEach((key, value) -> components.add(key.asString(), value)); // null 表示移除
            if (!components.isEmpty()) {
                json.add(COMPONENTS, components); // 注册组件
            }
            json.add(VARIANTS, variantsToJson(entry, generated)); // 全部分支
            JsonObject behavior = new JsonObject(); // Geyser 所需行为
            behavior.add("use", GsonHelper.get().toJsonTree(binding.clientBehavior().use())); // 使用行为
            if (binding.placesBlock()) {
                behavior.addProperty(PLACES_BLOCK, true);
            }
            json.add(CLIENT_BEHAVIOR, behavior); // 客户端行为
            entries.add(json); // 加入注册列表
        }
        result.add(ENTRIES, entries); // 全部注册条目
        JsonArray diagnostics = new JsonArray(); // 诊断列表
        for (ItemRegistrationPlan.Diagnostic diagnostic : plan.diagnostics()) { // 逐项写入诊断
            JsonObject json = new JsonObject(); // 当前条目
            json.addProperty(SEVERITY, diagnostic.severity().name()); // 诊断级别
            json.addProperty(CODE, diagnostic.code()); // 诊断代码
            json.addProperty(ITEM_TYPE, diagnostic.javaItemType().asString()); // 基础 Java 物品类型
            if (diagnostic.definitionId() != null) { // 有模型 ID 时记录
                json.addProperty(MODEL, diagnostic.definitionId().asString()); // 匹配的 item_model ID
            }
            JsonArray items = new JsonArray(); // 涉及的物品列表
            diagnostic.itemIds().forEach(id -> items.add(id.asString())); // 写入物品 ID
            json.add(ITEM_IDS, items); // 涉及的 CE 物品 ID
            json.add(DETAILS, GsonHelper.get().toJsonTree(diagnostic.details())); // 诊断详情
            diagnostics.add(json); // 加入诊断列表
        }
        result.add(DIAGNOSTICS, diagnostics); // 诊断列表
        return GsonHelper.sortObjectKeys(result).getAsJsonObject(); // 排序键以稳定输出
    }

    private static JsonArray variantsToJson(ItemRegistrationPlan.Entry entry, BedrockItemIcons.Result generated) {
        JsonArray variants = new JsonArray(); // 显示分支
        for (BedrockItemIcons.Variant variant : generated.variants().getOrDefault(entry.bedrockId(), List.of())) {
            String identifier = variant.id().asString();
            JsonObject branch = new JsonObject(); // 当前分支
            branch.addProperty(BEDROCK_IDENTIFIER, identifier); // 分支物品 ID
            JsonArray conditions = new JsonArray(); // 分支条件，全部满足
            for (ItemBindingCondition condition : variant.rule().conditions()) {
                JsonObject match = new JsonObject(); // 当前条件
                switch (condition) {
                    case ItemBindingCondition.BooleanMatch value -> {
                        value.property().writeProperty(match); // 条件属性
                        match.addProperty(EXPECTED, value.expected()); // 期望值
                    }
                    case ItemBindingCondition.SelectMatch value -> {
                        value.property().writeProperty(match); // 选择属性
                        JsonArray values = new JsonArray(); // 可选值
                        value.values().forEach(values::add); // 任一匹配
                        match.add(VALUES, values); // 匹配集合
                        match.addProperty(NEGATED, value.negated()); // fallback 取反
                    }
                    case ItemBindingCondition.NumericRange value -> {
                        value.property().writeProperty(match); // 数值属性
                        match.addProperty(SCALE, value.scale()); // 数值倍率
                        if (value.minInclusive() != null) {
                            match.addProperty(MIN, value.minInclusive()); // 包含下界
                        }
                        if (value.maxExclusive() != null) {
                            match.addProperty(MAX, value.maxExclusive()); // 不含上界
                        }
                    }
                }
                conditions.add(match); // 加入条件
            }
            branch.add(CONDITIONS, conditions); // 完整匹配条件
            JsonObject resources = new JsonObject(); // 分支资源
            BedrockItemBlocks.DisplayBlock block = generated.displayBlocks().get(variant.id()); // 展示方块
            if (block != null) {
                resources.addProperty(DISPLAY_BLOCK, block.name()); // 展示方块名称
            }
            resources.addProperty(ATTACHABLE, generated.attachables().contains(variant.id())); // 是否已生成手持模型
            resources.addProperty(GENERATED, block != null || generated.icons().contains(identifier)); // 资源生成状态
            if (variant.unsupportedReason() != null) {
                resources.addProperty(UNSUPPORTED_REASON, variant.unsupportedReason()); // 跳过原因
            }
            branch.add(RESOURCES, resources); // 分支资源引用
            variants.add(branch); // 加入分支
        }
        return variants;
    }
}
