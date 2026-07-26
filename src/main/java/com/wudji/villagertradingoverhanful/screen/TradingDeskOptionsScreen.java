package com.wudji.villagertradingoverhanful.screen;

import com.wudji.villagertradingoverhanful.preferences.TradingDeskPreferences;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TradingDeskOptionsScreen extends Screen {
    private static final int[] BATCH_LIMITS = {1, 8, 16, 32, 64, 128};

    private final Screen parent;
    private Button batchLimitButton;
    private Button hideUnavailableButton;

    public TradingDeskOptionsScreen(Screen parent) {
        super(Component.translatable("screen.villagertradingoverhanful.options_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int rowY = height / 2 - 30;
        addRenderableWidget(Button.builder(Component.literal("−"), button -> changeBatchLimit(-1)).bounds(centerX - 105, rowY, 20, 20).build());
        batchLimitButton = addRenderableWidget(Button.builder(batchLimitLabel(), button -> changeBatchLimit(1)).bounds(centerX - 80, rowY, 160, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> changeBatchLimit(1)).bounds(centerX + 85, rowY, 20, 20).build());
        hideUnavailableButton = addRenderableWidget(Button.builder(hideUnavailableLabel(), button -> {
            TradingDeskPreferences.setHideUnavailable(!TradingDeskPreferences.shouldHideUnavailable());
            button.setMessage(hideUnavailableLabel());
        }).bounds(centerX - 105, rowY + 28, 210, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(centerX - 100, rowY + 68, 200, 20).build());
    }

    @Override
    public void onClose() {
        TradingDeskPreferences.save();
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 62, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, delta);
    }

    private void changeBatchLimit(int direction) {
        int current = TradingDeskPreferences.getBatchLimit();
        int index = 0;
        for (int i = 0; i < BATCH_LIMITS.length; i++) {
            if (BATCH_LIMITS[i] >= current) {
                index = i;
                break;
            }
        }
        index = Math.floorMod(index + direction, BATCH_LIMITS.length);
        TradingDeskPreferences.setBatchLimit(BATCH_LIMITS[index]);
        batchLimitButton.setMessage(batchLimitLabel());
    }

    private Component batchLimitLabel() {
        return Component.translatable("screen.villagertradingoverhanful.batch_limit", TradingDeskPreferences.getBatchLimit());
    }

    private Component hideUnavailableLabel() {
        return Component.translatable("screen.villagertradingoverhanful.hide_unavailable", TradingDeskPreferences.shouldHideUnavailable() ? Component.translatable("options.on") : Component.translatable("options.off"));
    }
}
