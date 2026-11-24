package com.wudji.villagertradingoverhanful.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.mixin.SlotPositionAccessor;
import com.wudji.villagertradingoverhanful.platform.Services;
import com.wudji.villagertradingoverhanful.preferences.TradingDeskPreferences;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;


import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TradingDeskScreen extends AbstractContainerScreen<MerchantMenu> {
    private static final int PANEL_WIDTH = 352;
    private static final int PANEL_HEIGHT = 204;
    private static final int LEFT_PANEL_WIDTH = 176;
    private static final int RIGHT_PANEL_X = LEFT_PANEL_WIDTH;
    private static final int ROW_HEIGHT = 27;
    private static final int OFFER_BUTTON_HEIGHT = 24;
    private static final int HEADER_HEIGHT = 39;
    private static final int MAX_OFFER_BUTTONS = 5;
    private static final int OFFER_BUTTON_X = 5;
    private static final int OFFER_BUTTON_WIDTH = 157;
    private static final int LIST_FRAME_X = 4;
    private static final int LIST_FRAME_Y = 38;
    private static final int LIST_FRAME_WIDTH = 168;
    private static final int LIST_FRAME_HEIGHT = 140;
    private static final int SCROLLBAR_X = 164;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_HEIGHT = MAX_OFFER_BUTTONS * ROW_HEIGHT;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int MUTED_COLOR = 0xFF707070;
    private static final int ROW_MUTED_COLOR = 0xFFC0C0C0;
    private static final int OUT_OF_STOCK_COLOR = 0xFFFF6060;
    private static final int BUTTON_TEXT_COLOR = 0xFFFFFFFF;
    private static final int EXPERIENCE_BAR_WIDTH = 28;
    private static final int EXPERIENCE_BAR_HEIGHT = 5;
    private static final int PANEL_BORDER = 4;
    private static final ResourceLocation PANEL_TEXTURE = texture("panel");
    private static final ResourceLocation LIST_TEXTURE = texture("trade_list");
    private static final ResourceLocation SLOT_TEXTURE = texture("slot");
    private static final ResourceLocation FAVORITE_STAR = texture("favorite_star");
    private static final ResourceLocation FAVORITE_STAR_EMPTY = texture("favorite_star_empty");
    private static final ResourceLocation EXPERIENCE_TEXTURE = texture("experience_bar_current");
    private static final ResourceLocation TRADE_ARROW_TEXTURE = texture("trade_arrow");
    private static final ResourceLocation TRADE_ARROW_OUT_OF_STOCK_TEXTURE = texture("trade_arrow_out_of_stock");
    private static final ResourceLocation SCROLLER_TEXTURE = texture("scroller");
    private static final ResourceLocation SCROLLER_DISABLED_TEXTURE = texture("scroller_disabled");
    private static final ResourceLocation SCROLLER_BACKGROUND_TEXTURE = texture("scroller_background");

    private final List<Integer> visibleOffers = new ArrayList<>();
    private final Inventory inventory;
    private OfferFilter filter = OfferFilter.ALL;
    private OfferOrder order = OfferOrder.VANILLA;
    private int selectedOffer = -1;
    private int scrollRow;
    private int queuedTrades;
    private int knownOfferCount = -1;
    private int pendingDestinationSlot = -1;
    private EditBox amountInput;
    private OfferButton[] offerButtons = new OfferButton[0];
    private ItemStack tooltipStack;
    private boolean offerTooltipHovered;

    public TradingDeskScreen(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.inventory = inventory;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = Math.max(0, (width - PANEL_WIDTH) / 2);
        topPos = Math.max(12, (height - PANEL_HEIGHT) / 2);
        arrangeSlots();
        createOfferButtons();
        refreshOffers();

        int listX = getListX();
        int rightX = getRightX();
        int controlWidth = (getListWidth() - 16) / 3;
        addRenderableWidget(new VanillaButton(font, listX + 5, topPos + 16, controlWidth, filter.label(), button -> {
            filter = filter.next();
            scrollRow = 0;
            refreshOffers();
            button.setMessage(filter.label());
        }));
        addRenderableWidget(new VanillaButton(font, listX + 8 + controlWidth, topPos + 16, controlWidth, order.label(), button -> {
            order = order.next();
            refreshOffers();
            button.setMessage(order.label());
        }));
        addRenderableWidget(new VanillaButton(font, listX + 11 + controlWidth * 2, topPos + 16, controlWidth, Component.translatable("screen.villagertradingoverhanful.options"), button -> Services.PLATFORM.openConfigScreen(this)));
        addRenderableWidget(new VanillaButton(font, rightX + 11, topPos + 66, 73, Component.translatable("screen.villagertradingoverhanful.trade_once"), button -> beginTrades(1)));
        addRenderableWidget(new VanillaButton(font, rightX + 90, topPos + 66, 75, Component.translatable("screen.villagertradingoverhanful.trade_stack"), button -> beginTrades(TradingDeskPreferences.getBatchLimit())));

        amountInput = addRenderableWidget(new EditBox(font, rightX + 11, topPos + 90, 38, 20, Component.translatable("screen.villagertradingoverhanful.amount")));
        amountInput.setValue("1");
        amountInput.setMaxLength(3);
        amountInput.setHint(Component.translatable("screen.villagertradingoverhanful.amount"));
        amountInput.setTextColor(BUTTON_TEXT_COLOR);
        addRenderableWidget(new VanillaButton(font, rightX + 53, topPos + 90, 22, Component.literal("−"), button -> changeAmount(-1)));
        addRenderableWidget(new VanillaButton(font, rightX + 79, topPos + 90, 22, Component.literal("+"), button -> changeAmount(1)));
        addRenderableWidget(new VanillaButton(font, rightX + 105, topPos + 90, 60, Component.translatable("screen.villagertradingoverhanful.trade_amount"), button -> beginTrades(readAmount())));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, PANEL_WIDTH, PANEL_HEIGHT);
        drawListFrame(graphics, leftPos + LIST_FRAME_X, topPos + LIST_FRAME_Y, LIST_FRAME_WIDTH, LIST_FRAME_HEIGHT);

        int rightX = getRightX();
        int experienceFill = getExperienceFill();
        if (experienceFill >= 0) {
            int experienceX = rightX + 168 - EXPERIENCE_BAR_WIDTH;
            int experienceY = topPos + 10;
            graphics.fill(experienceX, experienceY, experienceX + EXPERIENCE_BAR_WIDTH, experienceY + EXPERIENCE_BAR_HEIGHT, 0xFF0E1110);
            graphics.fill(experienceX, experienceY, experienceX + EXPERIENCE_BAR_WIDTH, experienceY + 1, 0xFF000000);
            graphics.fill(experienceX, experienceY + EXPERIENCE_BAR_HEIGHT - 1, experienceX + EXPERIENCE_BAR_WIDTH, experienceY + EXPERIENCE_BAR_HEIGHT, 0xFF000000);
            graphics.fill(experienceX, experienceY, experienceX + 1, experienceY + EXPERIENCE_BAR_HEIGHT, 0xFF000000);
            graphics.fill(experienceX + EXPERIENCE_BAR_WIDTH - 1, experienceY, experienceX + EXPERIENCE_BAR_WIDTH, experienceY + EXPERIENCE_BAR_HEIGHT, 0xFF000000);
        }

        for (Slot slot : menu.slots) {
            drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }

        if (selectedOffer >= 0 && selectedOffer < menu.getOffers().size()) {
            MerchantOffer offer = menu.getOffers().get(selectedOffer);
            ResourceLocation arrow = offer.isOutOfStock() ? TRADE_ARROW_OUT_OF_STOCK_TEXTURE : TRADE_ARROW_TEXTURE;
            Slot secondInputSlot = menu.slots.get(1);
            Slot resultSlot = menu.slots.get(2);
            int arrowX = leftPos + (secondInputSlot.x + 16 + resultSlot.x - 10) / 2;
            int arrowY = topPos + secondInputSlot.y + 3;
            blit(graphics, arrow, arrowX, arrowY, 0, 0, 10, 9, 10, 9);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int rightX = RIGHT_PANEL_X;
        Component merchantTitle = getMerchantTitle();
        int experienceFill = getExperienceFill();
        if (experienceFill < 0) {
            graphics.drawString(font, merchantTitle, rightX + 88 - font.width(merchantTitle) / 2, 7, TEXT_COLOR, false);
        } else {
            int experienceX = 168 - EXPERIENCE_BAR_WIDTH;
            String visibleTitle = font.plainSubstrByWidth(merchantTitle.getString(), experienceX - 20);
            graphics.drawString(font, visibleTitle, rightX + 8, 7, TEXT_COLOR, false);
            if (experienceFill > 0) {
                blit(graphics, EXPERIENCE_TEXTURE, rightX + experienceX + 1, 11, 1, 1, experienceFill, 3, 102, 5);
            }
        }
        graphics.drawString(font, Component.translatable("screen.villagertradingoverhanful.offer_inputs"), rightX + 8, 20, MUTED_COLOR, false);
        graphics.drawString(font, Component.translatable("screen.villagertradingoverhanful.inventory"), rightX + 8, 111, MUTED_COLOR, false);
        if (queuedTrades > 0) {
            graphics.drawString(font, Component.translatable("screen.villagertradingoverhanful.queue", queuedTrades), rightX + 8, 56, TEXT_COLOR, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        tooltipStack = null;
        offerTooltipHovered = false;
        super.render(graphics, mouseX, mouseY, delta);
        renderResultPreview(graphics);
        drawScrollbar(graphics);

        int footerY = topPos + HEADER_HEIGHT + MAX_OFFER_BUTTONS * ROW_HEIGHT + 2;
        graphics.drawString(font, Component.translatable("screen.villagertradingoverhanful.offer_count", visibleOffers.isEmpty() ? 0 : scrollRow + 1, Math.min(scrollRow + MAX_OFFER_BUTTONS, visibleOffers.size()), visibleOffers.size()), leftPos + 7, footerY + 8, MUTED_COLOR, false);

        if (tooltipStack != null) {
            graphics.renderTooltip(font, tooltipStack, mouseX, mouseY);
        } else if (offerTooltipHovered) {
            graphics.renderTooltip(font, Component.translatable("screen.villagertradingoverhanful.offer_tooltip"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount != 0.0D && amountInput != null && amountInput.isMouseOver(mouseX, mouseY)) {
            changeAmount((int) Math.signum(amount));
            return true;
        }
        if (mouseX >= leftPos + LIST_FRAME_X && mouseX < leftPos + LIST_FRAME_X + LIST_FRAME_WIDTH && mouseY >= topPos + HEADER_HEIGHT && mouseY < topPos + HEADER_HEIGHT + SCROLLBAR_HEIGHT) {
            int maximumScroll = Math.max(0, visibleOffers.size() - MAX_OFFER_BUTTONS);
            scrollRow = Math.max(0, Math.min(maximumScroll, scrollRow - (int) Math.signum(amount)));
            syncOfferButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return mouseX < leftPos || mouseX >= leftPos + PANEL_WIDTH || mouseY < topPos || mouseY >= topPos + PANEL_HEIGHT;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (knownOfferCount != menu.getOffers().size()) {
            knownOfferCount = menu.getOffers().size();
            refreshOffers();
            if (!visibleOffers.isEmpty()) {
                selectOffer(visibleOffers.get(0));
            }
        }
        if (pendingDestinationSlot >= 0) {
            minecraft.gameMode.handleInventoryMouseClick(menu.containerId, pendingDestinationSlot, 0, ClickType.PICKUP, minecraft.player);
            pendingDestinationSlot = -1;
            queuedTrades--;
            return;
        }

        if (queuedTrades <= 0 || selectedOffer < 0 || selectedOffer >= menu.getOffers().size()) {
            return;
        }

        MerchantOffer offer = menu.getOffers().get(selectedOffer);
        ItemStack result = menu.slots.get(2).getItem();
        if (offer.isOutOfStock() || result.isEmpty() || !ItemStack.isSameItemSameTags(result, offer.getResult()) || result.getCount() != offer.getResult().getCount()) {
            queuedTrades = 0;
            return;
        }

        pendingDestinationSlot = findDestinationSlot(result);
        if (pendingDestinationSlot < 0) {
            queuedTrades = 0;
            return;
        }
        minecraft.gameMode.handleInventoryMouseClick(menu.containerId, 2, 0, ClickType.PICKUP, minecraft.player);
    }

    private void createOfferButtons() {
        offerButtons = new OfferButton[MAX_OFFER_BUTTONS];
        for (int row = 0; row < MAX_OFFER_BUTTONS; row++) {
            OfferButton button = new OfferButton(this, leftPos + OFFER_BUTTON_X, topPos + HEADER_HEIGHT + row * ROW_HEIGHT, OFFER_BUTTON_WIDTH, OFFER_BUTTON_HEIGHT);
            addRenderableWidget(button);
            offerButtons[row] = button;
        }
    }

    private void syncOfferButtons() {
        if (offerButtons == null) {
            return;
        }
        for (int row = 0; row < offerButtons.length; row++) {
            int offerIndex = scrollRow + row < visibleOffers.size() ? visibleOffers.get(scrollRow + row) : -1;
            offerButtons[row].setOfferIndex(offerIndex);
        }
    }

    private void renderOfferContents(GuiGraphics graphics, Button button, int offerIndex, int mouseX, int mouseY) {
        MerchantOffer offer = menu.getOffers().get(offerIndex);
        boolean favorite = TradingDeskPreferences.isFavorite(offerKey(offer));
        ItemStack hoveredStack = null;
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int itemY = y + 4;

        if (offerIndex == selectedOffer) {
            int height = button.getHeight();
            graphics.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
            graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
            graphics.fill(x, y, x + 1, y + height, 0xFFFFFFFF);
            graphics.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);
        }

        renderItem(graphics, offer.getCostA(), x + 5, itemY);
        if (isHoveringItem(mouseX, mouseY, x + 5, itemY)) {
            hoveredStack = offer.getCostA();
        }

        if (!offer.getCostB().isEmpty()) {
            renderItem(graphics, offer.getCostB(), x + 35, itemY);
            if (isHoveringItem(mouseX, mouseY, x + 35, itemY)) {
                hoveredStack = offer.getCostB();
            }
        }

        ResourceLocation arrow = offer.isOutOfStock() ? TRADE_ARROW_OUT_OF_STOCK_TEXTURE : TRADE_ARROW_TEXTURE;
        blit(graphics, arrow, x + 54, y + 7, 0, 0, 10, 9, 10, 9);

        renderItem(graphics, offer.getResult(), x + 68, itemY);
        if (hoveredStack == null && isHoveringItem(mouseX, mouseY, x + 68, itemY)) {
            hoveredStack = offer.getResult();
        }

        int textWidth = width - 86 - 20;
        String name = font.plainSubstrByWidth(offer.getResult().getHoverName().getString(), Math.max(0, textWidth));
        int nameColor = offer.isOutOfStock() ? OUT_OF_STOCK_COLOR : BUTTON_TEXT_COLOR;
        graphics.drawString(font, name, x + 86, y + 3, nameColor, false);

        Component stock = offer.isOutOfStock()
                ? Component.translatable("screen.villagertradingoverhanful.sold_out")
                : Component.translatable("screen.villagertradingoverhanful.stock_left", offer.getMaxUses() - offer.getUses());
        int stockColor = offer.isOutOfStock() ? OUT_OF_STOCK_COLOR : ROW_MUTED_COLOR;
        graphics.drawString(font, font.plainSubstrByWidth(stock.getString(), Math.max(0, textWidth)), x + 86, y + 13, stockColor, false);

        blit(graphics, favorite ? FAVORITE_STAR : FAVORITE_STAR_EMPTY, x + width - 18, y + 4, 0, 0, 16, 16, 16, 16);

        if (hoveredStack != null) {
            tooltipStack = hoveredStack;
        } else if (button.isMouseOver(mouseX, mouseY)) {
            offerTooltipHovered = true;
        }
    }

    private void renderResultPreview(GuiGraphics graphics) {
        if (selectedOffer < 0 || selectedOffer >= menu.getOffers().size() || !menu.slots.get(2).getItem().isEmpty()) {
            return;
        }
        Slot resultSlot = menu.slots.get(2);
        renderItem(graphics, menu.getOffers().get(selectedOffer).getResult(), leftPos + resultSlot.x, topPos + resultSlot.y);
    }

    private void renderItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int trackX = leftPos + SCROLLBAR_X;
        int trackY = topPos + HEADER_HEIGHT;
        blit(graphics, SCROLLER_BACKGROUND_TEXTURE, trackX, trackY, 0, 0, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 6, 32);

        int maximumScroll = Math.max(0, visibleOffers.size() - MAX_OFFER_BUTTONS);
        if (maximumScroll <= 0) {
            blit(graphics, SCROLLER_DISABLED_TEXTURE, trackX, trackY, 0, 0, SCROLLBAR_WIDTH, 27, 6, 27);
            return;
        }

        int thumbY = trackY + scrollRow * (SCROLLBAR_HEIGHT - 27) / maximumScroll;
        blit(graphics, SCROLLER_TEXTURE, trackX, thumbY, 0, 0, SCROLLBAR_WIDTH, 27, 6, 27);
    }

    private void toggleFavorite(int offerIndex) {
        TradingDeskPreferences.toggleFavorite(offerKey(menu.getOffers().get(offerIndex)));
        refreshOffers();
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        int border = PANEL_BORDER;
        int contentWidth = 176;
        int contentHeight = 222;
        int textureWidth = 256;
        int textureHeight = 256;
        blit(graphics, PANEL_TEXTURE, x, y, 0, 0, border, border, textureWidth, textureHeight);
        blit(graphics, PANEL_TEXTURE, x + width - border, y, contentWidth - border, 0, border, border, textureWidth, textureHeight);
        blit(graphics, PANEL_TEXTURE, x, y + height - border, 0, contentHeight - border, border, border, textureWidth, textureHeight);
        blit(graphics, PANEL_TEXTURE, x + width - border, y + height - border, contentWidth - border, contentHeight - border, border, border, textureWidth, textureHeight);
        blit(graphics, PANEL_TEXTURE, x + border, y, border, 0, width - border * 2, border, textureWidth, textureHeight);
        blit(graphics, PANEL_TEXTURE, x + border, y + height - border, border, contentHeight - border, width - border * 2, border, textureWidth, textureHeight);
        blit(graphics, PANEL_TEXTURE, x, y + border, 0, border, border, height - border * 2, textureWidth, textureHeight);
        blit(graphics, PANEL_TEXTURE, x + width - border, y + border, contentWidth - border, border, border, height - border * 2, textureWidth, textureHeight);
        blit(graphics, PANEL_TEXTURE, x + border, y + border, border, border, width - border * 2, height - border * 2, textureWidth, textureHeight);
    }

    private static void drawListFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        int textureWidth = 512;
        int textureHeight = 256;
        blit(graphics, LIST_TEXTURE, x, y, 4, 17, width, 1, textureWidth, textureHeight);
        blit(graphics, LIST_TEXTURE, x, y, 4, 17, 1, height, textureWidth, textureHeight);
        blit(graphics, LIST_TEXTURE, x, y + height - 1, 5, 158, width, 1, textureWidth, textureHeight);
        blit(graphics, LIST_TEXTURE, x + width - 1, y, 100, 20, 1, height, textureWidth, textureHeight);
        blit(graphics, LIST_TEXTURE, x + 1, y + 1, 5, 18, width - 2, height - 2, textureWidth, textureHeight);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        blit(graphics, SLOT_TEXTURE, x, y, 0, 0, 18, 18, 18, 18);
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Constants.MOD_ID, "textures/gui/" + name + ".png");
    }

    private boolean isHoveringItem(int mouseX, int mouseY, int itemX, int itemY) {
        return mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16;
    }

    private void beginTrades(int requestedTrades) {
        if (selectedOffer < 0 || selectedOffer >= menu.getOffers().size()) {
            return;
        }
        MerchantOffer offer = menu.getOffers().get(selectedOffer);
        selectOffer(selectedOffer);
        pendingDestinationSlot = -1;
        queuedTrades = Math.min(requestedTrades, Math.min(offer.getMaxUses() - offer.getUses(), getAffordableTradeCount(offer)));
    }

    private void selectOffer(int offerIndex) {
        selectedOffer = offerIndex;
        menu.setSelectionHint(offerIndex);
        menu.tryMoveItems(offerIndex);
        menu.slotsChanged(menu.slots.get(0).container);
        minecraft.getConnection().send(new ServerboundSelectTradePacket(offerIndex));
    }

    private void refreshOffers() {
        visibleOffers.clear();
        for (int offerIndex = 0; offerIndex < menu.getOffers().size(); offerIndex++) {
            MerchantOffer offer = menu.getOffers().get(offerIndex);
            if (filter.matches(this, offer) && (!TradingDeskPreferences.shouldHideUnavailable() || !offer.isOutOfStock())) {
                visibleOffers.add(offerIndex);
            }
        }
        visibleOffers.sort(order.comparator(this));
        int maximumScroll = Math.max(0, visibleOffers.size() - MAX_OFFER_BUTTONS);
        scrollRow = Math.min(scrollRow, maximumScroll);
        syncOfferButtons();
    }

    private void arrangeSlots() {
        setSlotPosition(0, RIGHT_PANEL_X + 17, 38);
        setSlotPosition(1, RIGHT_PANEL_X + 47, 38);
        setSlotPosition(2, RIGHT_PANEL_X + 142, 38);
        for (int slotIndex = 3; slotIndex < menu.slots.size(); slotIndex++) {
            int inventoryIndex = slotIndex - 3;
            int column = inventoryIndex % 9;
            int row = inventoryIndex / 9;
            int y = row < 3 ? 122 + row * 18 : 176;
            setSlotPosition(slotIndex, RIGHT_PANEL_X + 8 + column * 18, y);
        }
    }

    private void setSlotPosition(int slotIndex, int x, int y) {
        SlotPositionAccessor slot = (SlotPositionAccessor) menu.slots.get(slotIndex);
        slot.villagerTradingOverhaul$setX(x);
        slot.villagerTradingOverhaul$setY(y);
    }

    private int findDestinationSlot(ItemStack result) {
        for (int slotIndex = 3; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            ItemStack stack = slot.getItem();
            if (ItemStack.isSameItemSameTags(stack, result) && stack.getCount() + result.getCount() <= slot.getMaxStackSize(result)) {
                return slotIndex;
            }
        }
        for (int slotIndex = 3; slotIndex < menu.slots.size(); slotIndex++) {
            if (menu.slots.get(slotIndex).getItem().isEmpty()) {
                return slotIndex;
            }
        }
        return -1;
    }

    private void changeAmount(int change) {
        amountInput.setValue(Integer.toString(Math.max(1, Math.min(999, readAmount() + change))));
    }

    private int readAmount() {
        try {
            return Math.max(1, Math.min(999, Integer.parseInt(amountInput.getValue())));
        } catch (NumberFormatException exception) {
            amountInput.setValue("1");
            return 1;
        }
    }

    private Component getMerchantTitle() {
        int traderLevel = menu.getTraderLevel();
        if (traderLevel > 0 && traderLevel <= 5 && menu.showProgressBar()) {
            return Component.translatable("merchant.title", title, Component.translatable("merchant.level." + traderLevel));
        }
        return title;
    }

    private int getExperienceFill() {
        int traderLevel = menu.getTraderLevel();
        if (!menu.showProgressBar() || !VillagerData.canLevelUp(traderLevel)) {
            return -1;
        }
        int minimumExperience = VillagerData.getMinXpPerLevel(traderLevel);
        int maximumExperience = VillagerData.getMaxXpPerLevel(traderLevel);
        int range = maximumExperience - minimumExperience;
        if (range <= 0) {
            return -1;
        }
        return Math.max(0, Math.min(EXPERIENCE_BAR_WIDTH - 2, (menu.getTraderXp() - minimumExperience) * (EXPERIENCE_BAR_WIDTH - 2) / range));
    }

    private boolean canAfford(MerchantOffer offer) {
        return countAvailable(offer.getCostA()) >= offer.getCostA().getCount() && (offer.getCostB().isEmpty() || countAvailable(offer.getCostB()) >= offer.getCostB().getCount());
    }

    private int getAffordableTradeCount(MerchantOffer offer) {
        ItemStack costA = offer.getCostA();
        ItemStack costB = offer.getCostB();
        int available = countAvailable(costA);
        if (costB.isEmpty()) {
            return available / costA.getCount();
        }
        if (ItemStack.isSameItemSameTags(costA, costB)) {
            return available / (costA.getCount() + costB.getCount());
        }
        return Math.min(available / costA.getCount(), countAvailable(costB) / costB.getCount());
    }

    private int countAvailable(ItemStack wanted) {
        int count = 0;
        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            if (slotIndex == 2) {
                continue;
            }
            ItemStack stack = menu.slots.get(slotIndex).getItem();
            if (ItemStack.isSameItemSameTags(stack, wanted)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private String offerKey(MerchantOffer offer) {
        return offer.getCostA() + ">" + offer.getCostB() + ">" + offer.getResult();
    }

    private int getListX() {
        return leftPos;
    }

    private int getListWidth() {
        return LEFT_PANEL_WIDTH;
    }

    private int getRightX() {
        return leftPos + RIGHT_PANEL_X;
    }

    private static final class OfferButton extends Button {
        private final TradingDeskScreen screen;
        private int offerIndex = -1;

        private OfferButton(TradingDeskScreen screen, int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), button -> {
                OfferButton self = (OfferButton) button;
                if (self.offerIndex >= 0) {
                    self.screen.selectOffer(self.offerIndex);
                }
            }, DEFAULT_NARRATION);
            this.screen = screen;
        }

        private void setOfferIndex(int offerIndex) {
            this.offerIndex = offerIndex;
            this.visible = offerIndex >= 0;
            this.active = offerIndex >= 0;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!visible || offerIndex < 0) {
                return;
            }
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            screen.renderOfferContents(graphics, this, offerIndex, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (visible && active && offerIndex >= 0 && button == InputConstants.MOUSE_BUTTON_RIGHT
                    && mouseX >= getX() && mouseX < getX() + getWidth()
                    && mouseY >= getY() && mouseY < getY() + getHeight()) {
                screen.toggleFavorite(offerIndex);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    private enum OfferFilter {
        ALL("screen.villagertradingoverhanful.filter.all") {
            @Override
            boolean matches(TradingDeskScreen screen, MerchantOffer offer) {
                return true;
            }
        },
        READY("screen.villagertradingoverhanful.filter.ready") {
            @Override
            boolean matches(TradingDeskScreen screen, MerchantOffer offer) {
                return !offer.isOutOfStock() && screen.canAfford(offer);
            }
        },
        STARRED("screen.villagertradingoverhanful.filter.starred") {
            @Override
            boolean matches(TradingDeskScreen screen, MerchantOffer offer) {
                return TradingDeskPreferences.isFavorite(screen.offerKey(offer));
            }
        };

        private final String translationKey;

        OfferFilter(String translationKey) {
            this.translationKey = translationKey;
        }

        Component label() {
            return Component.translatable(translationKey);
        }

        abstract boolean matches(TradingDeskScreen screen, MerchantOffer offer);

        OfferFilter next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private enum OfferOrder {
        VANILLA("screen.villagertradingoverhanful.order.vanilla"),
        COST("screen.villagertradingoverhanful.order.cost"),
        STOCK("screen.villagertradingoverhanful.order.stock");

        private final String translationKey;

        OfferOrder(String translationKey) {
            this.translationKey = translationKey;
        }

        Component label() {
            return Component.translatable(translationKey);
        }

        Comparator<Integer> comparator(TradingDeskScreen screen) {
            Comparator<Integer> favoriteFirst = Comparator.comparing((Integer index) -> !TradingDeskPreferences.isFavorite(screen.offerKey(screen.menu.getOffers().get(index))));
            return switch (this) {
                case VANILLA -> favoriteFirst.thenComparingInt(Integer::intValue);
                case COST -> favoriteFirst.thenComparingInt(index -> screen.menu.getOffers().get(index).getCostA().getCount());
                case STOCK -> favoriteFirst.thenComparing(Comparator.comparingInt((Integer index) -> screen.menu.getOffers().get(index).getMaxUses() - screen.menu.getOffers().get(index).getUses()).reversed());
            };
        }

        OfferOrder next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }
}




