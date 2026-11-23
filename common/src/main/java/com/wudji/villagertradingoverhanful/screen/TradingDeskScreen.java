package com.wudji.villagertradingoverhanful.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.wudji.villagertradingoverhanful.mixin.SlotPositionAccessor;
import com.wudji.villagertradingoverhanful.platform.Services;
import com.wudji.villagertradingoverhanful.preferences.TradingDeskPreferences;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
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
    private static final int LIST_WIDTH = 176;
    private static final int RIGHT_PANEL_WIDTH = 176;
    private static final int ROW_HEIGHT = 27;
    private static final int HEADER_HEIGHT = 38;
    private static final int FOOTER_HEIGHT = 25;
    private static final int RIGHT_PANEL_HEIGHT = 204;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MUTED_COLOR = 0xFFC0C0C0;
    private static final int OUT_OF_STOCK_COLOR = 0xFFFF7070;
    private static final int FAVORITE_COLOR = 0xFFFFD45A;
    private static final int QUEUE_COLOR = 0xFFFFD47A;
    private static final int EXPERIENCE_BAR_WIDTH = 28;
    private static final int EXPERIENCE_BAR_HEIGHT = 5;
    private static final Identifier PANEL_SPRITE = Identifier.withDefaultNamespace("popup/background");
    private static final Identifier ROW_SPRITE = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier ROW_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/button_highlighted");
    private static final Identifier ROW_DISABLED_SPRITE = Identifier.withDefaultNamespace("widget/button_disabled");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("widget/slot_frame");
    private static final Identifier FIELD_SPRITE = Identifier.withDefaultNamespace("widget/text_field");
    private static final Identifier EXPERIENCE_BAR_CURRENT_SPRITE = Identifier.withDefaultNamespace("container/villager/experience_bar_current");
    private static final Identifier TRADE_ARROW_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow");
    private static final Identifier TRADE_ARROW_OUT_OF_STOCK_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow_out_of_stock");

    private final List<Integer> visibleOffers = new ArrayList<>();
    private OfferFilter filter = OfferFilter.ALL;
    private OfferOrder order = OfferOrder.VANILLA;
    private int selectedOffer = -1;
    private int scrollRow;
    private int queuedTrades;
    private int knownOfferCount = -1;
    private int pendingDestinationSlot = -1;
    private EditBox amountInput;
    private Button cycleTradesButton;

    public TradingDeskScreen(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        int listWidth = getListWidth();
        leftPos = Math.max(listWidth, (width + listWidth - RIGHT_PANEL_WIDTH) / 2);
        topPos = Math.max(12, (height - RIGHT_PANEL_HEIGHT) / 2);
        arrangeSlots();
        refreshOffers();

        int listX = getListX();
        int controlWidth = (getListWidth() - 16) / 3;
        addRenderableWidget(Button.builder(filter.label(), button -> {
            filter = filter.next();
            scrollRow = 0;
            refreshOffers();
            button.setMessage(filter.label());
        }).bounds(listX + 5, topPos + 18, controlWidth, 18).build());
        addRenderableWidget(Button.builder(order.label(), button -> {
            order = order.next();
            refreshOffers();
            button.setMessage(order.label());
        }).bounds(listX + 8 + controlWidth, topPos + 18, controlWidth, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.villagertradingoverhanful.options"), button -> Services.PLATFORM.openConfigScreen(this)).bounds(listX + 11 + controlWidth * 2, topPos + 18, controlWidth, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.villagertradingoverhanful.trade_once"), button -> beginTrades(1)).bounds(leftPos + 11, topPos + 67, 73, 19).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.villagertradingoverhanful.trade_stack"), button -> beginTrades(TradingDeskPreferences.getBatchLimit())).bounds(leftPos + 90, topPos + 67, 75, 19).build());

        amountInput = addRenderableWidget(new EditBox(font, leftPos + 11, topPos + 90, 38, 18, Component.translatable("screen.villagertradingoverhanful.amount")));
        amountInput.setValue("1");
        amountInput.setMaxLength(3);
        amountInput.setHint(Component.translatable("screen.villagertradingoverhanful.amount"));
        addRenderableWidget(Button.builder(Component.literal("−"), button -> changeAmount(-1)).bounds(leftPos + 53, topPos + 90, 22, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> changeAmount(1)).bounds(leftPos + 79, topPos + 90, 22, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.villagertradingoverhanful.trade_amount"), button -> beginTrades(readAmount())).bounds(leftPos + 105, topPos + 90, 60, 18).build());

        if (Services.TRADE_CYCLING.isLoaded()) {
            int footerY = topPos + HEADER_HEIGHT + getMaxRows() * ROW_HEIGHT + 2;
            int cycleWidth = Math.min(72, Math.max(48, getListWidth() - 90));
            cycleTradesButton = addRenderableWidget(Button.builder(
                    Component.translatable("screen.villagertradingoverhanful.cycle_trades", Services.TRADE_CYCLING.keyDisplay()),
                    button -> {
                        Services.TRADE_CYCLING.cycleTrades();
                        playCycleSound();
                    }).bounds(listX + getListWidth() - cycleWidth - 5, footerY + 4, cycleWidth, 16).build());
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

        int listX = getListX();
        int panelWidth = getListWidth() + RIGHT_PANEL_WIDTH;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL_SPRITE, listX, topPos, panelWidth, RIGHT_PANEL_HEIGHT);

        int experienceFill = getExperienceFill();
        if (experienceFill >= 0) {
            int experienceX = leftPos + 168 - EXPERIENCE_BAR_WIDTH;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FIELD_SPRITE, experienceX - 1, topPos + 9, EXPERIENCE_BAR_WIDTH + 2, EXPERIENCE_BAR_HEIGHT + 2);
        }

        for (Slot slot : menu.slots) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
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
        Component merchantTitle = getMerchantTitle();
        int experienceFill = getExperienceFill();
        if (experienceFill < 0) {
            graphics.text(font, merchantTitle, 88 - font.width(merchantTitle) / 2, 7, TEXT_COLOR, true);
        } else {
            int experienceX = 168 - EXPERIENCE_BAR_WIDTH;
            String visibleTitle = font.plainSubstrByWidth(merchantTitle.getString(), experienceX - 20);
            graphics.text(font, visibleTitle, 8, 7, TEXT_COLOR, true);
            if (experienceFill > 0) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_CURRENT_SPRITE, 102, 5, 0, 0, experienceX, 10, experienceFill, EXPERIENCE_BAR_HEIGHT);
            }
        }
        graphics.text(font, Component.translatable("screen.villagertradingoverhanful.offer_inputs"), 8, 20, MUTED_COLOR, false);
        graphics.text(font, Component.translatable("screen.villagertradingoverhanful.inventory"), 8, 109, MUTED_COLOR, false);
        if (queuedTrades > 0) {
            graphics.text(font, Component.translatable("screen.villagertradingoverhanful.queue", queuedTrades), 8, 56, QUEUE_COLOR, false);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractContents(graphics, mouseX, mouseY, delta);
        int listX = getListX();
        int listTop = topPos + HEADER_HEIGHT;
        int maxRows = getMaxRows();
        int end = Math.min(visibleOffers.size(), scrollRow + maxRows);
        for (int row = scrollRow; row < end; row++) {
            int y = listTop + (row - scrollRow) * ROW_HEIGHT;
            renderOfferRow(graphics, visibleOffers.get(row), listX + 5, y, mouseX, mouseY);
        }
        renderResultPreview(graphics);

        int footerY = listTop + maxRows * ROW_HEIGHT + 2;
        graphics.text(font, Component.translatable("screen.villagertradingoverhanful.offer_count", visibleOffers.isEmpty() ? 0 : scrollRow + 1, Math.min(end, visibleOffers.size()), visibleOffers.size()), listX + 7, footerY + 6, MUTED_COLOR, false);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int listTop = topPos + HEADER_HEIGHT;
        if (mouseX >= getListX() + 5 && mouseX < getListX() + getListWidth() - 5 && mouseY >= listTop && mouseY < listTop + getMaxRows() * ROW_HEIGHT) {
            int row = scrollRow + (int) ((mouseY - listTop) / ROW_HEIGHT);
            if (row < visibleOffers.size()) {
                int offerIndex = visibleOffers.get(row);
                if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
                    TradingDeskPreferences.toggleFavorite(offerKey(menu.getOffers().get(offerIndex)));
                    refreshOffers();
                } else {
                    selectOffer(offerIndex);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= getListX() && mouseX < getListX() + getListWidth() && mouseY >= topPos && mouseY < topPos + getListHeight()) {
            int maximumScroll = Math.max(0, visibleOffers.size() - getMaxRows());
            scrollRow = Math.max(0, Math.min(maximumScroll, scrollRow - (int) Math.signum(verticalAmount)));
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
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        return mouseX < getListX() || mouseX >= leftPos + RIGHT_PANEL_WIDTH || mouseY < topPos || mouseY >= topPos + RIGHT_PANEL_HEIGHT;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (cycleTradesButton != null) {
            cycleTradesButton.active = canCycleTrades();
        }
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

    private void renderOfferRow(GuiGraphicsExtractor graphics, int offerIndex, int x, int y, int mouseX, int mouseY) {
        MerchantOffer offer = menu.getOffers().get(offerIndex);
        boolean favorite = TradingDeskPreferences.isFavorite(offerKey(offer));
        boolean selected = offerIndex == selectedOffer;
        int rowWidth = getListWidth() - 10;
        int rowHeight = ROW_HEIGHT - 3;
        boolean hovered = mouseX >= x && mouseX < x + rowWidth && mouseY >= y && mouseY < y + rowHeight;
        Identifier rowSprite = offer.isOutOfStock() ? ROW_DISABLED_SPRITE : (selected || hovered ? ROW_HIGHLIGHTED_SPRITE : ROW_SPRITE);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, rowSprite, x, y, rowWidth, rowHeight);
        renderTradeStack(graphics, offer.getCostA(), x + 3, y + 3);
        if (isHoveringItem(mouseX, mouseY, x + 3, y + 3)) {
            graphics.setTooltipForNextFrame(font, offer.getCostA(), mouseX, mouseY);
            return;
        }
        if (!offer.getCostB().isEmpty()) {
            renderTradeStack(graphics, offer.getCostB(), x + 23, y + 3);
            if (isHoveringItem(mouseX, mouseY, x + 23, y + 3)) {
                graphics.setTooltipForNextFrame(font, offer.getCostB(), mouseX, mouseY);
                return;
            }
        }
        renderTradeStack(graphics, offer.getResult(), x + 47, y + 3);
        if (isHoveringItem(mouseX, mouseY, x + 47, y + 3)) {
            graphics.setTooltipForNextFrame(font, offer.getResult(), mouseX, mouseY);
            return;
        }
        int nameWidth = rowWidth - 91;
        if (nameWidth >= 8) {
            String name = font.plainSubstrByWidth(offer.getResult().getHoverName().getString(), nameWidth);
            graphics.text(font, name, x + 68, y + 4, offer.isOutOfStock() ? MUTED_COLOR : TEXT_COLOR, false);
            Component status = offer.isOutOfStock() ? Component.translatable("screen.villagertradingoverhanful.sold_out") : Component.translatable("screen.villagertradingoverhanful.stock_left", offer.getMaxUses() - offer.getUses());
            String statusText = font.plainSubstrByWidth(status.getString(), nameWidth);
            graphics.text(font, statusText, x + 68, y + 14, offer.isOutOfStock() ? OUT_OF_STOCK_COLOR : MUTED_COLOR, false);
        }
        if (rowWidth >= 100) {
            graphics.text(font, Component.literal(favorite ? "★" : "☆"), x + rowWidth - 15, y + 7, favorite ? FAVORITE_COLOR : MUTED_COLOR, false);
        }
        if (hovered) {
            graphics.setTooltipForNextFrame(Component.translatable("screen.villagertradingoverhanful.offer_tooltip"), mouseX, mouseY);
        }
    }

    private void renderResultPreview(GuiGraphicsExtractor graphics) {
        if (selectedOffer < 0 || selectedOffer >= menu.getOffers().size() || !menu.slots.get(2).getItem().isEmpty()) {
            return;
        }

        Slot resultSlot = menu.slots.get(2);
        renderTradeStack(graphics, menu.getOffers().get(selectedOffer).getResult(), leftPos + resultSlot.x, topPos + resultSlot.y);
    }

    private void renderTradeStack(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        graphics.item(stack, x, y);
        graphics.itemDecorations(font, stack, x, y);
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
        int maximumScroll = Math.max(0, visibleOffers.size() - getMaxRows());
        scrollRow = Math.min(scrollRow, maximumScroll);
    }

    private void arrangeSlots() {
        setSlotPosition(0, 17, 38);
        setSlotPosition(1, 47, 38);
        setSlotPosition(2, 142, 38);
        for (int slotIndex = 3; slotIndex < menu.slots.size(); slotIndex++) {
            int inventoryIndex = slotIndex - 3;
            int column = inventoryIndex % 9;
            int row = inventoryIndex / 9;
            int y = row < 3 ? 122 + row * 18 : 176;
            setSlotPosition(slotIndex, 8 + column * 18, y);
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
        return leftPos - getListWidth();
    }

    private int getListWidth() {
        return Math.max(80, Math.min(LIST_WIDTH, width - 188));
    }

    private int getListHeight() {
        return getPanelHeight();
    }

    private int getPanelHeight() {
        return RIGHT_PANEL_HEIGHT;
    }

    private int getMaxRows() {
        return Math.max(2, (getListHeight() - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT);
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
