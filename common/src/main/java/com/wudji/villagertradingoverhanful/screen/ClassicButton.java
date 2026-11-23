package com.wudji.villagertradingoverhanful.screen;

import com.wudji.villagertradingoverhanful.Constants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ClassicButton extends Button.Plain {
    private static final Identifier NORMAL = texture("classic_button");
    private static final Identifier HIGHLIGHTED = texture("classic_button_highlighted");
    private static final Identifier DISABLED = texture("classic_button_disabled");
    private static final int TEXTURE_WIDTH = 200;
    private static final int TEXTURE_HEIGHT = 20;

    public ClassicButton(int x, int y, int width, Component message, OnPress onPress) {
        super(x, y, width, TEXTURE_HEIGHT, message.copy().withStyle(ChatFormatting.DARK_GRAY), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Identifier texture = !active ? DISABLED : (isHoveredOrFocused() ? HIGHLIGHTED : NORMAL);
        blitButton(graphics, texture, getX(), getY(), getWidth());
        extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }

    private static void blitButton(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width) {
        if (width < 6) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, 3, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + 3, y, 3.0F, 0.0F, width - 6, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - 3, y, 197.0F, 0.0F, 3, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/" + name + ".png");
    }
}
