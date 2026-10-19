package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockLayoutTexturesTest {

    @Test
    void doubleChestReconstructsEveryLockFace() throws Exception {
        for (int scale : List.of(1, 2)) {
            BufferedImage left = pattern(64 * scale, 0x81000000);
            BufferedImage right = pattern(64 * scale, 0xc2000000);
            BufferedImage result = convert("doubleChest", left, right);
            // 每行描述一个半锁面：来源、源坐标、目标坐标、尺寸及翻转方向。
            int[][] faces = {
                    {1, 2, 0, 1, 0, 1, 1, 0, 1}, {0, 2, 0, 2, 0, 1, 1, 0, 1},
                    {1, 1, 0, 3, 0, 1, 1, 0, 1}, {0, 1, 0, 4, 0, 1, 1, 0, 1},
                    {1, 0, 1, 0, 1, 1, 4, 1, 1}, {0, 2, 1, 3, 1, 1, 4, 1, 1},
                    {1, 3, 1, 1, 1, 1, 4, 1, 1}, {0, 3, 1, 2, 1, 1, 4, 1, 1},
                    {0, 1, 1, 4, 1, 1, 4, 1, 1}, {1, 1, 1, 5, 1, 1, 4, 1, 1}
            };
            for (int[] face : faces) {
                assertRegion(face[0] == 0 ? left : right, result, face[1], face[2], face[3], face[4],
                        face[5], face[6], face[7] == 1, face[8] == 1, scale);
            }
        }
    }

    @Test
    void drownedMapsAllLimbsAndMirrorsIndividualFaces() throws Exception {
        for (int scale : List.of(1, 2)) {
            BufferedImage base = pattern(64 * scale, 0xff000000);
            BufferedImage outer = pattern(64 * scale, 0x80000000);
            BufferedImage result = convert("drowned", base, outer);
            int[][] limbs = {
                    {0, 40, 16, 0, 16, 0}, {0, 32, 48, 40, 16, 1},
                    {0, 0, 16, 16, 48, 0}, {0, 16, 48, 32, 48, 1},
                    {1, 40, 16, 48, 48, 0}, {1, 32, 48, 40, 32, 1},
                    {1, 0, 16, 0, 48, 0}, {1, 16, 48, 0, 32, 1}
            };
            int[][] faces = {{4, 0, 4, 4}, {8, 0, 4, 4}, {0, 4, 4, 12},
                    {4, 4, 4, 12}, {8, 4, 4, 12}, {12, 4, 4, 12}};
            for (int[] limb : limbs) {
                for (int[] face : faces) {
                    int targetX = face[0];
                    if (limb[5] == 1 && face[1] == 4) {
                        if (targetX == 0) {
                            targetX = 8;
                        } else if (targetX == 8) {
                            targetX = 0;
                        }
                    }
                    assertRegion(limb[0] == 0 ? base : outer, result,
                            limb[1] + face[0], limb[2] + face[1], limb[3] + targetX, limb[4] + face[1],
                            face[2], face[3], limb[5] == 1, false, scale);
                }
            }
            assertRegion(base, result, 0, 0, 0, 0, 32, 16, false, false, scale);
            assertRegion(base, result, 16, 16, 16, 16, 24, 16, false, false, scale);
            assertRegion(outer, result, 0, 0, 32, 0, 32, 16, false, false, scale);
            assertRegion(outer, result, 16, 16, 16, 32, 24, 16, false, false, scale);
        }
    }

    @Test
    void drownedPreservesTransparentAndTranslucentOuterPixels() throws Exception {
        BufferedImage base = pattern(64, 0xff000000);
        BufferedImage outer = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        outer.setRGB(8, 8, 0x804275a9);
        BufferedImage result = convert("drowned", base, outer);
        assertEquals(0x804275a9, result.getRGB(40, 8));
        assertEquals(0, result.getRGB(41, 8));
    }

    @Test
    void drownedKeepsHigherResolutionFromEitherInput() throws Exception {
        BufferedImage high = pattern(128, 0xff000000);
        BufferedImage low = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        BufferedImage outerHigh = convert("drowned", low, high);
        assertEquals(128, outerHigh.getWidth());
        assertRegion(high, outerHigh, 0, 0, 32, 0, 32, 16, false, false, 2);
        BufferedImage baseHigh = convert("drowned", high, low);
        assertEquals(128, baseHigh.getWidth());
        assertRegion(high, baseHigh, 0, 0, 0, 0, 32, 16, false, false, 2);
    }

    @Test
    void bundleKeepsHighResolutionAndCompositesFrontOverBack() throws Exception {
        BufferedImage back = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        back.setRGB(0, 0, 0xffff0000);
        BufferedImage front = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        front.setRGB(0, 0, 0x800000ff);
        front.setRGB(1, 0, 0xff123456);
        BufferedImage result = convert("bundle", back, front);
        assertEquals(64, result.getWidth());
        assertEquals(64, result.getHeight());
        assertEquals(0xff7f0080, result.getRGB(0, 0));
        assertEquals(0xff123456, result.getRGB(1, 0));
        assertEquals(0xffff0000, result.getRGB(2, 0));
        assertEquals(64, convert("bundle", front, back).getWidth());
    }

    private static BufferedImage convert(String name, BufferedImage first, BufferedImage second) throws Exception {
        Method method = BedrockLayoutTextures.class.getDeclaredMethod(name, List.class);
        method.setAccessible(true);
        return (BufferedImage) method.invoke(new BedrockLayoutTextures(), List.of(first, second));
    }

    private static BufferedImage pattern(int width, int alpha) {
        BufferedImage image = new BufferedImage(width, width, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < width; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, alpha | x << 16 | y << 8 | (x + y) & 0xff);
            }
        }
        return image;
    }

    private static void assertRegion(BufferedImage source, BufferedImage target, int sx, int sy, int dx, int dy,
                                     int width, int height, boolean flipX, boolean flipY, int scale) {
        for (int y = 0; y < height * scale; y++) {
            for (int x = 0; x < width * scale; x++) {
                int sourceX = sx * scale + (flipX ? width * scale - x - 1 : x);
                int sourceY = sy * scale + (flipY ? height * scale - y - 1 : y);
                assertEquals(source.getRGB(sourceX, sourceY), target.getRGB(dx * scale + x, dy * scale + y),
                        "UV at " + (dx * scale + x) + "," + (dy * scale + y));
            }
        }
    }
}
