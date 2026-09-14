package dev.skinroller;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SkinRollerMod implements ClientModInitializer {

    static final Logger LOG = LoggerFactory.getLogger("skinroller");

    private static final long RATE_LIMIT_BACKOFF_MS = TimeUnit.HOURS.toMillis(1);
    private static final int MAX_LOGGED_BODY = 300;

    private final AtomicBoolean busy = new AtomicBoolean();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "skinroller-upload");
        t.setDaemon(true);
        return t;
    });
    private Path configDir;

    @Override
    public void onInitializeClient() {
        configDir = FabricLoader.getInstance().getConfigDir();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> trigger(client, "startup", false));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> trigger(client, "disconnect", true));
    }

    private void trigger(Minecraft client, String reason, boolean useCooldown) {
        String token = client.getUser().getAccessToken();
        if (token == null || token.isBlank()) {
            LOG.info("[{}] skipped: no access token", reason);
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            LOG.info("[{}] skipped: an upload is already running", reason);
            return;
        }
        worker.execute(() -> {
            try {
                run(reason, token, useCooldown);
            } catch (Exception e) {
                LOG.error("[{}] upload failed", reason, e);
            } finally {
                busy.set(false);
            }
        });
    }

    private void run(String reason, String token, boolean useCooldown) throws IOException, InterruptedException {
        Config cfg = Config.load(configDir);
        Path statePath = configDir.resolve(UploadState.FILE);
        UploadState state = UploadState.load(statePath);
        long now = System.currentTimeMillis();

        if (now < state.backoffUntil()) {
            LOG.info("[{}] skipped: backing off after a 429, {} min left", reason, minutesLeft(state.backoffUntil(), now));
            return;
        }
        long readyAt = state.lastUpload() + TimeUnit.MINUTES.toMillis(cfg.cooldownMinutes());
        if (useCooldown && now < readyAt) {
            LOG.info("[{}] skipped: cooldown, {} min left", reason, minutesLeft(readyAt, now));
            return;
        }

        new UploadState(now, 0).save(statePath);

        byte[] png = SkinPainter.paint(cfg.baseSkin(), cfg.scrambleLength(), new Random());
        handleResponse(reason, cfg, SkinUploader.upload(png, cfg.variant(), token), now, statePath);
    }

    private static void handleResponse(String reason, Config cfg, SkinUploader.Response res, long now, Path statePath)
            throws IOException {
        int status = res.status();
        if (status >= 200 && status < 300) {
            LOG.info("[{}] skin uploaded ({})", reason, cfg.variant());
        } else if (status == 429) {
            long wait = Math.max(RATE_LIMIT_BACKOFF_MS, TimeUnit.SECONDS.toMillis(res.retryAfterSeconds()));
            new UploadState(now, now + wait).save(statePath);
            LOG.warn("[{}] rate limited (HTTP 429); no uploads for {} min", reason, TimeUnit.MILLISECONDS.toMinutes(wait));
        } else if (status == 401) {
            LOG.warn("[{}] token rejected (HTTP 401); not a signed-in Microsoft account session?", reason);
        } else {
            LOG.warn("[{}] upload failed: HTTP {} {}", reason, status, abbreviate(res.body()));
        }
    }

    private static long minutesLeft(long until, long now) {
        return (until - now + 59_999) / 60_000;
    }

    private static String abbreviate(String body) {
        if (body == null) return "";
        return body.length() > MAX_LOGGED_BODY ? body.substring(0, MAX_LOGGED_BODY) + "..." : body;
    }
}
