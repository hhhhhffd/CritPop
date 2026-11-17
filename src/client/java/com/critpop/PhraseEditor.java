package com.critpop;

import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class PhraseEditor extends Screen {
    private final Screen parent;
    private final ArrayList<String> phrases;
    private boolean enabled;
    private int page;
    private int panelWidth;
    private int panelX;
    private int rows;
    private int listY;
    private int actionY;
    private int navigationY;
    private int footerY;

    PhraseEditor(Screen parent) {
        super(Component.literal("Crit Pop Settings"));
        this.parent = parent;
        var config = CritPopConfig.get();
        enabled = config.enabled;
        phrases = new ArrayList<>(config.phrases);
    }

    @Override protected void init() {
        panelWidth = Math.min(350, Math.max(180, width - 28));
        panelX = (width - panelWidth) / 2;
        listY = 65;
        rows = Math.clamp((height - 141) / 22, 1, 7);
        actionY = listY + rows * 22 + 4;
        navigationY = actionY + 25;
        footerY = actionY + 51;
        page = Math.clamp(page, 0, pages() - 1);

        var toggle = Button.builder(toggleText(), button -> {
            enabled = !enabled;
            button.setMessage(toggleText());
        }).bounds(panelX, 28, panelWidth, 19).build();
        addRenderableWidget(toggle);

        int offset = page * rows;
        int deleteWidth = 60;
        for (int i = 0; i < rows && offset + i < phrases.size(); i++) {
            int index = offset + i;
            int y = listY + 22 * i;
            EditBox field = new EditBox(font, panelX, y, panelWidth - deleteWidth - 5, 19, null,
                    Component.literal("Phrase " + (index + 1)));
            field.setMaxLength(CritPopConfig.MAX_LENGTH);
            field.setValue(phrases.get(index));
            field.setHint(Component.literal("Type a phrase"));
            field.setResponder(value -> phrases.set(index, value));
            addRenderableWidget(field);
            addRenderableWidget(Button.builder(Component.literal("Remove"), button -> {
                phrases.remove(index);
                rebuildWidgets();
            }).bounds(panelX + panelWidth - deleteWidth, y, deleteWidth, 19).build());
        }

        int leftWidth = (panelWidth - 6) / 2;
        int rightX = panelX + leftWidth + 6;
        int rightWidth = panelWidth - leftWidth - 6;
        Button add = addRenderableWidget(Button.builder(Component.literal("Add phrase"), button -> {
            if (phrases.size() >= CritPopConfig.MAX_PHRASES) return;
            phrases.add("");
            page = (phrases.size() - 1) / rows;
            rebuildWidgets();
        }).bounds(panelX, actionY, leftWidth, 19).tooltip(Tooltip.create(
                Component.literal("Up to 64 phrases, 48 characters each."))).build());
        add.active = phrases.size() < CritPopConfig.MAX_PHRASES;
        addRenderableWidget(Button.builder(Component.literal("Reset defaults"), button -> {
            phrases.clear();
            phrases.addAll(CritPopConfig.DEFAULT_PHRASES);
            page = 0;
            rebuildWidgets();
        }).bounds(rightX, actionY, rightWidth, 19).build());

        Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            --page;
            rebuildWidgets();
        }).bounds(panelX, navigationY, 32, 19).build());
        previous.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            ++page;
            rebuildWidgets();
        }).bounds(panelX + panelWidth - 32, navigationY, 32, 19).build());
        next.active = page + 1 < pages();

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
            CritPopConfig.get().apply(enabled, phrases);
            onClose();
        }).bounds(panelX, footerY, leftWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(rightX, footerY, rightWidth, 20).build());
    }

    private int pages() {
        return Math.max(1, (phrases.size() + rows - 1) / rows);
    }

    private Component toggleText() {
        return Component.literal(enabled ? "Crit Pop: ON" : "Crit Pop: OFF");
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, getTitle(), width / 2, 13, 0xFFFFFFFF);
        g.drawString(font, "Phrases for confirmed melee critical hits", panelX, 53, 0xFFE1BAFF);
        if (phrases.isEmpty()) g.drawString(font, "No phrases yet.", panelX, listY + 4, 0xFFBBBBBB);
        g.drawCenteredString(font, (page + 1) + " / " + pages() + "   •   " + phrases.size() + " phrases",
                width / 2, navigationY + 6, 0xFFCCCCCC);
    }

    @Override protected void repositionElements() {
        rebuildWidgets();
    }

    @Override public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
