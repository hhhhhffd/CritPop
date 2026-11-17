package com.critpop;

/** The overlay is clipped above the crosshair in every viewport. */
public final class PopPlacement {
    public static final double[][] ZONES = {
            {.18, .20}, {.39, .14}, {.66, .18}, {.82, .32},
            {.24, .34}, {.55, .33}, {.80, .13}, {.43, .28}
    };

    public static int ceiling(int height) {
        return Math.max(0, Math.min((int)(height * .44), height / 2 - 15));
    }

    public static float scale(int width, int height, int textWidth) {
        return Math.min(height < 260 ? 1.16f : 1.58f, width * .45f / (textWidth + 20));
    }

    public static float x(int width, int zone, float radius) {
        return Math.clamp((float)(width * ZONES[zone][0]), radius, Math.max(radius, width - radius));
    }

    public static float y(int height, int zone, float age, float radius) {
        float rise = 11f * age;
        return Math.clamp((float)(height * ZONES[zone][1]) - rise, radius, Math.max(radius, ceiling(height) - radius));
    }
}
