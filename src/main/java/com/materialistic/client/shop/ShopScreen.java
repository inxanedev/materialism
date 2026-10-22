package com.materialistic.client.shop;

import com.materialistic.Money;
import com.materialistic.network.BuyItemPayload;
import com.materialistic.network.PurchaseResultPayload;
import com.materialistic.shop.ShopCategory;
import com.materialistic.shop.ShopEntry;
import com.materialistic.shop.ShopSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class ShopScreen extends Screen {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "materialistic",
            "textures/gui/shop.png"
    );
    private static final int IMAGE_WIDTH = 424;
    private static final int IMAGE_HEIGHT = 222;
    private static final int GRID_X = 92;
    private static final int GRID_Y = 30;
    private static final int GRID_COLUMNS = 9;
    private static final int GRID_ROWS = 6;
    private static final int GRID_SIZE = GRID_COLUMNS * GRID_ROWS;
    private static final int DETAIL_X = 264;
    private static final int CATEGORY_WIDTH = 80;

    private final ShopSnapshot snapshot;
    private final List<Offer> offers = new ArrayList<>();
    private final List<Offer> filtered = new ArrayList<>();
    private final Map<String, Button> categoryButtons = new HashMap<>();
    private final Map<String, Button> presetButtons = new HashMap<>();

    private int leftPos;
    private int topPos;
    private int page;
    private String selectedCategory = "all";
    private String search = "";
    private String selectedId;
    private int quantity = 1;
    private boolean boxed;
    private boolean manualDelivery;
    private boolean updatingQuantity;
    private boolean buyArmed;
    private boolean purchasePending;
    private String statusText = "";
    private int statusColor = 0xFFD8D8D8;

    private EditBox searchBox;
    private EditBox quantityBox;
    private Button previousPage;
    private Button nextPage;
    private Button minusButton;
    private Button plusButton;
    private Button looseButton;
    private Button boxedButton;
    private Button buyButton;
    private Button sellButton;

    public ShopScreen(ShopSnapshot snapshot) {
        super(Component.translatable("materialistic.shop.title"));
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;

        for (ShopEntry entry : this.snapshot.entries) {
            Identifier id = Identifier.tryParse(entry.id);
            if (id == null) {
                continue;
            }

            var item = BuiltInRegistries.ITEM.getOptional(id);
            if (item.isEmpty()) {
                continue;
            }

            ItemStack stack = new ItemStack(item.get());
            if (stack.isEmpty()) {
                continue;
            }

            this.offers.add(new Offer(entry, stack, stack.getHoverName().getString()));
        }

        this.searchBox = new EditBox(
                this.font,
                this.leftPos + GRID_X,
                this.topPos + 6,
                162,
                18,
                Component.translatable("materialistic.shop.search")
        );
        this.searchBox.setHint(Component.translatable("materialistic.shop.search"));
        this.searchBox.setResponder(value -> {
            this.search = value == null ? "" : value.toLowerCase(Locale.ROOT);
            this.page = 0;
            this.buyArmed = false;
            this.applyFilter();
        });
        this.addRenderableWidget(this.searchBox);

        int categoryY = this.topPos + 30;
        for (ShopCategory category : this.snapshot.categories) {
            Button button = Button.builder(Component.translatable(category.title), pressed -> {
                this.selectedCategory = category.id;
                this.page = 0;
                this.buyArmed = false;
                this.applyFilter();
            }).bounds(this.leftPos + 6, categoryY, CATEGORY_WIDTH, 16).build();
            categoryY += 17;
            this.categoryButtons.put(category.id, button);
            this.addRenderableWidget(button);
        }

        this.previousPage = Button.builder(Component.literal("<"), button -> {
            if (this.page > 0) {
                this.page--;
                this.buyArmed = false;
                this.updateControls();
            }
        }).bounds(this.leftPos + GRID_X + 26, this.topPos + 146, 18, 18).build();
        this.nextPage = Button.builder(Component.literal(">"), button -> {
            if (this.page + 1 < this.pageCount()) {
                this.page++;
                this.buyArmed = false;
                this.updateControls();
            }
        }).bounds(this.leftPos + GRID_X + 118, this.topPos + 146, 18, 18).build();
        this.addRenderableWidget(this.previousPage);
        this.addRenderableWidget(this.nextPage);

        this.quantityBox = new EditBox(
                this.font,
                this.leftPos + 287,
                this.topPos + 94,
                58,
                18,
                Component.translatable("materialistic.shop.quantity")
        );
        this.quantityBox.setMaxLength(6);
        this.quantityBox.setValue("1");
        this.quantityBox.setResponder(value -> this.onQuantityTyped(value));
        this.addRenderableWidget(this.quantityBox);

        this.minusButton = Button.builder(Component.literal("-"), button -> this.setQuantity(this.quantity - 1, null))
                .bounds(this.leftPos + 349, this.topPos + 94, 20, 18)
                .build();
        this.plusButton = Button.builder(Component.literal("+"), button -> this.setQuantity(this.quantity + 1, null))
                .bounds(this.leftPos + 371, this.topPos + 94, 20, 18)
                .build();
        this.addRenderableWidget(this.minusButton);
        this.addRenderableWidget(this.plusButton);

        this.presetButtons.put("one", Button.builder(Component.literal("1"), button -> this.setQuantity(1, false))
                .bounds(this.leftPos + 272, this.topPos + 118, 38, 16).build());
        this.presetButtons.put("stack", Button.builder(Component.translatable("materialistic.shop.stack"), button -> {
            Offer offer = this.selectedOffer();
            if (offer != null) {
                this.setQuantity(offer.stack.getMaxStackSize(), false);
            }
        }).bounds(this.leftPos + 314, this.topPos + 118, 40, 16).build());
        this.presetButtons.put("shulker", Button.builder(Component.translatable("materialistic.shop.shulker"), button -> {
            Offer offer = this.selectedOffer();
            if (offer != null) {
                this.setQuantity(capacity(offer.stack), true);
            }
        }).bounds(this.leftPos + 358, this.topPos + 118, 48, 16).build());
        for (Button button : this.presetButtons.values()) {
            this.addRenderableWidget(button);
        }

        this.looseButton = Button.builder(Component.translatable("materialistic.shop.loose"), button -> this.setBoxed(false))
                .bounds(this.leftPos + 267, this.topPos + 140, 70, 16)
                .build();
        this.boxedButton = Button.builder(Component.translatable("materialistic.shop.boxed"), button -> this.setBoxed(true))
                .bounds(this.leftPos + 341, this.topPos + 140, 70, 16)
                .build();
        this.addRenderableWidget(this.looseButton);
        this.addRenderableWidget(this.boxedButton);

        this.buyButton = Button.builder(Component.translatable("materialistic.shop.buy"), button -> this.onBuy())
                .bounds(this.leftPos + 265, this.topPos + 200, 148, 20)
                .build();
        this.addRenderableWidget(this.buyButton);

        this.sellButton = Button.builder(Component.translatable("materialistic.shop.sell_short"), button -> this.openSell())
                .bounds(this.leftPos + 50, this.topPos + 4, 36, 20)
                .build();
        this.addRenderableWidget(this.sellButton);

        this.applyFilter();
    }

    private void applyFilter() {
        this.filtered.clear();
        Offer selected = this.selectedOffer();
        for (Offer offer : this.offers) {
            boolean categoryMatch = this.selectedCategory.equals("all")
                    || this.selectedCategory.equals(offer.entry.category);
            if (!categoryMatch) {
                continue;
            }

            if (!this.search.isEmpty() && !offer.name.toLowerCase(Locale.ROOT).contains(this.search)
                    && !offer.entry.id.toLowerCase(Locale.ROOT).contains(this.search)) {
                continue;
            }

            this.filtered.add(offer);
        }

        int maxPage = Math.max(0, this.pageCount() - 1);
        if (this.page > maxPage) {
            this.page = maxPage;
        }

        if (selected == null || !this.filtered.contains(selected)) {
            this.selectedId = this.filtered.isEmpty() ? null : this.filtered.getFirst().entry.id;
            this.quantity = 1;
            this.boxed = false;
            this.manualDelivery = false;
            this.buyArmed = false;
        }

        this.updateControls();
    }

    private void updateControls() {
        for (Map.Entry<String, Button> entry : this.categoryButtons.entrySet()) {
            ShopCategory category = this.categoryById(entry.getKey());
            if (category == null) {
                continue;
            }

            Component title = Component.translatable(category.title);
            entry.getValue().setMessage(entry.getKey().equals(this.selectedCategory)
                    ? Component.literal("» ").append(title)
                    : title);
        }

        Offer offer = this.selectedOffer();
        boolean hasItem = offer != null;
        boolean packable = hasItem && isPackable(offer.stack);
        int stackSize = hasItem ? Math.max(1, offer.stack.getMaxStackSize()) : 1;
        boolean showDelivery = hasItem && packable && this.quantity > stackSize;

        this.quantityBox.setVisible(hasItem);
        this.minusButton.visible = hasItem;
        this.plusButton.visible = hasItem;
        for (Button button : this.presetButtons.values()) {
            button.visible = hasItem;
            button.active = hasItem;
        }

        Button shulkerButton = this.presetButtons.get("shulker");
        if (shulkerButton != null) {
            shulkerButton.active = packable;
        }

        this.looseButton.visible = showDelivery;
        this.boxedButton.visible = showDelivery;
        this.looseButton.active = showDelivery && !this.boxed;
        this.boxedButton.active = showDelivery && this.boxed;

        this.previousPage.active = this.page > 0;
        this.nextPage.active = this.page + 1 < this.pageCount();

        long total = hasItem ? this.total(offer) : 0L;
        boolean affordable = hasItem && total <= this.snapshot.balance;
        this.buyButton.visible = hasItem;
        this.buyButton.active = hasItem && affordable && !this.purchasePending;
        this.buyButton.setMessage(Component.translatable(
                this.buyArmed ? "materialistic.shop.buy_anyway" : "materialistic.shop.buy"
        ));

        this.updateQuantityValue();
    }

    private void updateQuantityValue() {
        if (this.quantityBox == null || this.quantityBox.isFocused()) {
            return;
        }

        String value = Integer.toString(this.quantity);
        if (!value.equals(this.quantityBox.getValue())) {
            this.updatingQuantity = true;
            this.quantityBox.setValue(value);
            this.updatingQuantity = false;
        }
    }

    private void onQuantityTyped(String value) {
        if (this.updatingQuantity) {
            return;
        }

        if (value == null || value.isBlank()) {
            return;
        }

        try {
            int typed = Integer.parseInt(value);
            int max = this.maxQuantity();
            int clamped = Math.max(1, Math.min(max, typed));
            this.quantity = clamped;
            if (clamped != typed) {
                this.updatingQuantity = true;
                this.quantityBox.setValue(Integer.toString(clamped));
                this.updatingQuantity = false;
            }
            this.applyAutomaticDelivery();
            this.buyArmed = false;
            this.updateControls();
        } catch (NumberFormatException ignored) {
        }
    }

    private void setQuantity(int value, Boolean forceBoxed) {
        Offer offer = this.selectedOffer();
        if (offer == null) {
            return;
        }

        int max = this.maxQuantity();
        this.quantity = Math.max(1, Math.min(max, value));
        if (forceBoxed != null) {
            this.boxed = forceBoxed && isPackable(offer.stack);
            this.manualDelivery = true;
        } else {
            this.manualDelivery = false;
            this.applyAutomaticDelivery();
        }

        this.buyArmed = false;
        this.updateControls();
    }

    private void applyAutomaticDelivery() {
        Offer offer = this.selectedOffer();
        if (offer == null || !isPackable(offer.stack)) {
            this.boxed = false;
            return;
        }

        int stackSize = Math.max(1, offer.stack.getMaxStackSize());
        if (this.quantity <= stackSize) {
            this.boxed = false;
        } else if (!this.manualDelivery) {
            this.boxed = this.quantity > 9L * stackSize;
        }
    }

    private void setBoxed(boolean value) {
        Offer offer = this.selectedOffer();
        if (offer == null || !isPackable(offer.stack)) {
            return;
        }

        this.boxed = value;
        this.manualDelivery = true;
        this.buyArmed = false;
        this.updateControls();
    }

    private void onBuy() {
        Offer offer = this.selectedOffer();
        if (offer == null || this.purchasePending) {
            return;
        }

        long total = this.total(offer);
        if (total > this.snapshot.balance) {
            return;
        }

        int dropped = this.estimatedDropped(offer);
        if (dropped > 0 && !this.buyArmed) {
            this.buyArmed = true;
            this.updateControls();
            return;
        }

        this.purchasePending = true;
        this.buyArmed = false;
        this.updateControls();
        ClientPlayNetworking.send(new BuyItemPayload(
                Identifier.parse(offer.entry.id),
                this.quantity,
                this.boxed
        ));
    }

    public void handlePurchaseResult(PurchaseResultPayload result) {
        this.snapshot.balance = result.balance();
        this.purchasePending = false;
        this.buyArmed = false;
        if (result.success()) {
            this.statusColor = 0xFF8FE388;
            this.statusText = Component.translatable("materialistic.shop.complete").getString();
        } else {
            this.statusColor = 0xFFFF7777;
            this.statusText = Component.translatable("materialistic.shop.failed").getString();
        }
        this.updateControls();
    }

    private void openSell() {
        if (this.minecraft.getConnection() == null) {
            return;
        }

        this.minecraft.gui.setScreen(null);
        this.minecraft.getConnection().sendCommand("sell");
    }

    private Offer selectedOffer() {
        if (this.selectedId == null) {
            return null;
        }

        for (Offer offer : this.filtered) {
            if (offer.entry.id.equals(this.selectedId)) {
                return offer;
            }
        }

        return null;
    }

    private ShopCategory categoryById(String id) {
        for (ShopCategory category : this.snapshot.categories) {
            if (category.id.equals(id)) {
                return category;
            }
        }
        return null;
    }

    private int pageCount() {
        return Math.max(1, (this.filtered.size() + GRID_SIZE - 1) / GRID_SIZE);
    }

    private int capacity(ItemStack stack) {
        return 27 * Math.max(1, stack.getMaxStackSize());
    }

    private int maxQuantity() {
        Offer offer = this.selectedOffer();
        if (offer == null) {
            return 1;
        }

        if (!isPackable(offer.stack)) {
            return 27;
        }

        return Math.max(1, this.snapshot.maxShulkerBoxes) * this.capacity(offer.stack);
    }

    private long total(Offer offer) {
        long itemCost = offer.entry.price * (long) this.quantity;
        int boxes = this.boxed ? (int) Math.ceil(this.quantity / (double) this.capacity(offer.stack)) : 0;
        return itemCost + boxes * this.snapshot.shulkerBoxPrice;
    }

    private int boxCount(Offer offer) {
        return this.boxed ? (int) Math.ceil(this.quantity / (double) this.capacity(offer.stack)) : 0;
    }

    private int estimatedDropped(Offer offer) {
        if (this.boxed) {
            int free = 0;
            for (int slot = 0; slot < this.minecraft.player.getInventory().getContainerSize(); slot++) {
                if (this.minecraft.player.getInventory().getItem(slot).isEmpty()) {
                    free++;
                }
            }
            return Math.max(0, this.boxCount(offer) - free);
        }

        int stackSize = Math.max(1, offer.stack.getMaxStackSize());
        long room = 0L;
        for (int slot = 0; slot < this.minecraft.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = this.minecraft.player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                room += stackSize;
            } else if (ItemStack.isSameItemSameComponents(stack, offer.stack)) {
                room += Math.max(0, stackSize - stack.getCount());
            }
        }

        return (int) Math.max(0L, this.quantity - room);
    }

    private static boolean isPackable(ItemStack stack) {
        Item item = stack.getItem();
        return !(item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                IMAGE_WIDTH,
                IMAGE_HEIGHT,
                512,
                256
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(this.font, this.title, this.leftPos + 8, this.topPos + 10, 0xFFFFD166, true);
        Component balance = Component.translatable("materialistic.shop.balance", Money.currency(this.snapshot.balance));
        int balanceWidth = this.font.width(balance);
        int balanceGroupWidth = 20 + balanceWidth;
        int balanceX = this.leftPos + 258 + Math.max(0, (162 - balanceGroupWidth) / 2);
        graphics.item(new ItemStack(Items.GOLD_INGOT), balanceX, this.topPos + 6);
        graphics.text(
                this.font,
                balance,
                balanceX + 20,
                this.topPos + 10,
                0xFFFFE08A,
                true
        );

        this.drawGrid(graphics, mouseX, mouseY);
        this.drawPageText(graphics);
        this.drawDetail(graphics);
    }

    private void drawGrid(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int start = this.page * GRID_SIZE;
        int end = Math.min(start + GRID_SIZE, this.filtered.size());
        int hovered = this.gridIndexAt(mouseX, mouseY);

        for (int index = start; index < end; index++) {
            int local = index - start;
            int column = local % GRID_COLUMNS;
            int row = local / GRID_COLUMNS;
            int x = this.leftPos + GRID_X + column * 18;
            int y = this.topPos + GRID_Y + row * 18;
            Offer offer = this.filtered.get(index);

            if (offer.entry.id.equals(this.selectedId)) {
                graphics.outline(x - 1, y - 1, 20, 20, 0xFFFFD166);
            } else if (hovered == index) {
                graphics.fill(x - 1, y - 1, x + 17, y + 17, 0x30FFFFFF);
            }

            graphics.item(offer.stack, x, y);
        }

        if (this.filtered.isEmpty()) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("materialistic.shop.no_items"),
                    this.leftPos + GRID_X + 81,
                    this.topPos + 82,
                    0xFFAAAAAA
            );
        } else if (hovered >= 0 && hovered < this.filtered.size()) {
            Offer offer = this.filtered.get(hovered);
            graphics.setTooltipForNextFrame(this.font, offer.stack, mouseX, mouseY);
        }
    }

    private void drawPageText(GuiGraphicsExtractor graphics) {
        graphics.centeredText(
                this.font,
                Component.translatable(
                        "materialistic.shop.page",
                        this.page + 1,
                        this.pageCount()
                ),
                this.leftPos + GRID_X + 81,
                this.topPos + 152,
                0xFFB8C7D9
        );
    }

    private void drawDetail(GuiGraphicsExtractor graphics) {
        Offer offer = this.selectedOffer();
        if (offer == null) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("materialistic.shop.select"),
                    this.leftPos + DETAIL_X + 74,
                    this.topPos + 62,
                    0xFFAAAAAA
            );
            return;
        }

        int x = this.leftPos + DETAIL_X;
        graphics.item(offer.stack, x + 2, this.topPos + 32);
        List<FormattedCharSequence> nameLines = this.font.split(offer.stack.getHoverName(), 90);
        for (int i = 0; i < Math.min(3, nameLines.size()); i++) {
            graphics.text(this.font, nameLines.get(i), x + 40, this.topPos + 34 + i * 10, 0xFFFFFFFF);
        }

        int center = this.leftPos + 339;
        graphics.centeredText(
                this.font,
                Component.translatable("materialistic.shop.price", Money.currency(offer.entry.price)),
                center,
                this.topPos + 70,
                0xFFFFE08A
        );
        graphics.centeredText(
                this.font,
                Component.translatable("materialistic.shop.quantity"),
                center,
                this.topPos + 84,
                0xFFB8C7D9
        );

        int boxes = this.boxCount(offer);
        if (this.boxed) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("materialistic.shop.boxes", boxes, this.snapshot.maxShulkerBoxes),
                    center,
                    this.topPos + 158,
                    0xFFB8C7D9
            );
            graphics.centeredText(
                    this.font,
                    Component.translatable(
                            "materialistic.shop.box_cost",
                            Money.currency(this.snapshot.shulkerBoxPrice * boxes)
                    ),
                    center,
                    this.topPos + 170,
                    0xFFB8C7D9
            );
        }

        graphics.centeredText(
                this.font,
                Component.translatable("materialistic.shop.total", Money.currency(this.total(offer))),
                center,
                this.topPos + 180,
                0xFFFFD166
        );

        int dropped = this.estimatedDropped(offer);
        if (dropped > 0) {
            Component warning = Component.translatable(
                    this.boxed ? "materialistic.shop.drop_boxes" : "materialistic.shop.drop_items",
                    dropped
            );
            graphics.centeredText(this.font, warning, center, this.topPos + 190, 0xFFFF7777);
        } else if (!this.statusText.isEmpty()) {
            graphics.centeredText(this.font, this.statusText, center, this.topPos + 190, this.statusColor);
        }

        if (this.buyArmed) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("materialistic.shop.confirm_drop"),
                    center,
                    this.topPos + 194,
                    0xFFFF7777
            );
        }

        if (this.total(offer) > this.snapshot.balance) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("materialistic.shop.no_money"),
                    center,
                    this.topPos + 194,
                    0xFFFF7777
            );
        }
    }

    private int gridIndexAt(double mouseX, double mouseY) {
        int localX = (int) (mouseX - (this.leftPos + GRID_X));
        int localY = (int) (mouseY - (this.topPos + GRID_Y));
        if (localX < 0 || localY < 0) {
            return -1;
        }

        int column = localX / 18;
        int row = localY / 18;
        if (column >= GRID_COLUMNS || row >= GRID_ROWS) {
            return -1;
        }

        return this.page * GRID_SIZE + row * GRID_COLUMNS + column;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        int index = this.gridIndexAt(event.x(), event.y());
        if (index >= 0 && index < this.filtered.size()) {
            Offer offer = this.filtered.get(index);
            if (!offer.entry.id.equals(this.selectedId)) {
                this.selectedId = offer.entry.id;
                this.quantity = 1;
                this.boxed = false;
                this.manualDelivery = false;
                this.buyArmed = false;
                this.statusText = "";
                this.updateControls();
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D && this.gridIndexAt(mouseX, mouseY) >= 0) {
            if (scrollY > 0.0D && this.page > 0) {
                this.page--;
            } else if (scrollY < 0.0D && this.page + 1 < this.pageCount()) {
                this.page++;
            }
            this.buyArmed = false;
            this.updateControls();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private record Offer(ShopEntry entry, ItemStack stack, String name) {
    }
}
