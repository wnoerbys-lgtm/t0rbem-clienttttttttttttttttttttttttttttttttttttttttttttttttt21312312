package dev.codex.paytracker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class PayTrackerScreen extends Screen {
    private static final int SLOT_SIZE = 20;
    private EditBox minimumInput;
    private Button auctionButton;
    private ItemStack selectedItem = ItemStack.EMPTY;
    private int selectedSlot = -1;
    private String status = "";

    public PayTrackerScreen() { super(Component.literal("Donut Auction")); }

    @Override
    protected void init() {
        int center = width / 2;
        minimumInput = new EditBox(font, center - 100, 52, 100, 20, Component.literal("Minimum bid"));
        minimumInput.setValue(PayTrackerStore.getMinimum().stripTrailingZeros().toPlainString());
        minimumInput.setHint(Component.literal("Min bid"));
        addRenderableWidget(minimumInput);
        addRenderableWidget(Button.builder(Component.literal("Save min"), button -> saveMinimum())
                .bounds(center + 5, 52, 95, 20).build());
        auctionButton = addRenderableWidget(Button.builder(Component.literal(PayTrackerStore.isAuctionActive() ? "End auction" : "Start auction"), button -> toggleAuction())
                .bounds(center - 80, 80, 160, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(center - 40, height - 30, 80, 20).build());
    }

    private void saveMinimum() {
        try {
            BigDecimal value = readMinimum();
            PayTrackerStore.setMinimum(value);
            status = "Minimum bid saved";
        } catch (NumberFormatException exception) {
            status = "Enter a valid minimum bid";
        }
    }

    private BigDecimal readMinimum() {
        BigDecimal value = new BigDecimal(minimumInput.getValue().replace(",", "").trim());
        if (value.signum() < 0) throw new NumberFormatException();
        return value;
    }

    private void toggleAuction() {
        if (PayTrackerStore.isAuctionActive()) {
            PayTrackerStore.endAuction();
            status = "Auction ended";
            auctionButton.setMessage(Component.literal("Start auction"));
            return;
        }
        if (selectedItem.isEmpty()) {
            status = "Select an item from your inventory first";
            return;
        }
        try {
            PayTrackerStore.setMinimum(readMinimum());
            PayTrackerStore.startAuction(selectedItem);
            status = "Auction started";
            auctionButton.setMessage(Component.literal("End auction"));
        } catch (NumberFormatException exception) {
            status = "Enter a valid minimum bid";
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Avoid Minecraft 1.21.11's per-frame blur collision with other screen layers.
        graphics.fill(0, 0, width, height, 0xC0101010);
        int center = width / 2;
        graphics.drawCenteredString(font, title, center, 16, 0xFFFFFF);

        if (PayTrackerStore.isAuctionActive()) {
            ItemStack item = PayTrackerStore.getAuctionItem();
            graphics.renderItem(item, center - 138, 50);
            graphics.drawString(font, "Auctioning: " + item.getHoverName().getString(), center - 114, 54, 0xFFFFFF);
            graphics.drawString(font, "Minimum bid: $" + money(PayTrackerStore.getMinimum()), center - 114, 70, 0xAAAAAA);
            if (PayTrackerStore.getLeadingPlayer().isEmpty()) {
                graphics.drawString(font, "Highest bid: none yet", center - 138, 112, 0xFFFF55);
            } else {
                graphics.drawString(font, "Highest: " + PayTrackerStore.getLeadingPlayer() + "  $" + money(PayTrackerStore.getLeadingAmount()), center - 138, 112, 0x55FF55);
            }
            graphics.drawString(font, "Payment feed", center - 138, 132, 0xFFFFFF);
            PayTrackerStore.Payment[] payments = PayTrackerStore.getPayments();
            int rows = Math.max(0, Math.min(18, (height - 180) / 14));
            for (int i = 0; i < Math.min(rows, payments.length); i++) {
                PayTrackerStore.Payment payment = payments[i];
                int color = payment.amount().compareTo(PayTrackerStore.getMinimum()) >= 0 ? 0xFFFFFF : 0x888888;
                graphics.drawString(font, payment.player(), center - 138, 150 + i * 14, color);
                graphics.drawString(font, "$" + money(payment.amount()), center + 82, 150 + i * 14, color);
            }
            if (payments.length == 0) graphics.drawString(font, "Waiting for payments...", center - 138, 152, 0x888888);
        } else {
            graphics.drawCenteredString(font, "Choose the auction item from your inventory", center, 118, 0xFFFFFF);
            drawInventoryPicker(graphics, center);
            if (selectedItem.isEmpty()) {
                graphics.drawCenteredString(font, "No item selected", center, 242, 0x888888);
            } else {
                graphics.renderItem(selectedItem, center - 50, 260);
                graphics.drawString(font, "Selected: " + selectedItem.getHoverName().getString(), center - 28, 264, 0x55FF55);
            }
            graphics.drawCenteredString(font, "Minimum bid: $" + money(PayTrackerStore.getMinimum()), center, 290, 0xAAAAAA);
        }

        if (!status.isEmpty()) graphics.drawCenteredString(font, status, center, height - 48, 0xFFFF55);
        super.render(graphics, mouseX, mouseY, delta);
    }

    private void drawInventoryPicker(GuiGraphics graphics, int center) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        int left = center - 90;
        int top = 144;
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 9; column++) {
                int x = left + column * SLOT_SIZE;
                int y = top + row * SLOT_SIZE;
                graphics.fill(x, y, x + 18, y + 18, 0xFF555555);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF222222);
                int inventorySlot = row == 3 ? column : 9 + row * 9 + column;
                ItemStack stack = player.getInventory().getItem(inventorySlot);
                if (!stack.isEmpty()) graphics.renderItem(stack, x + 1, y + 1);
                if (inventorySlot == selectedSlot) {
                    graphics.fill(x, y, x + 18, y + 1, 0xFF55FF55);
                    graphics.fill(x, y + 17, x + 18, y + 18, 0xFF55FF55);
                    graphics.fill(x, y, x + 1, y + 18, 0xFF55FF55);
                    graphics.fill(x + 17, y, x + 18, y + 18, 0xFF55FF55);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (!PayTrackerStore.isAuctionActive() && button == 0) {
            int center = width / 2;
            int left = center - 90;
            int top = 144;
            int column = ((int) mouseX - left) / SLOT_SIZE;
            int row = ((int) mouseY - top) / SLOT_SIZE;
            if (column >= 0 && column < 9 && row >= 0 && row < 4
                    && mouseX >= left && mouseY >= top
                    && mouseX < left + 9 * SLOT_SIZE && mouseY < top + 4 * SLOT_SIZE) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    int inventorySlot = row == 3 ? column : 9 + row * 9 + column;
                    ItemStack stack = player.getInventory().getItem(inventorySlot);
                    if (!stack.isEmpty()) {
                        selectedItem = stack.copy();
                        selectedSlot = inventorySlot;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static String money(BigDecimal amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount.stripTrailingZeros());
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
