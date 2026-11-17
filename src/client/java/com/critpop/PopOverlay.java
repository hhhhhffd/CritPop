package com.critpop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class PopOverlay {
    private static final long LIFETIME = 1260;
    private static final int MAX_ACTIVE = 4;
    private static final int[] PALETTE = {0xFF62C6, 0x5FE8FF, 0xFFD86F, 0xB9FF79, 0xC79BFF, 0xFFA280};
    private final ArrayList<Pop> active = new ArrayList<>();
    private final Random rng = new Random();
    private final ArrayList<String> bag = new ArrayList<>();
    private List<String> lastList = List.of();
    private String lastPhrase;
    private int lastZone = -1;
    private int zonesUsed;
    private int lastColor = -1;

    void clear() {
        active.clear();
        bag.clear();
        lastPhrase = null;
        lastZone = lastColor = -1;
        zonesUsed = 0;
    }

    void pop(List<String> phrases, long now) {
        prune(now);
        if (!lastList.equals(phrases)) {
            lastList = List.copyOf(phrases);
            bag.clear();
        }
        if (bag.isEmpty()) {
            bag.addAll(phrases);
            Collections.shuffle(bag, rng);
            int last = bag.size() - 1;
            if (bag.get(last).equals(lastPhrase)) {
                for (int i = last - 1; i >= 0; i--) {
                    if (!bag.get(i).equals(lastPhrase)) {
                        Collections.swap(bag, i, last);
                        break;
                    }
                }
            }
        }
        String phrase = bag.removeLast();
        int zone = pickZone();
        int shade = rng.nextInt(PALETTE.length - (lastColor < 0 ? 0 : 1));
        if (lastColor >= 0 && shade >= lastColor) shade++;
        if (active.size() >= MAX_ACTIVE) active.removeFirst();
        active.add(new Pop(Component.literal(phrase).withStyle(ChatFormatting.BOLD), zone, PALETTE[shade], now, rng.nextDouble(0.0, 6.2831853)));
        lastPhrase = phrase;
        lastZone = zone;
        lastColor = shade;
    }

    private int pickZone() {
        int winner = 0;
        double best = -1_000_000;
        var anchors = PopPlacement.ZONES;
        if (zonesUsed == (1 << anchors.length) - 1) zonesUsed = 0;
        for (int i = 0; i < anchors.length; i++) {
            if (i == lastZone || (zonesUsed & (1 << i)) != 0) continue;
            double distance = 1;
            for (Pop p : active) {
                double dx = anchors[i][0] - anchors[p.zone][0];
                double dy = anchors[i][1] - anchors[p.zone][1];
                distance = Math.min(distance, dx * dx + 1.4 * dy * dy);
            }
            double score = distance + rng.nextDouble() * .065;
            if (score > best) {
                winner = i;
                best = score;
            }
        }
        zonesUsed |= 1 << winner;
        return winner;
    }

    private void prune(long now) {
        active.removeIf(pop -> now - pop.start >= LIFETIME);
    }

    void render(GuiGraphics graphics, Font font, long now) {
        prune(now);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int ceiling = PopPlacement.ceiling(height);
        if (active.isEmpty() || ceiling < 38 || width < 80) return;
        graphics.enableScissor(4, 6, width - 4, ceiling);
        for (Pop pop : active) draw(graphics, font, pop, now, width, height);
        graphics.disableScissor();
    }

    private void draw(GuiGraphics g, Font font, Pop pop, long now, int width, int height) {
        long elapsed = now - pop.start;
        float age = (float)elapsed / LIFETIME;
        float intro = Math.clamp((float)elapsed / 170, 0f, 1f);
        float overshoot = easeOutBack(intro);
        float fade = Math.clamp((LIFETIME - elapsed) / 300f, 0f, 1f);
        float opacity = fade * Math.clamp(elapsed / 55f, 0f, 1f);
        if (opacity <= 0) return;

        int textWidth = font.width(pop.text);
        int half = (textWidth + 1) / 2 + 8;
        float base = PopPlacement.scale(width, height, textWidth);
        float scale = base * (.50f + .50f * overshoot) * (1f - (1f - fade) * .09f);
        float x = PopPlacement.x(width, pop.zone, (half + 13) * base + 5);
        float y = PopPlacement.y(height, pop.zone, age, 19f * base);
        drawSparks(g, pop, x, y, half, age, base);

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);

        int accent = pop.color;
        g.fill(-half + 1, -9, half + 2, 13, argb(0x000000, opacity * .55f));
        g.fill(-half - 3, -8, half + 3, 9, argb(accent, opacity * .16f));
        g.fill(-half + 2, -11, half - 2, 11, argb(0x151326, opacity * .94f));
        g.fill(-half, -9, half, 9, argb(0x151326, opacity * .94f));
        g.fill(-half + 2, -11, half - 2, -10, argb(accent, opacity * .90f));
        g.fill(-half + 2, 10, half - 2, 11, argb(accent, opacity * .46f));
        g.fill(-half, -8, -half + 2, 8, argb(accent, opacity));
        g.fill(half - 2, -8, half, 8, argb(accent, opacity));
        g.drawString(font, pop.text, -textWidth / 2, -4, argb(accent, opacity), true);

        pose.popMatrix();
    }

    private static void drawSparks(GuiGraphics g, Pop pop, float x, float y, int half, float age, float base) {
        float sparkLife = Math.clamp(age / .60f, 0f, 1f);
        float alpha = (1f - sparkLife) * Math.min(1f, age * 24f);
        if (alpha <= .02f) return;
        for (int i = 0; i < 8; i++) {
            double angle = pop.seed + i * (Math.PI / 4);
            float spread = 8f + sparkLife * 18f;
            float dx = (float)Math.cos(angle) * (half + spread);
            float dy = (float)Math.sin(angle) * (8f + spread * .46f);
            int px = Math.round(x + dx * base);
            int py = Math.round(y + dy * base);
            int size = i % 3 == 0 ? 2 : 1;
            g.fill(px - size, py - 1, px + size + 1, py + 1, argb(pop.color, alpha));
            if (size == 2) g.fill(px - 1, py - size, px + 1, py + size + 1, argb(0xFFFFFF, alpha * .78f));
        }
    }

    private static int argb(int rgb, float opacity) {
        return Math.round(Math.clamp(opacity, 0f, 1f) * 255f) << 24 | rgb;
    }

    private static float easeOutBack(float t) {
        float n = t - 1f;
        return 1f + 2.70158f * n * n * n + 1.70158f * n * n;
    }

    private record Pop(Component text, int zone, int color, long start, double seed) { }
}
