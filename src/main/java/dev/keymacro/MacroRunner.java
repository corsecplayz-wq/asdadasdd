package dev.keymacro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Each slot: when its trigger key is pressed, swap through the listed hotbar
 * slots, waiting delayMs between swaps (rounded to game ticks, min 1 tick).
 */
final class MacroRunner {
    private static final class State {
        InputUtil.Key trigger;
        int[] seq = new int[0];
        int delayTicks = 1;
        boolean useItem, loop;
        boolean wasDown = true; // avoids a spurious trigger right after (re)load
        boolean running;
        int idx, wait;
    }

    private MacroConfig cfg;
    private final State[] states = new State[MacroConfig.SLOT_COUNT];

    MacroRunner() {
        apply(MacroConfig.load());
    }

    MacroConfig config() {
        return cfg;
    }

    int reload() {
        apply(MacroConfig.load());
        int n = 0;
        for (State s : states) if (s.trigger != null && s.seq.length > 0) n++;
        return n;
    }

    void apply(MacroConfig c) {
        c.normalize();
        cfg = c;
        for (int i = 0; i < MacroConfig.SLOT_COUNT; i++) {
            MacroConfig.Slot s = c.slots[i];
            State st = new State();
            st.trigger = s.enabled ? keyOrNull(s.trigger) : null;
            st.seq = parseHotbar(s.hotbar);
            st.delayTicks = Math.max(1, (int) Math.round(s.delayMs / 50.0));
            st.useItem = s.useItem;
            st.loop = s.loop;
            states[i] = st;
        }
    }

    void stopAll() {
        for (State s : states) s.running = false;
    }

    void tick(MinecraftClient client) {
        long handle = GLFW.glfwGetCurrentContext();
        if (handle == 0L) return;
        boolean canRun = cfg.enabled && client.player != null && client.currentScreen == null;

        for (State st : states) {
            if (st.trigger == null || st.seq.length == 0) continue;

            boolean down = isDown(handle, st.trigger);
            if (down && !st.wasDown && !st.running && canRun) {
                st.running = true;
                st.idx = 0;
                st.wait = 0;
            }
            st.wasDown = down;

            if (!st.running) continue;
            if (!canRun) { st.running = false; continue; }

            if (st.wait > 0 && --st.wait > 0) continue;

            int slot = st.seq[st.idx++];
            tap(client.options.hotbarKeys[slot - 1]);
            if (st.useItem) tap(client.options.useKey);

            if (st.idx >= st.seq.length) {
                if (st.loop && down) st.idx = 0;
                else { st.running = false; continue; }
            }
            st.wait = st.delayTicks;
        }
    }

    /** Fires whatever key the player has bound to this action (respects rebinding). */
    private static void tap(KeyBinding kb) {
        InputUtil.Key k = keyOrNull(kb.getBoundKeyTranslationKey());
        if (k != null) KeyBinding.onKeyPressed(k);
    }

    private static boolean isDown(long handle, InputUtil.Key key) {
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(handle, key.getCode()) == GLFW.GLFW_PRESS;
        }
        if (key.getCategory() == InputUtil.Type.KEYSYM) {
            return GLFW.glfwGetKey(handle, key.getCode()) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    static InputUtil.Key keyOrNull(String translationKey) {
        if (translationKey == null || translationKey.isBlank()
                || translationKey.equals("key.keyboard.unknown")) return null;
        try {
            return InputUtil.fromTranslationKey(translationKey);
        } catch (Exception e) {
            return null;
        }
    }

    static int[] parseHotbar(String text) {
        List<Integer> out = new ArrayList<>();
        for (String part : text.split("[,\\s]+")) {
            if (part.isEmpty()) continue;
            try {
                int n = Integer.parseInt(part);
                if (n >= 1 && n <= 9 && out.size() < 32) out.add(n);
            } catch (NumberFormatException ignored) {}
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }
}
