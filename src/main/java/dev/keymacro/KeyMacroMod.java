package dev.keymacro;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyMacroMod implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("keymacro");
    private static final MacroRunner RUNNER = new MacroRunner();
    private static final KeyBinding.Category CATEGORY =
        KeyBinding.Category.create(Identifier.of("keymacro", "main"));
    public static KeyBinding OPEN_KEY;

    @Override
    public void onInitializeClient() {
        // Default: Right Shift. Rebindable in Options > Controls > Key Macro,
        // or from the button inside the menu itself.
        OPEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.keymacro.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_KEY.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    client.setScreen(new MacroScreen(RUNNER));
                }
            }
            RUNNER.tick(client);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("keymacro")
                .then(ClientCommandManager.literal("reload").executes(ctx -> {
                    int n = RUNNER.reload();
                    ctx.getSource().sendFeedback(Text.literal("[KeyMacro] Loaded, " + n + " slot(s) active."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("stop").executes(ctx -> {
                    RUNNER.stopAll();
                    ctx.getSource().sendFeedback(Text.literal("[KeyMacro] Stopped."));
                    return 1;
                }))));
    }
}
