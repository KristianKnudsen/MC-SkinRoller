package dev.skinroller;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

record Config(Path baseSkin, String variant, int scrambleLength, long cooldownMinutes) {

    static final long MIN_COOLDOWN_MINUTES = 5;

    private static final String FILE = "skinroller.properties";
    private static final String BUILTIN = "builtin";
    private static final String DEFAULTS = """
            # MC-SkinRoller config. Relative paths are resolved against this directory.
            # builtin = a random suit shipped with the mod; or a 64x64 .png, or a folder of them
            baseSkin=builtin
            # classic or slim; must match the base skin's arms
            variant=classic
            scrambleLength=25
            # never less than 5
            cooldownMinutes=5
            """;

    static Config load(Path configDir) throws IOException {
        Path file = configDir.resolve(FILE);
        if (!Files.exists(file)) {
            Files.createDirectories(configDir);
            Files.writeString(file, DEFAULTS);
            SkinRollerMod.LOG.info("wrote default config to {}", file);
        }
        Map<String, String> kv = readKeyValues(file);

        Path baseSkin = resolveBaseSkin(configDir, kv.getOrDefault("baseSkin", BUILTIN));
        String variant = kv.getOrDefault("variant", "classic").toLowerCase(Locale.ROOT);
        require(variant.equals("classic") || variant.equals("slim"), "variant must be classic or slim, got " + variant);
        int scrambleLength = Integer.parseInt(kv.getOrDefault("scrambleLength", "25"));
        require(scrambleLength >= 1, "scrambleLength must be at least 1, got " + scrambleLength);
        long cooldown = Long.parseLong(kv.getOrDefault("cooldownMinutes", "5"));
        if (cooldown < MIN_COOLDOWN_MINUTES) {
            SkinRollerMod.LOG.warn("cooldownMinutes={} is below the minimum; using {}", cooldown, MIN_COOLDOWN_MINUTES);
            cooldown = MIN_COOLDOWN_MINUTES;
        }
        return new Config(baseSkin, variant, scrambleLength, cooldown);
    }

    static Map<String, String> readKeyValues(Path file) throws IOException {
        Map<String, String> out = new HashMap<>();
        for (String line : Files.readAllLines(file)) {
            line = line.strip();
            int eq = line.indexOf('=');
            if (line.isEmpty() || line.startsWith("#") || eq < 0) continue;
            out.put(line.substring(0, eq).strip(), line.substring(eq + 1).strip());
        }
        return out;
    }

    private static Path resolveBaseSkin(Path configDir, String value) {
        if (!value.equalsIgnoreCase(BUILTIN)) return configDir.resolve(value);
        return FabricLoader.getInstance().getModContainer("skinroller")
                .flatMap(mod -> mod.findPath("skinroller/suits"))
                .orElseThrow(() -> new IllegalStateException("built-in suits missing from the mod jar"));
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalArgumentException(message);
    }
}
