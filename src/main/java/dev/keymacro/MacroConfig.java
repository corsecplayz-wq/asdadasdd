package dev.keymacro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MacroConfig {
    public static final int SLOT_COUNT = 3;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public Slot[] slots = { new Slot(), new Slot(), new Slot() };

    public static class Slot {
        public boolean enabled = true;
        public String trigger = "";   // translation key, e.g. "key.keyboard.r"; empty = unbound
        public String hotbar = "2,1"; // hotbar slots to swap through, e.g. "2,1,3"
        public int delayMs = 100;     // time between swaps (min 50 ms = 1 tick)
        public boolean useItem = false; // right-click after each swap
        public boolean loop = false;    // repeat while trigger is held
    }

    void normalize() {
        Slot[] fixed = new Slot[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            Slot s = (slots != null && i < slots.length && slots[i] != null) ? slots[i] : new Slot();
            if (s.trigger == null) s.trigger = "";
            if (s.hotbar == null) s.hotbar = "";
            fixed[i] = s;
        }
        slots = fixed;
    }

    static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("keymacro.json");
    }

    static MacroConfig load() {
        MacroConfig cfg = new MacroConfig();
        Path p = path();
        if (Files.exists(p)) {
            try {
                MacroConfig read = GSON.fromJson(Files.readString(p), MacroConfig.class);
                if (read != null) cfg = read;
            } catch (Exception e) {
                KeyMacroMod.LOG.error("Could not read config, using defaults", e);
            }
        }
        cfg.normalize();
        return cfg;
    }

    static void save(MacroConfig cfg) {
        try {
            Files.writeString(path(), GSON.toJson(cfg));
        } catch (IOException e) {
            KeyMacroMod.LOG.error("Could not save config", e);
        }
    }
}
