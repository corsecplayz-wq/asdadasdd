package dev.keymacro;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class MacroScreen extends Screen {
    private static final int NONE = -2, OPEN_KEY = -1;
    private static final int PW = 320, PH = 52, GAP = 4;

    private final MacroRunner runner;
    private final MacroConfig cfg;
    private final ButtonWidget[] keyButtons = new ButtonWidget[MacroConfig.SLOT_COUNT];
    private ButtonWidget openKeyButton;
    private int listening = NONE;
    private int panelX, top;

    public MacroScreen(MacroRunner runner) {
        super(Text.literal("Key Macro"));
        this.runner = runner;
        this.cfg = runner.config();
    }

    @Override
    protected void init() {
        panelX = width / 2 - PW / 2;
        int total = MacroConfig.SLOT_COUNT * PH + (MacroConfig.SLOT_COUNT - 1) * GAP + 10 + 20;
        top = Math.max(26, (height - total) / 2 + 6);

        for (int i = 0; i < MacroConfig.SLOT_COUNT; i++) buildPanel(i, top + i * (PH + GAP));

        int by = top + MacroConfig.SLOT_COUNT * PH + (MacroConfig.SLOT_COUNT - 1) * GAP + 10;
        openKeyButton = addDrawableChild(ButtonWidget.builder(openKeyText(), b -> {
            listening = OPEN_KEY;
            refresh();
        }).dimensions(panelX, by, 154, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
            .dimensions(panelX + 166, by, 154, 20).build());
    }

    private void buildPanel(int i, int y) {
        MacroConfig.Slot s = cfg.slots[i];
        int x = panelX;

        addDrawableChild(ButtonWidget.builder(onOff("Enabled", s.enabled), b -> {
            s.enabled = !s.enabled;
            b.setMessage(onOff("Enabled", s.enabled));
        }).dimensions(x + 48, y + 4, 62, 20).build());

        keyButtons[i] = addDrawableChild(ButtonWidget.builder(slotKeyText(i), b -> {
            listening = i;
            refresh();
        }).dimensions(x + 114, y + 4, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(onOff("Loop", s.loop), b -> {
            s.loop = !s.loop;
            b.setMessage(onOff("Loop", s.loop));
        }).dimensions(x + 218, y + 4, 96, 20)
            .tooltip(Tooltip.of(Text.literal("Repeat the swap sequence while the key is held")))
            .build());

        TextFieldWidget hotbar = new TextFieldWidget(textRenderer, x + 6, y + 28, 100, 20, Text.literal("Hotbar slots"));
        hotbar.setMaxLength(32);
        hotbar.setTextPredicate(t -> t.matches("[0-9, ]*"));
        hotbar.setText(s.hotbar);
        hotbar.setPlaceholder(Text.literal("Slots e.g. 2,1"));
        hotbar.setTooltip(Tooltip.of(Text.literal("Hotbar slots (1-9) to swap through, in order, separated by commas")));
        hotbar.setChangedListener(t -> s.hotbar = t);
        addDrawableChild(hotbar);

        TextFieldWidget delay = new TextFieldWidget(textRenderer, x + 110, y + 28, 80, 20, Text.literal("Delay"));
        delay.setMaxLength(5);
        delay.setTextPredicate(t -> t.matches("\\d*"));
        delay.setText(Integer.toString(s.delayMs));
        delay.setPlaceholder(Text.literal("Delay (ms)"));
        delay.setTooltip(Tooltip.of(Text.literal("Time between swaps in milliseconds.\n50 ms = 1 game tick = fastest possible.")));
        delay.setChangedListener(t -> {
            try { s.delayMs = t.isEmpty() ? 0 : Integer.parseInt(t); } catch (NumberFormatException ignored) {}
        });
        addDrawableChild(delay);

        addDrawableChild(ButtonWidget.builder(onOff("Use item", s.useItem), b -> {
            s.useItem = !s.useItem;
            b.setMessage(onOff("Use item", s.useItem));
        }).dimensions(x + 194, y + 28, 120, 20)
            .tooltip(Tooltip.of(Text.literal("Right-click after each swap")))
            .build());
    }

    // ----- text helpers -----

    private static Text onOff(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "ON" : "OFF"));
    }

    private Text slotKeyText(int i) {
        if (listening == i) return Text.literal("> Press a key <").formatted(Formatting.YELLOW);
        InputUtil.Key k = MacroRunner.keyOrNull(cfg.slots[i].trigger);
        return Text.literal("Key: " + (k == null ? "NONE" : k.getLocalizedText().getString()));
    }

    private Text openKeyText() {
        if (listening == OPEN_KEY) return Text.literal("> Press a key <").formatted(Formatting.YELLOW);
        return Text.literal("Menu key: ").append(KeyMacroMod.OPEN_KEY.getBoundKeyLocalizedText());
    }

    private void refresh() {
        for (int i = 0; i < keyButtons.length; i++) keyButtons[i].setMessage(slotKeyText(i));
        openKeyButton.setMessage(openKeyText());
    }

    // ----- key capture -----

    private void bind(InputUtil.Key key) {
        if (listening == OPEN_KEY) {
            KeyMacroMod.OPEN_KEY.setBoundKey(key);
            KeyBinding.updateKeysByCode();
            client.options.write();
        } else if (listening >= 0) {
            cfg.slots[listening].trigger = key.getTranslationKey();
        }
        listening = NONE;
        refresh();
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (listening != NONE) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                if (listening >= 0) cfg.slots[listening].trigger = ""; // Esc clears a slot's key
                listening = NONE;
                refresh();
            } else {
                bind(input.key() == GLFW.GLFW_KEY_UNKNOWN
                    ? InputUtil.Type.SCANCODE.createFromCode(input.scancode())
                    : InputUtil.Type.KEYSYM.createFromCode(input.key()));
            }
            return true;
        }
        boolean typing = getFocused() instanceof TextFieldWidget tf && tf.isFocused();
        if (!typing && KeyMacroMod.OPEN_KEY.matchesKey(input)) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (listening != NONE) {
            if (click.button() >= 2) { // middle / side mouse buttons can be bound
                bind(InputUtil.Type.MOUSE.createFromCode(click.button()));
                return true;
            }
            listening = NONE;
            refresh();
        }
        return super.mouseClicked(click, doubled);
    }

    // ----- drawing -----

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        for (int i = 0; i < MacroConfig.SLOT_COUNT; i++) {
            int y = top + i * (PH + GAP);
            context.fill(panelX, y, panelX + PW, y + PH, 0xB0101010);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top - 16, 0xFFFFFFFF);
        for (int i = 0; i < MacroConfig.SLOT_COUNT; i++) {
            context.drawTextWithShadow(textRenderer, Text.literal("Slot " + (i + 1)),
                panelX + 8, top + i * (PH + GAP) + 10, 0xFFFFFFFF);
        }
        int hy = top + MacroConfig.SLOT_COUNT * PH + (MacroConfig.SLOT_COUNT - 1) * GAP + 36;
        String hint = listening != NONE
            ? "Press a key or mouse button 3+ (Esc = " + (listening >= 0 ? "clear" : "cancel") + ")"
            : "50 ms = 1 game tick (fastest). Hover fields for help.";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(hint), width / 2, hy,
            listening != NONE ? 0xFFFFFF55 : 0xFFAAAAAA);
    }

    @Override
    public void removed() {
        MacroConfig.save(cfg);
        runner.apply(cfg);
    }
}
