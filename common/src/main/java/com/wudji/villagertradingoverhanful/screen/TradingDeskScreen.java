package com.wudji.villagertradingoverhanful.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.mixin.SlotPositionAccessor;
import com.wudji.villagertradingoverhanful.platform.Services;
import com.wudji.villagertradingoverhanful.preferences.TradingDeskPreferences;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
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
    private static final int RIGHT_PANEL_WIDTH = 176;
    private static final int RIGHT_PANEL_X = LEFT_PANEL_WIDTH;
    private static final int ROW_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 39;
    private static final int FOOTER_HEIGHT = 25;
    private static final int MAX_OFFER_BUTTONS = 7;
    private static final int OFFER_BUTTON_X = 5;
    private static final int OFFER_BUTTON_WIDTH = 157;
    private static final int LIST_FRAME_X = 4;
    private static final int LIST_FRAME_Y = 38;
    private static final int LIST_FRAME_WIDTH = 168;
    private static final int LIST_FRAME_HEIGHT = 142;
    private static final int SCROLLBAR_X = 164;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_HEIGHT = MAX_OFFER_BUTTONS * ROW_HEIGHT;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int MUTED_COLOR = 0xFF707070;
    private static final int OUT_OF_STOCK_COLOR = 0xFFFF6060;
    private static final int BUTTON_TEXT_COLOR = 0xFFFFFFFF;
    private static final int EXPERIENCE_BAR_WIDTH = 28;
    private static final int EXPERIENCE_BAR_HEIGHT = 5;
    private static final Identifier PANEL_TEXTURE = texture("classic_panel");
    private static final Identifier SLOT_TEXTURE = texture("classic_slot");
    private static final Identifier FAVORITE_STAR = texture("favorite_star");
    private static final Identifier FAVORITE_STAR_EMPTY = texture("favorite_star_empty");
    private static final Identifier EXPERIENCE_BAR_CURRENT_SPRITE = Identifier.withDefaultNamespace("container/villager/experience_bar_current");
    private static final Identifier EXPERIENCE_BAR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/villager/experience_bar_background");
    private static final Identifier TRADE_ARROW_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow");
    private static final Identifier TRADE_ARROW_OUT_OF_STOCK_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow_out_of_stock");
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller_disabled");
    private static final Identifier SCROLLER_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("widget/scroller_background");

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

    public TradingDeskScreen(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PANEL_WIDTH, PANEL_HEIGHT);
        this.inventory = inventory;
    }

    @Override
    protected void init() {
        super.init();
        arrangeSlots();
        createOfferButtons();
        refreshOffers();

        int listX = getListX();
        int rightX = getRightX();
        int controlWidth = (getListWidth() - 16) / 3;
        addRenderableWidget(new VanillaButton(font, listX + 5, topPos + 18, controlWidth, filter.label(), button -> {
            filter = filter.next();
            scrollRow = 0;
            refreshOffers();
            button.setMessage(filter.label());
        }));
        addRenderableWidget(new VanillaButton(font, listX + 8 + controlWidth, topPos + 18, controlWidth, order.label(), button -> {
            order = order.next();
            refreshOffers();
            button.setMessage(order.label());
        }));
        addRenderableWidget(new VanillaButton(font, listX + 11 + controlWidth * 2, topPos + 18, controlWidth, Component.translatable("screen.villagertradingoverhanful.options"), button -> Services.PLATFORM.openConfigScreen(this)));
        addRenderableWidget(new VanillaButton(font, rightX + 11, topPos + 66, 73, Component.translatable("screen.villagertradingoverhanful.trade_once"), button -> beginTrades(1)));
        addRenderableWidget(new VanillaButton(font, rightX + 90, topPos + 66, 75, Component.translatable("screen.villagertradingoverhanful.trade_stack"), button -> beginTrades(TradingDeskPreferences.getBatchLimit())));

        amountInput = addRenderableWidget(new EditBox(font, rightX + 11, topPos + 90, 38, 20, Component.translatable("screen.villagertradingoverhanful.amount")));
        amountInput.setValue("1");
        amountInput.setMaxLength(3);
        amountInput.setHint(Component.translatable("screen.villagertradingoverhanful.amount"));
        amountInput.setTextColor(BUTTON_TEXT_COLOR);
        amountInput.setTextShadow(false);
        addRenderableWidget(new VanillaButton(font, rightX + 53, topPos + 90, 22, Component.literal("−"), button -> changeAmount(-1)));
        addRenderableWidget(new VanillaButton(font, rightX + 79, topPos + 90, 22, Component.literal("+"), button -> changeAmount(1)));
        addRenderableWidget(new VanillaButton(font, rightX + 105, topPos + 90, 60, Component.translatable("screen.villagertradingoverhanful.trade_amount"), button -> beginTrades(readAmount())));

        if (Services.TRADE_CYCLING.isLoaded()) {
            int footerY = topPos + HEADER_HEIGHT + MAX_OFFER_BUTTONS * ROW_HEIGHT;
            AbstractWidget cycleButton = TradeCyclingButtonFactory.create(listX + LEFT_PANEL_WIDTH - 23, footerY + 4, menu, inventory, getTitle(), () -> Services.TRADE_CYCLING.cycleTrades());
            if (cycleButton != null) {
                addRenderableWidget(cycleButton);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (isInGameUi()) {
            extractTransparentBackground(graphics);
        } else {
            extractBlurredBackground(graphics);
            extractMenuBackground(graphics);
        }
        minecraft.gui.hud.extractDeferredSubtitles();

        drawPanel(graphics, leftPos, topPos, PANEL_WIDTH, PANEL_HEIGHT);
        drawListFrame(graphics, leftPos + LIST_FRAME_X, topPos + LIST_FRAME_Y, LIST_FRAME_WIDTH, LIST_FRAME_HEIGHT);

        int rightX = getRightX();
        int experienceFill = getExperienceFill();
        if (experienceFill >= 0) {
            int experienceX = rightX + 168 - EXPERIENCE_BAR_WIDTH;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_BACKGROUND_SPRITE, experienceX, topPos + 10, EXPERIENCE_BAR_WIDTH, EXPERIENCE_BAR_HEIGHT);
        }

        for (Slot slot : menu.slots) {
            drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }

        if (selectedOffer >= 0 && selectedOffer < menu.getOffers().size()) {
            MerchantOffer offer = menu.getOffers().get(selectedOffer);
            Identifier arrowSprite = offer.isOutOfStock() ? TRADE_ARROW_OUT_OF_STOCK_SPRITE : TRADE_ARROW_SPRITE;
            Slot secondInputSlot = menu.slots.get(1);
            Slot resultSlot = menu.slots.get(2);
            int arrowX = leftPos + (secondInputSlot.x + 16 + resultSlot.x - 10) / 2;
            int arrowY = topPos + secondInputSlot.y + 3;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, arrowSprite, arrowX, arrowY, 10, 9);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int rightX = RIGHT_PANEL_X;
        Component merchantTitle = getMerchantTitle();
        int experienceFill = getExperienceFill();
        if (experienceFill < 0) {
            graphics.text(font, merchantTitle, rightX + 88 - font.width(merchantTitle) / 2, 7, TEXT_COLOR, false);
        } else {
            int experienceX = 168 - EXPERIENCE_BAR_WIDTH;
            String visibleTitle = font.plainSubstrByWidth(merchantTitle.getString(), experienceX - 20);
            graphics.text(font, visibleTitle, rightX + 8, 7, TEXT_COLOR, false);
            if (experienceFill > 0) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_CURRENT_SPRITE, rightX + experienceX, 10, experienceFill, EXPERIENCE_BAR_HEIGHT);
            }
        }
        graphics.text(font, Component.translatable("screen.villagertradingoverhanful.offer_inputs"), rightX + 8, 20, MUTED_COLOR, false);
        graphics.text(font, Component.translatable("screen.villagertradingoverhanful.inventory"), rightX + 8, 111, MUTED_COLOR, false);
        if (queuedTrades > 0) {
            graphics.text(font, Component.translatable("screen.villagertradingoverhanful.queue", queuedTrades), rightX + 8, 56, TEXT_COLOR, false);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractContents(graphics, mouseX, mouseY, delta);
        renderResultPreview(graphics);
        drawScrollbar(graphics);

        int footerY = topPos + HEADER_HEIGHT + MAX_OFFER_BUTTONS * ROW_HEIGHT;
        graphics.text(font, Component.translatable("screen.villagertradingoverhanful.offer_count", visibleOffers.isEmpty() ? 0 : scrollRow + 1, Math.min(scrollRow + MAX_OFFER_BUTTONS, visibleOffers.size()), visibleOffers.size()), leftPos + 7, footerY + 8, MUTED_COLOR, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= leftPos + LIST_FRAME_X && mouseX < leftPos + LIST_FRAME_X + LIST_FRAME_WIDTH && mouseY >= topPos + HEADER_HEIGHT && mouseY < topPos + HEADER_HEIGHT + SCROLLBAR_HEIGHT) {
            int maximumScroll = Math.max(0, visibleOffers.size() - MAX_OFFER_BUTTONS);
            scrollRow = Math.max(0, Math.min(maximumScroll, scrollRow - (int) Math.signum(verticalAmount)));
            syncOfferButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (Services.TRADE_CYCLING.isLoaded() && Services.TRADE_CYCLING.matchesCycleKey(event)) {
            if (canCycleTrades()) {
                Services.TRADE_CYCLING.cycleTrades();
                playCycleSound();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    private boolean canCycleTrades() {
        return menu.showProgressBar() && menu.getTraderXp() <= 0;
    }

    private void playCycleSound() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (knownOfferCount != menu.getOffers().size()) {
            knownOfferCount = menu.getOffers().size();
            refreshOffers();
            if (!visibleOffers.isEmpty()) {
                selectOffer(visibleOffers.getFirst());
            }
        }
        if (pendingDestinationSlot >= 0) {
            minecraft.gameMode.handleContainerInput(menu.containerId, pendingDestinationSlot, 0, ContainerInput.PICKUP, minecraft.player);
            pendingDestinationSlot = -1;
            queuedTrades--;
            return;
        }

        if (queuedTrades <= 0 || selectedOffer < 0 || selectedOffer >= menu.getOffers().size()) {
            return;
        }

        MerchantOffer offer = menu.getOffers().get(selectedOffer);
        ItemStack result = menu.slots.get(2).getItem();
        if (offer.isOutOfStock() || result.isEmpty() || !ItemStack.isSameItemSameComponents(result, offer.getResult()) || result.getCount() != offer.getResult().getCount()) {
            queuedTrades = 0;
            return;
        }

        pendingDestinationSlot = findDestinationSlot(result);
        if (pendingDestinationSlot < 0) {
            queuedTrades = 0;
            return;
        }
        minecraft.gameMode.handleContainerInput(menu.containerId, 2, 0, ContainerInput.PICKUP, minecraft.player);
    }

    private void createOfferButtons() {
        offerButtons = new OfferButton[MAX_OFFER_BUTTONS];
        for (int row = 0; row < MAX_OFFER_BUTTONS; row++) {
            OfferButton button = new OfferButton(this, leftPos + OFFER_BUTTON_X, topPos + HEADER_HEIGHT + row * ROW_HEIGHT, OFFER_BUTTON_WIDTH, ROW_HEIGHT);
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

    private void renderOfferContents(GuiGraphicsExtractor graphics, OfferButton button, int offerIndex, int mouseX, int mouseY) {
        MerchantOffer offer = menu.getOffers().get(offerIndex);
        boolean favorite = TradingDeskPreferences.isFavorite(offerKey(offer));
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int itemY = y + 2;

        if (offerIndex == selectedOffer) {
            graphics.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
            graphics.fill(x, y + ROW_HEIGHT - 1, x + width, y + ROW_HEIGHT, 0xFFFFFFFF);
            graphics.fill(x, y, x + 1, y + ROW_HEIGHT, 0xFFFFFFFF);
            graphics.fill(x + width - 1, y, x + width, y + ROW_HEIGHT, 0xFFFFFFFF);
        }

        renderItem(graphics, offer.getCostA(), x + 5, itemY);
        if (isHoveringItem(mouseX, mouseY, x + 5, itemY)) {
            graphics.setTooltipForNextFrame(font, offer.getCostA(), mouseX, mouseY);
            return;
        }

        if (!offer.getCostB().isEmpty()) {
            renderItem(graphics, offer.getCostB(), x + 35, itemY);
            if (isHoveringItem(mouseX, mouseY, x + 35, itemY)) {
                graphics.setTooltipForNextFrame(font, offer.getCostB(), mouseX, mouseY);
                return;
            }
        }

        Identifier arrowSprite = offer.isOutOfStock() ? TRADE_ARROW_OUT_OF_STOCK_SPRITE : TRADE_ARROW_SPRITE;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, arrowSprite, x + 54, y + 3, 10, 9);

        renderItem(graphics, offer.getResult(), x + 68, itemY);
        if (isHoveringItem(mouseX, mouseY, x + 68, itemY)) {
            graphics.setTooltipForNextFrame(font, offer.getResult(), mouseX, mouseY);
            return;
        }

        int nameWidth = width - 90 - 22;
        String name = font.plainSubstrByWidth(offer.getResult().getHoverName().getString(), Math.max(0, nameWidth));
        int nameColor = offer.isOutOfStock() ? OUT_OF_STOCK_COLOR : BUTTON_TEXT_COLOR;
        graphics.text(font, name, x + 90, y + 6, nameColor, false);

        Identifier star = favorite ? FAVORITE_STAR : FAVORITE_STAR_EMPTY;
        graphics.blit(RenderPipelines.GUI_TEXTURED, star, x + width - 20, y + 2, 0.0F, 0.0F, 16, 16, 16, 16);

        if (button.isHoveredOrFocused()) {
            Component tooltip = Component.translatable("screen.villagertradingoverhanful.offer_tooltip").copy()
                    .append(Component.literal("\n"))
                    .append(offer.isOutOfStock()
                            ? Component.translatable("screen.villagertradingoverhanful.sold_out")
                            : Component.translatable("screen.villagertradingoverhanful.stock_left", offer.getMaxUses() - offer.getUses()));
            graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    private void renderResultPreview(GuiGraphicsExtractor graphics) {
        if (selectedOffer < 0 || selectedOffer >= menu.getOffers().size() || !menu.slots.get(2).getItem().isEmpty()) {
            return;
        }

        Slot resultSlot = menu.slots.get(2);
        renderItem(graphics, menu.getOffers().get(selectedOffer).getResult(), leftPos + resultSlot.x, topPos + resultSlot.y);
    }

    private void renderItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        graphics.fakeItem(stack, x, y);
        graphics.itemDecorations(font, stack, x, y);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics) {
        int trackX = leftPos + SCROLLBAR_X;
        int trackY = topPos + HEADER_HEIGHT;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_BACKGROUND_SPRITE, trackX, trackY, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);

        int maximumScroll = Math.max(0, visibleOffers.size() - MAX_OFFER_BUTTONS);
        if (maximumScroll <= 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_DISABLED_SPRITE, trackX, trackY, SCROLLBAR_WIDTH, 27);
            return;
        }

        int thumbY = trackY + scrollRow * (SCROLLBAR_HEIGHT - 27) / maximumScroll;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, trackX, thumbY, SCROLLBAR_WIDTH, 27);
    }

    private void toggleFavorite(int offerIndex) {
        TradingDeskPreferences.toggleFavorite(offerKey(menu.getOffers().get(offerIndex)));
        refreshOffers();
    }

    private static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int cap = 5;
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, x, y, 0.0F, 0.0F, cap, cap, 20, 20);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, x + width - cap, y, 15.0F, 0.0F, cap, cap, 20, 20);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, x, y + height - cap, 0.0F, 15.0F, cap, cap, 20, 20);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, x + width - cap, y + height - cap, 15.0F, 15.0F, cap, cap, 20, 20);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, x + cap, y, 5.0F, 0.0F, width - cap * 2, cap, 20, 20);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, x + cap, y + height - cap, 5.0F, 15.0F, width - cap * 2, cap, 20, 20);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, x, y + cap, 0.0F, 5.0F, cap, height - cap * 2, 20, 20);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, x + width - cap, y + cap, 15.0F, 5.0F, cap, height - cap * 2, 20, 20);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, x + cap, y + cap, 5.0F, 5.0F, width - cap * 2, height - cap * 2, 20, 20);
    }

    private static void drawListFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + 1, 0xFF000000);
        graphics.fill(x, y, x + 1, y + height, 0xFF000000);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF000000);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF000000);
    }

    private static void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, x, y, 0.0F, 0.0F, 18, 18, 18, 18);
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
            if (ItemStack.isSameItemSameComponents(stack, result) && stack.getCount() + result.getCount() <= slot.getMaxStackSize(result)) {
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
        if (ItemStack.isSameItemSameComponents(costA, costB)) {
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
            if (ItemStack.isSameItemSameComponents(stack, wanted)) {
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

    private int getListHeight() {
        return getPanelHeight();
    }

    private int getPanelHeight() {
        return PANEL_HEIGHT;
    }

    private int getMaxRows() {
        return Math.max(2, (getListHeight() - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT);
    }

    private int getRightX() {
        return leftPos + RIGHT_PANEL_X;
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/" + name + ".png");
    }

    private static final class OfferButton extends Button.Plain {
        private final TradingDeskScreen screen;
        private int offerIndex = -1;

        private OfferButton(TradingDeskScreen screen, int x, int y, int width, int height) {
            super(x, y, width, height, CommonComponents.EMPTY, button -> {
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
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            if (!visible || offerIndex < 0) {
                return;
            }
            extractDefaultSprite(graphics);
            screen.renderOfferContents(graphics, this, offerIndex, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (visible && active && offerIndex >= 0 && event.button() == InputConstants.MOUSE_BUTTON_RIGHT && isMouseOver(event.x(), event.y())) {
                screen.toggleFavorite(offerIndex);
                return true;
            }
            return super.mouseClicked(event, doubleClick);
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
