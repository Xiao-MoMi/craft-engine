package net.momirealms.craftengine.core.pack;

public final class ResourceLocation {

    public static boolean isValid(final String resourceLocation) {
        int index = resourceLocation.indexOf(":");
        if (index == -1) {
            return isValidPath(resourceLocation);
        } else {
            return isValidNamespace(resourceLocation.substring(0, index)) && isValidPath(resourceLocation.substring(index + 1));
        }
    }

    public static boolean validPathChar(char character) {
        return character == '_' || character == '-' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '/' || character == '.';
    }

    private static boolean validNamespaceChar(char character) {
        return character == '_' || character == '-' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '.';
    }

    public static boolean isValidNamespace(String namespace) {
        for (int i = 0; i < namespace.length(); ++i) {
            if (!validNamespaceChar(namespace.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidPath(String path) {
        for(int i = 0; i < path.length(); ++i) {
            if (!validPathChar(path.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
