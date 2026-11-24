package com.wudji.villagertradingoverhanful.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class VanillaButton extends Button.Plain {
    private final Font font;
    private Component label;

    public VanillaButton(Font font, int x, int y, int width, Component message, OnPress onPress) {
        super(x, y, width, 20, Component.empty(), onPress, DEFAULT_NARRATION);
        this.font = font;
        this.label = message;
    }

    @Override
    public void setMessage(Component message) {
        this.label = message;
        super.setMessage(Component.empty());
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderContents(graphics, mouseX, mouseY, partialTick);
        if (label.getString().isEmpty()) {
            return;
        }
        int color = active ? 0xFFFFFFFF : 0xFFA0A0A0;
        int textWidth = font.width(label);
        int textX = getX() + (getWidth() - textWidth) / 2;
        int textY = getY() + (getHeight() - 8) / 2;
        graphics.drawString(font, label, textX, textY, color, false);
    }
}
