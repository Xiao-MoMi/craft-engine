package net.momirealms.craftengine.core.pack.bedrock.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.text.ContainerTitleCodec;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BedrockGuiWriter {
    private final JsonObject controls = new JsonObject();
    private final JsonArray routes = new JsonArray();
    private final JsonArray smallRoutes = new JsonArray();

    public BedrockGuiWriter() {
        this.controls.addProperty("namespace", "craftengine_gui");
        JsonObject empty = new JsonObject();
        empty.addProperty("type", "panel");
        this.controls.add("empty", empty);
        JsonObject slot = new JsonObject();
        slot.addProperty("$item_collection_name", "container_items");
        slot.addProperty("$background_images", "craftengine_gui.empty");
        this.controls.add("slot@common.container_item", slot);
        this.addTitleLayerTemplate();
    }

    public void add(int rows, String texture) {
        String id = "container_" + rows;
        String marker = ContainerTitleLayout.MARKER + rows + "/";
        JsonObject route = new JsonObject();
        route.addProperty("requires", "($desktop_screen and (not (($craftengine_title - '" + marker + "') = $craftengine_title)))");
        route.addProperty("$screen_content", "craftengine_gui." + id);
        if (rows <= 3) {
            this.smallRoutes.add(route);
        } else {
            this.routes.add(route);
        }
        this.addPanel(id, rows, texture);
    }

    private void addPanel(String id, int rows, String texture) {
        JsonArray content = new JsonArray();
        JsonObject background = this.position(0, 0, 176, 114 + rows * 18);
        background.addProperty("type", "image");
        background.addProperty("texture", texture);
        background.addProperty("layer", 0);
        content.add(GsonHelper.createObject("background", background));
        this.addTitleLayers(content, rows);
        JsonObject frame = new JsonObject();
        frame.addProperty("$dialog_background", "craftengine_gui.empty");
        frame.addProperty("$show_divider", false);
        frame.addProperty("layer", 30);
        content.add(GsonHelper.createObject("frame@common.common_panel", frame));
        // The 18px cells include a one-pixel border around Java's 16px item render area.
        content.add(GsonHelper.createObject("container", this.grid(7, 17, rows, "container_items", "craftengine_gui.slot")));
        content.add(GsonHelper.createObject("inventory", this.grid(7, rows * 18 + 30, 3, "inventory_items", "common.grid_item_for_inventory")));
        content.add(GsonHelper.createObject("hotbar", this.grid(7, rows * 18 + 88, 1, "hotbar_items", "common.grid_item_for_hotbar")));
        JsonObject label = this.position(8, rows * 18 + 20, 162, 10);
        label.addProperty("text", "container.inventory");
        label.addProperty("layer", 5);
        content.add(GsonHelper.createObject("inventory_label@common.section_heading_label", label));
        content.add(GsonHelper.createObject("inventory_take_progress_icon_button@common.inventory_take_progress_icon_button", new JsonObject()));
        JsonObject flying = new JsonObject();
        flying.addProperty("layer", 40);
        content.add(GsonHelper.createObject("flying_item_renderer@common.flying_item_renderer", flying));
        content.add(GsonHelper.createObject("inventory_selected_icon_button@common.inventory_selected_icon_button", new JsonObject()));
        content.add(GsonHelper.createObject("gamepad_cursor@common.gamepad_cursor_button", new JsonObject()));

        JsonObject root = new JsonObject();
        root.add("size", GsonHelper.createArray(176, 114 + rows * 18));
        root.add("controls", content);
        JsonArray children = new JsonArray();
        children.add(GsonHelper.createObject("container_gamepad_helpers@common.container_gamepad_helpers", new JsonObject()));
        children.add(GsonHelper.createObject("selected_item_details_factory@common.selected_item_details_factory", new JsonObject()));
        children.add(GsonHelper.createObject("item_lock_notification_factory@common.item_lock_notification_factory", new JsonObject()));
        children.add(GsonHelper.createObject("root_panel@common.root_panel", root));
        JsonObject panel = new JsonObject();
        panel.addProperty("type", "panel");
        panel.add("controls", children);
        this.controls.add(id, panel);
    }

    private void addTitleLayers(JsonArray content, int rows) {
        String marker = ContainerTitleLayout.MARKER + rows + "/";
        JsonObject panel = this.position(8 - ContainerTitleLayout.ORIGIN, 5, 0, 0);
        panel.addProperty("type", "stack_panel");
        panel.addProperty("orientation", "horizontal");
        panel.addProperty("layer", 1);
        panel.addProperty("collection_name", "craftengine_title_layers");
        panel.addProperty("$craftengine_text", "$container_title");
        JsonObject factory = new JsonObject();
        factory.addProperty("name", "craftengine_title_layers_factory");
        factory.addProperty("control_name", "craftengine_gui.title_layer");
        factory.addProperty("max_children_size", ContainerTitleCodec.MAX_BYTES / ContainerTitleCodec.ENTRY_BYTES);
        panel.add("factory", factory);
        JsonArray bindings = new JsonArray();
        JsonObject trigger = new JsonObject();
        trigger.addProperty("binding_name", "#gamepad_helper_visible");
        bindings.add(trigger);
        this.addBinding(bindings, "($craftengine_text + ('%.0s' * #gamepad_helper_visible))", "#craftengine_raw");
        this.addBinding(bindings, "(#craftengine_raw - '§z§" + rows + "§r' - '§r" + marker + "' - '" + marker + "')", "#craftengine_data");
        this.addBinding(bindings, "('%." + ContainerTitleCodec.COUNT_BYTES + "s' * #craftengine_data)", "#craftengine_count");
        this.addBinding(bindings, "(#craftengine_count - 'N' - ':' + 0)", "#collection_length");
        this.addBinding(bindings, "(#craftengine_data - #craftengine_count)", "#craftengine_payload");
        panel.add("bindings", bindings);
        content.add(GsonHelper.createObject("title_layers", panel));
    }

    private void addTitleLayerTemplate() {
        JsonObject label = this.position(0, 0, 0, 0);
        JsonArray size = new JsonArray();
        size.add("default");
        size.add("default");
        label.add("size", size);
        label.addProperty("type", "label");
        label.addProperty("layer", 0);
        label.addProperty("localize", false);
        label.addProperty("text_alignment", "left");
        label.addProperty("shadow", false);
        label.addProperty("text", "#craftengine_layer");
        JsonArray bindings = new JsonArray();
        JsonObject details = new JsonObject();
        details.addProperty("binding_type", "collection_details");
        details.addProperty("binding_collection_name", "craftengine_title_layers");
        details.addProperty("binding_collection_prefix", "craftengine");
        bindings.add(details);
        JsonObject source = new JsonObject();
        source.addProperty("binding_type", "view");
        source.addProperty("source_control_name", "title_layers");
        source.addProperty("source_property_name", "#craftengine_payload");
        source.addProperty("target_property_name", "#craftengine_payload");
        bindings.add(source);
        // Decode each generated control's table entry through the client-verified staged bindings.
        this.addBinding(bindings, "('%.' + (#craftengine_collection_index * " + ContainerTitleCodec.ENTRY_BYTES + ") + 's')", "#craftengine_skip_format");
        this.addBinding(bindings, "(#craftengine_skip_format * #craftengine_payload)", "#craftengine_skip_prefix");
        this.addBinding(bindings, "(#craftengine_payload - #craftengine_skip_prefix)", "#craftengine_skip_tail");
        this.addBinding(bindings, "('%." + ContainerTitleCodec.FORMAT_BYTES + "s' * #craftengine_skip_tail)", "#craftengine_skip");
        this.addBinding(bindings, "('%.' + ((#craftengine_collection_index * " + ContainerTitleCodec.ENTRY_BYTES + ") + " + ContainerTitleCodec.FORMAT_BYTES + ") + 's')", "#craftengine_take_format");
        this.addBinding(bindings, "(#craftengine_take_format * #craftengine_payload)", "#craftengine_take_prefix");
        this.addBinding(bindings, "(#craftengine_payload - #craftengine_take_prefix)", "#craftengine_take_tail");
        this.addBinding(bindings, "('%." + ContainerTitleCodec.FORMAT_BYTES + "s' * #craftengine_take_tail)", "#craftengine_take");
        this.addBinding(bindings, "(#craftengine_skip * #craftengine_payload)", "#craftengine_body_prefix");
        this.addBinding(bindings, "(#craftengine_payload - #craftengine_body_prefix)", "#craftengine_body_tail");
        this.addBinding(bindings, "(#craftengine_take * #craftengine_body_tail)", "#craftengine_layer");
        label.add("bindings", bindings);
        JsonArray children = new JsonArray();
        children.add(GsonHelper.createObject("text", label));
        // Zero-width siblings share the title origin; equal child layers preserve factory drawing order.
        JsonObject item = this.position(0, 0, 0, 0);
        item.addProperty("type", "panel");
        item.addProperty("layer", 0);
        item.add("controls", children);
        this.controls.add("title_layer", item);
    }

    private void addBinding(JsonArray bindings, String expression, String target) {
        JsonObject binding = new JsonObject();
        binding.addProperty("binding_type", "view");
        binding.addProperty("source_property_name", expression);
        binding.addProperty("target_property_name", target);
        bindings.add(binding);
    }

    public void write(Path root) throws IOException {
        if (this.routes.isEmpty()) {
            return;
        }
        Files.createDirectories(root.resolve("ui/craftengine"));
        GsonHelper.writeJsonFile(this.controls, root.resolve("ui/craftengine/menus.json"));
        JsonObject chest = new JsonObject();
        chest.addProperty("namespace", "chest");
        chest.add("large_chest_screen", this.screen(this.routes));
        chest.add("small_chest_screen", this.screen(this.smallRoutes));
        GsonHelper.writeJsonFile(chest, root.resolve("ui/chest_screen.json"));
        JsonArray files = new JsonArray();
        files.add("ui/craftengine/menus.json");
        JsonObject definitions = new JsonObject();
        definitions.add("ui_defs", files);
        GsonHelper.writeJsonFile(definitions, root.resolve("ui/_ui_defs.json"));
    }

    private JsonObject screen(JsonArray routes) {
        JsonObject modification = new JsonObject();
        modification.addProperty("array_name", "variables");
        modification.addProperty("operation", "insert_back");
        modification.add("value", routes);
        JsonArray modifications = new JsonArray();
        modifications.add(modification);
        JsonObject screen = new JsonObject();
        // Assignment unwraps the client's raw-text title object before expression evaluation.
        screen.addProperty("$craftengine_title", "$container_title");
        screen.add("modifications", modifications);
        return screen;
    }

    private JsonObject grid(int x, int y, int rows, String collection, String template) {
        JsonObject grid = this.position(x, y, 162, rows * 18);
        grid.addProperty("type", "grid");
        grid.addProperty("layer", 5);
        grid.add("grid_dimensions", GsonHelper.createArray(9, rows));
        grid.addProperty("grid_item_template", template);
        grid.addProperty("collection_name", collection);
        // The Java background already contains the cell artwork; keep the native slot input and overlays.
        grid.addProperty("$background_images", "craftengine_gui.empty");
        return grid;
    }

    private JsonObject position(int x, int y, int width, int height) {
        JsonObject object = new JsonObject();
        object.addProperty("anchor_from", "top_left");
        object.addProperty("anchor_to", "top_left");
        object.add("offset", GsonHelper.createArray(x, y));
        object.add("size", GsonHelper.createArray(width, height));
        return object;
    }
}
