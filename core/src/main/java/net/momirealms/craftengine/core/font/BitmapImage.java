package net.momirealms.craftengine.core.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.CharacterUtils;
import net.momirealms.craftengine.core.util.FormatUtils;
import net.momirealms.craftengine.core.util.Key;

import java.util.function.Supplier;

public final class BitmapImage implements Supplier<JsonObject>, Image {
    private final Key id;
    private final Key font;
    private final int height;
    private final int ascent;
    private final String file;
    private final int[][] codepointGrid;

    public BitmapImage(Key id, Key font, int height, int ascent, String file, int[][] codepointGrid) {
        this.id = id;
        this.font = font;
        this.height = height;
        this.ascent = ascent;
        this.file = file;
        this.codepointGrid = codepointGrid;
    }

    @Override
    public String miniMessageAt(int row, int col) {
        int codepoint = this.codepointGrid[row][col];
        return FormatUtils.miniMessageFont(new String(Character.toChars(codepoint)), this.font.toString());
    }

    @Override
    public String mineDownAt(int row, int col) {
        int codepoint = this.codepointGrid[row][col];
        return FormatUtils.mineDownFont(new String(Character.toChars(codepoint)), this.font.toString());
    }

    public int height() {
        return height;
    }

    public int ascent() {
        return ascent;
    }

    public String file() {
        return file;
    }

    public Key font() {
        return font;
    }

    @Override
    public Key id() {
        return id;
    }

    public int[][] codepointGrid() {
        return this.codepointGrid.clone();
    }

    public int rows() {
        return this.codepointGrid.length;
    }

    public int columns() {
        return this.codepointGrid[0].length;
    }

    @Override
    public int codepointAt(int row, int column) {
        if (!isValidCoordinate(row, column)) {
            throw new IndexOutOfBoundsException("Invalid index: (" + row + ", " + column + ") for image " + id());
        }
        return this.codepointGrid[row][column];
    }

    @SuppressWarnings("all")
    @Override
    public Component componentAt(int row, int column) {
        int codepoint = codepointAt(row, column);
        return Component.text(new String(Character.toChars(codepoint))).font(net.kyori.adventure.key.Key.key(font().toString()));
    }

    public boolean isValidCoordinate(int row, int column) {
        return row >= 0 && row < this.codepointGrid.length && column >= 0 && column < this.codepointGrid[row].length;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        BitmapImage image = (BitmapImage) object;
        return this.id.equals(image.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public JsonObject get() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "bitmap");
        jsonObject.addProperty("height", this.height);
        jsonObject.addProperty("ascent", this.ascent);
        jsonObject.addProperty("file", this.file);
        JsonArray charArray = new JsonArray();
        jsonObject.add("chars", charArray);
        for (int[] codepoints : this.codepointGrid) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int codepoint : codepoints) {
                stringBuilder.append(CharacterUtils.encodeCharsToUnicode(Character.toChars(codepoint)));
            }
            // to deceive Gson
            charArray.add(stringBuilder.toString());
        }
        return jsonObject;
    }
}
