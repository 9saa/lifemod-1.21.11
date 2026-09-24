package com.example.lifemod.client.screen;

import com.example.lifemod.network.ModNetworking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ReviveScreen extends Screen {

    // --- Layout ---
    private static final int PANEL_WIDTH  = 260;
    private static final int PANEL_HEIGHT = 240;
    private static final int OUTER_MARGIN = 16;
    private static final int ROW_HEIGHT   = 16;

    // --- Colors (ARGB) ---
    private static final int COLOR_SCRIM       = 0xC0000000;
    private static final int COLOR_PANEL_BG    = 0xFF202020;
    private static final int COLOR_PANEL_BORD  = 0xFF555555;
    private static final int COLOR_LIST_BG     = 0xFF0A0A0A;
    private static final int COLOR_ROW_HOVER   = 0x40FFFFFF;
    private static final int COLOR_TEXT        = 0xFFFFFFFF;  // <-- added FF
    private static final int COLOR_TEXT_DIM    = 0xFFAAAAAA;  // <-- added FF
    private static final int COLOR_TITLE       = 0xFFFFD700;  // <-- added FF
    private static final int COLOR_WARN        = 0xFFFF5555;  // <-- added FF

    private final BlockPos beaconPos;
    private final List<ModNetworking.PlayerEntry> allPlayers;
    private final List<ModNetworking.PlayerEntry> filtered;

    private ModNetworking.PlayerEntry confirmTarget;

    private TextFieldWidget searchField;
    private ButtonWidget confirmButton;
    private ButtonWidget cancelConfirmButton;
    private ButtonWidget closeButton;

    private int panelX, panelY, panelW, panelH;
    private int listX, listY, listW, listH;

    public ReviveScreen(BlockPos beaconPos, List<ModNetworking.PlayerEntry> players) {
        super(Text.literal("Revive Beacon"));
        this.beaconPos = beaconPos;
        this.allPlayers = new ArrayList<>(players);
        this.filtered = new ArrayList<>(players);
    }

    @Override
    protected void init() {
        panelW = Math.min(PANEL_WIDTH, this.width - 40);
        panelH = Math.min(PANEL_HEIGHT, this.height - 40);
        panelX = (this.width  - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int innerX = panelX + OUTER_MARGIN;
        int innerW = panelW - OUTER_MARGIN * 2;

        // Search field
        searchField = new TextFieldWidget(
                this.textRenderer,
                innerX, panelY + 40, innerW, 20,
                Text.literal("Search"));
        searchField.setMaxLength(32);
        searchField.setChangedListener(this::onSearch);
        addSelectableChild(searchField);

        // List geometry
        listX = innerX;
        listY = panelY + 40 + 24 + 6;
        listW = innerW;
        listH = panelH - (listY - panelY) - 34;

        // Buttons
        int by = panelY + panelH - 28;
        closeButton = ButtonWidget.builder(Text.literal("Close"), b -> this.close())
                .dimensions(panelX + (panelW - 80) / 2, by, 80, 20).build();

        confirmButton = ButtonWidget.builder(Text.literal("Confirm"), b -> doRevive())
                .dimensions(panelX + 20, by, 90, 20).build();

        cancelConfirmButton = ButtonWidget.builder(Text.literal("Cancel"), b -> {
            confirmTarget = null;
            updateVisibility();
        }).dimensions(panelX + panelW - 110, by, 90, 20).build();

        addDrawableChild(closeButton);
        addDrawableChild(confirmButton);
        addDrawableChild(cancelConfirmButton);

        updateVisibility();
        setInitialFocus(searchField);
    }

    private void updateVisibility() {
        boolean c = confirmTarget != null;
        closeButton.visible         = !c;
        confirmButton.visible       =  c;
        cancelConfirmButton.visible =  c;
        searchField.setVisible(!c);
    }

    private void onSearch(String query) {
        filtered.clear();
        String q = query.toLowerCase().trim();
        for (ModNetworking.PlayerEntry p : allPlayers) {
            if (q.isEmpty() || p.name().toLowerCase().contains(q)) {
                filtered.add(p);
            }
        }
    }

    private void doRevive() {
        if (confirmTarget == null) return;
        ClientPlayNetworking.send(new ModNetworking.ReviveRequestPayload(
                confirmTarget.uuid(), beaconPos));
        this.close();
    }

    /** Escape while confirming cancels the confirm; otherwise it closes the screen. */
    @Override
    public void close() {
        if (confirmTarget != null) {
            confirmTarget = null;
            updateVisibility();
            return;
        }
        super.close();
    }

    // Disable the vanilla background (blur + darken) so our own scrim shows cleanly.
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // intentionally empty
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // 1. Scrim
        ctx.fill(0, 0, this.width, this.height, COLOR_SCRIM);

        // 2. Panel + border
        ctx.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, COLOR_PANEL_BORD);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL_BG);

        // 3. Title + divider
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Revive Beacon"), this.width / 2, panelY + 12, COLOR_TITLE);
        ctx.fill(panelX + 10, panelY + 30, panelX + panelW - 10, panelY + 31, COLOR_PANEL_BORD);

        if (confirmTarget != null) {
            drawConfirmation(ctx);
            // Still need to render the buttons
            super.render(ctx, mouseX, mouseY, delta);
            return;
        }

        // 4. List panel
        ctx.fill(listX - 1, listY - 1, listX + listW + 1, listY + listH + 1, COLOR_PANEL_BORD);
        ctx.fill(listX, listY, listX + listW, listY + listH, COLOR_LIST_BG);

        // 5. Widgets (search box + close button)
        super.render(ctx, mouseX, mouseY, delta);

        // 6. Rows / empty-state text ON TOP of everything
        if (filtered.isEmpty()) {
            String msg = allPlayers.isEmpty()
                    ? "No eliminated players"
                    : "No matches for search";
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(msg), listX + listW / 2,
                    listY + listH / 2 - 4, COLOR_TEXT_DIM);
            return;
        }

        int visibleRows = Math.max(1, listH / ROW_HEIGHT);
        int shown = Math.min(filtered.size(), visibleRows);
        for (int i = 0; i < shown; i++) {
            ModNetworking.PlayerEntry entry = filtered.get(i);
            int rowY = listY + i * ROW_HEIGHT;

            boolean hover = mouseX >= listX && mouseX <= listX + listW
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hover) {
                ctx.fill(listX, rowY, listX + listW, rowY + ROW_HEIGHT, COLOR_ROW_HOVER);
            }

            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal(entry.name()), listX + 6, rowY + 4, COLOR_TEXT);

            String lives = entry.lives() + (entry.lives() == 1 ? " life" : " lives");
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal(lives),
                    listX + listW - this.textRenderer.getWidth(lives) - 6,
                    rowY + 4, COLOR_TEXT_DIM);
        }

        if (filtered.size() > visibleRows) {
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("(" + (filtered.size() - visibleRows) + " more — refine search)"),
                    listX + 6, listY + listH + 5, COLOR_TEXT_DIM);
        }
    }

    private void drawConfirmation(DrawContext ctx) {
        int cx = this.width / 2;
        int cy = panelY + panelH / 2 - 30;

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Are you sure?"), cx, cy, COLOR_TITLE);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Revive " + confirmTarget.name() + "?"),
                cx, cy + 22, COLOR_TEXT);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("The beacon will be consumed."),
                cx, cy + 44, COLOR_WARN);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (confirmTarget == null && click.button() == 0) {
            double mx = click.x();
            double my = click.y();
            if (mx >= listX && mx <= listX + listW
                    && my >= listY && my < listY + listH) {
                int idx = (int) ((my - listY) / ROW_HEIGHT);
                if (idx >= 0 && idx < filtered.size()) {
                    confirmTarget = filtered.get(idx);
                    updateVisibility();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (confirmTarget != null) {
            // Let Screen's default ESC handling call close(), which we've overridden
            // to cancel the confirmation instead of exiting.
            return super.keyPressed(input);
        }
        if (searchField.keyPressed(input)) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
