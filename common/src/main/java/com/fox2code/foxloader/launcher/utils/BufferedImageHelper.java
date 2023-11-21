package com.fox2code.foxloader.launcher.utils;

import java.awt.image.BufferedImage;

public class BufferedImageHelper {
    public static int[] toSafe16x16Data(BufferedImage bufferedImage) {
        if (bufferedImage.getWidth() != 16 || bufferedImage.getHeight() != 16) {
            throw new IllegalArgumentException("Image must be 16x16 (got " +
                    bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
        }
        int[] safe16x16data = new int[256];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                bufferedImage.setRGB(x, y, safe16x16data[x + (y * 16)]);
            }
        }
        return safe16x16data;
    }

    public static BufferedImage toBufferedImage(int[] safe16x16data) {
        if (safe16x16data.length != (16 * 16))
            throw new IllegalArgumentException("Expected 256 pixels, got " + safe16x16data.length);
        BufferedImage bufferedImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                bufferedImage.setRGB(x, y, safe16x16data[x + (y * 16)]);
            }
        }
        return bufferedImage;
    }
}
