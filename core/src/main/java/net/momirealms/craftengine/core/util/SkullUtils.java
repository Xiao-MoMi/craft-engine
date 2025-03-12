/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.core.util;

import java.util.Base64;

public class SkullUtils {

    private SkullUtils() {}

    public static String identifierFromBase64(String base64) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64);
        String decodedString = new String(decodedBytes);
        int urlStartIndex = decodedString.indexOf("\"url\":\"") + 7;
        int urlEndIndex = decodedString.indexOf("\"", urlStartIndex);
        String textureUrl = decodedString.substring(urlStartIndex, urlEndIndex);
        return textureUrl.substring(textureUrl.lastIndexOf('/') + 1);
    }
}
