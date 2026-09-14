package dev.skinroller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

record UploadState(long lastUpload, long backoffUntil) {

    static final String FILE = "skinroller-state.properties";

    static UploadState load(Path file) {
        if (!Files.exists(file)) return new UploadState(0, 0);
        try {
            Map<String, String> kv = Config.readKeyValues(file);
            return new UploadState(Long.parseLong(kv.get("lastUpload")), Long.parseLong(kv.getOrDefault("backoffUntil", "0")));
        } catch (IOException | RuntimeException e) {
            SkinRollerMod.LOG.warn("unreadable {}; treating it as a fresh upload", file, e);
            UploadState fresh = new UploadState(System.currentTimeMillis(), 0);
            try {
                fresh.save(file);
            } catch (IOException ignored) {
            }
            return fresh;
        }
    }

    void save(Path file) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, "lastUpload=" + lastUpload + "\nbackoffUntil=" + backoffUntil + "\n");
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
