package net.momirealms.craftengine.core.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.CharacterUtils;
import net.momirealms.craftengine.core.util.Key;

public class BitmapImage implements FontProvider {
    private final Key imageId;
    private final Key font;
    private final int height;
    private final int ascent;
    private final String file;
    private final int[][] codepointGrid;

    public BitmapImage(Key imageId, Key font, int height, int ascent, String file, int[][] codepointGrid) {
        this.imageId = imageId;
        this.font = font;
        this.height = height;
        this.ascent = ascent;
        this.file = file;
        this.codepointGrid = codepointGrid;
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

    public Key imageId() {
        return imageId;
    }

    public int[][] codepointGrid() {
        return codepointGrid;
    }

    public int codepointAt(int row, int column) {
        if (row < 0 || row >= codepointGrid.length || column < 0 || column >= codepointGrid[row].length) {
            throw new IndexOutOfBoundsException("Invalid index: (" + row + ", " + column + ")");
        }
        return codepointGrid[row][column];
    }

    @SuppressWarnings("all")
    public Component componentAt(int row, int column) {
        int codepoint = codepointAt(row, column);
        return Component.text(new String(Character.toChars(codepoint))).font(net.kyori.adventure.key.Key.key(font().toString()));
    }

    public boolean isValidCoordinate(int row, int column) {
        return row >= 0 && row < codepointGrid.length && column >= 0 && column < codepointGrid[row].length;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        BitmapImage image = (BitmapImage) object;
        return imageId.equals(image.imageId);
    }

    @Override
    public int hashCode() {
        return imageId.hashCode();
    }

    @Override
    public JsonObject getJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "bitmap");
        jsonObject.addProperty("height", height);
        jsonObject.addProperty("ascent", ascent);
        jsonObject.addProperty("file", file);
        JsonArray charArray = new JsonArray();
        jsonObject.add("chars", charArray);
        for (int[] codepoints : codepointGrid) {
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
