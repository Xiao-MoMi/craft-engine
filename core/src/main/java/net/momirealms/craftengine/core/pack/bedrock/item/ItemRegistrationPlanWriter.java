package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class ItemRegistrationPlanWriter {

    private ItemRegistrationPlanWriter() {
    }

    public static void write(ItemRegistrationPlan plan, Path path) throws IOException {
        Files.writeString(path, toJson(plan).toString());
    }

    public static JsonObject toJson(ItemRegistrationPlan plan) {
        JsonObject result = new JsonObject();
        result.addProperty(KIND, ITEM_REGISTRATION_PLAN_KIND);
        result.addProperty(FORMAT_VERSION, ITEM_REGISTRATION_PLAN_FORMAT_VERSION); // 计划格式版本
        result.addProperty(SOURCE_VERSION, VersionHelper.MINECRAFT_VERSION.version()); // 来源 Java 版本
        JsonArray entries = new JsonArray();
        for (ItemRegistrationPlan.Entry entry : plan.entries()) {
            JsonObject json = new JsonObject();
            json.addProperty(BEDROCK_IDENTIFIER, entry.bedrockId().asString()); // 基岩物品 ID
            json.addProperty(JAVA_ITEM, entry.javaItemType().asString()); // 基础 Java 物品类型
            json.addProperty(MODEL, entry.definitionId().asString()); // 匹配的 item_model ID
            JsonObject components = new JsonObject(); // 相对原版的组件差异
            entry.properties().components().forEach((key, value) -> components.add(key.asString(), value)); // null 表示移除
            json.add(COMPONENTS, components); // 注册组件
            JsonObject resources = new JsonObject(); // 资源信息
            resources.addProperty(ICON, entry.icon()); // 基岩图标资源键
            resources.addProperty(ATTACHABLE_IDENTIFIER, entry.bedrockId().asString()); // 预留 attachable ID
            resources.addProperty(GENERATED, false); // 资源尚未生成
            resources.addProperty(MODEL_RULE_COUNT, entry.definition().rules().size()); // 模型绑定规则数量
            json.add(RESOURCES, resources); // 资源引用及状态
            ResolvedItemBinding binding = entry.source(); // 物品关联结果
            JsonObject source = new JsonObject(); // 来源信息
            source.addProperty(ITEM_ID, binding.itemId().asString()); // 来源 CE 物品 ID
            source.addProperty(SERVER_ITEM_TYPE, binding.serverItemType().asString()); // 投影前物品类型
            source.addProperty(CLIENT_ITEM_TYPE, binding.clientItemType().asString()); // 投影后物品类型
            source.add(CLIENT_BEHAVIOR, GsonHelper.get().toJsonTree(binding.clientBehavior())); // 使用、攻击及交互行为
            json.add(SOURCE, source); // 来源物品信息
            entries.add(json); // 加入注册列表
        }
        result.add(ENTRIES, entries); // 全部注册条目
        JsonArray diagnostics = new JsonArray(); // 诊断列表
        for (ItemRegistrationPlan.Diagnostic diagnostic : plan.diagnostics()) { // 逐项写入诊断
            JsonObject json = new JsonObject(); // 当前条目
            json.addProperty(SEVERITY, diagnostic.severity().name()); // 诊断级别
            json.addProperty(CODE, diagnostic.code()); // 诊断代码
            json.addProperty(JAVA_ITEM, diagnostic.javaItemType().asString()); // 基础 Java 物品类型
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
}
