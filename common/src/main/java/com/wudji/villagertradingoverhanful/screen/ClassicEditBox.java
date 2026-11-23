package com.wudji.villagertradingoverhanful.screen;

import com.wudji.villagertradingoverhanful.Constants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ClassicEditBox extends EditBox {
    private static final Identifier FIELD = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/classic_field.png");
    private static final int FIELD_WIDTH = 42;
    private static final int FIELD_HEIGHT = 18;
    private static final int TEXT_OFFSET_Y = 5;

    public ClassicEditBox(Font font, int x, int y, int width, Component message) {
        super(font, x, y + TEXT_OFFSET_Y, width, 8, message);
        setBordered(false);
        setTextColor(0xFFFFFFFF);
        setTextShadow(false);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, FIELD, getX() - 2, getY() - TEXT_OFFSET_Y, 0.0F, 0.0F, FIELD_WIDTH, FIELD_HEIGHT, FIELD_WIDTH, FIELD_HEIGHT);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getX() - 2 && mouseX < getX() + getWidth() + 2 && mouseY >= getY() - TEXT_OFFSET_Y && mouseY < getY() - TEXT_OFFSET_Y + FIELD_HEIGHT;
    }
}
