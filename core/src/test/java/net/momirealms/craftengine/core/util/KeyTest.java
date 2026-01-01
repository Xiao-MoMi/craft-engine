package net.momirealms.craftengine.core.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the {@link Key} utility class.
 * Tests cover key parsing, creation, validation, and equality operations.
 */
@DisplayName("Key Utility Tests")
class KeyTest {

    private static final String DEFAULT_NAMESPACE = "minecraft";

    @Nested
    @DisplayName("Key Creation Tests")
    class KeyCreationTests {

        @Test
        @DisplayName("Should create key with namespace and value")
        void shouldCreateKeyWithNamespaceAndValue() {
            Key key = Key.of("craftengine", "custom_block");

            assertNotNull(key);
            assertEquals("craftengine", key.namespace());
            assertEquals("custom_block", key.value());
        }

        @Test
        @DisplayName("Should create key with default minecraft namespace")
        void shouldCreateKeyWithDefaultNamespace() {
            Key key = Key.of("stone");

            assertNotNull(key);
            assertEquals(DEFAULT_NAMESPACE, key.namespace());
            assertEquals("stone", key.value());
        }

        @ParameterizedTest
        @CsvSource(
            {
                "minecraft:stone, minecraft, stone",
                "craftengine:ruby, craftengine, ruby",
                "custom:items/sword, custom, items/sword",
                "test:path/to/item, test, path/to/item",
            }
        )
        @DisplayName("Should parse key from string with colon separator")
        void shouldParseKeyFromString(
            String input,
            String expectedNamespace,
            String expectedValue
        ) {
            Key key = Key.of(input);

            assertNotNull(key);
            assertEquals(expectedNamespace, key.namespace());
            assertEquals(expectedValue, key.value());
        }
    }

    @Nested
    @DisplayName("Key Validation Tests")
    class KeyValidationTests {

        @ParameterizedTest
        @ValueSource(
            strings = {
                "valid_key",
                "valid-key",
                "valid.key",
                "valid/key/path",
                "123numeric",
                "a",
            }
        )
        @DisplayName("Should accept valid key values")
        void shouldAcceptValidKeyValues(String value) {
            assertDoesNotThrow(() -> Key.of(DEFAULT_NAMESPACE, value));
        }
    }

    @Nested
    @DisplayName("Key Equality Tests")
    class KeyEqualityTests {

        @Test
        @DisplayName("Should be equal when namespace and value match")
        void shouldBeEqualWhenNamespaceAndValueMatch() {
            Key key1 = Key.of("craftengine", "test");
            Key key2 = Key.of("craftengine", "test");

            assertEquals(key1, key2);
            assertEquals(key1.hashCode(), key2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when namespace differs")
        void shouldNotBeEqualWhenNamespaceDiffers() {
            Key key1 = Key.of("minecraft", "stone");
            Key key2 = Key.of("craftengine", "stone");

            assertNotEquals(key1, key2);
        }

        @Test
        @DisplayName("Should not be equal when value differs")
        void shouldNotBeEqualWhenValueDiffers() {
            Key key1 = Key.of("minecraft", "stone");
            Key key2 = Key.of("minecraft", "dirt");

            assertNotEquals(key1, key2);
        }
    }

    @Nested
    @DisplayName("Key String Representation Tests")
    class KeyStringRepresentationTests {

        @Test
        @DisplayName("Should return correct string representation")
        void shouldReturnCorrectStringRepresentation() {
            Key key = Key.of("craftengine", "custom_item");

            assertEquals("craftengine:custom_item", key.toString());
        }

        @Test
        @DisplayName("Should return correct string for minecraft namespace")
        void shouldReturnCorrectStringForMinecraftNamespace() {
            Key key = Key.of("minecraft", "diamond");

            assertEquals("minecraft:diamond", key.toString());
        }

        @ParameterizedTest
        @CsvSource(
            {
                "ns, val, ns:val",
                "a, b, a:b",
                "long_namespace, long_value_path/sub, long_namespace:long_value_path/sub",
            }
        )
        @DisplayName("Should format as namespace:value")
        void shouldFormatAsNamespaceColonValue(
            String namespace,
            String value,
            String expected
        ) {
            Key key = Key.of(namespace, value);

            assertEquals(expected, key.toString());
        }
    }

    @Nested
    @DisplayName("Key Utility Method Tests")
    class KeyUtilityMethodTests {

        @Test
        @DisplayName("Should check if key belongs to namespace")
        void shouldCheckNamespaceBelonging() {
            Key key = Key.of("craftengine", "item");

            assertTrue(key.namespace().equals("craftengine"));
            assertFalse(key.namespace().equals("minecraft"));
        }

        @Test
        @DisplayName("Should handle keys with path separators")
        void shouldHandleKeysWithPathSeparators() {
            Key key = Key.of("craftengine", "blocks/custom/ruby_ore");

            assertEquals("craftengine", key.namespace());
            assertEquals("blocks/custom/ruby_ore", key.value());
            assertEquals("craftengine:blocks/custom/ruby_ore", key.toString());
        }
    }
}
