package com.wudji.villagertradingoverhanful.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class VanillaButton extends Button.Plain {
    private final Font font;

    public VanillaButton(Font font, int x, int y, int width, Component message, OnPress onPress) {
        super(x, y, width, 20, message, onPress, DEFAULT_NARRATION);
        this.font = font;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractDefaultSprite(graphics);
        Component message = getMessage();
        if (message.getString().isEmpty()) {
            return;
        }
        int color = active ? 0xFFFFFFFF : 0xFFA0A0A0;
        int textWidth = font.width(message);
        int textX = getX() + (getWidth() - textWidth) / 2;
        int textY = getY() + (getHeight() - 8) / 2;
        graphics.text(font, message, textX, textY, color, false);
    }
}
