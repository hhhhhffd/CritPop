package com.critpop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CritPopConfig {
    public static final int MAX_PHRASES = 64;
    public static final int MAX_LENGTH = 48;
    public static final List<String> DEFAULT_PHRASES = List.of(
            "CLEAN CRIT!", "LIGHTS OUT!", "THAT'S A HIT!", "POW!", "PERFECT DROP!",
            "SHARP WORK!", "BOOM!", "RIGHT ON TARGET!", "BIG ENERGY!", "NAILED IT!",
            "CRIT CONFIRMED!", "IN THE ZONE!");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("critpop.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LoggerFactory.getLogger("Crit Pop");
    private static CritPopConfig current;

    public boolean enabled = true;
    public List<String> phrases = DEFAULT_PHRASES;

    public static CritPopConfig get() {
        if (current == null) current = load();
        return current;
    }

    private static CritPopConfig load() {
        if (!Files.isRegularFile(FILE)) return new CritPopConfig();
        try (var reader = Files.newBufferedReader(FILE)) {
            CritPopConfig saved = GSON.fromJson(reader, CritPopConfig.class);
            if (saved != null) {
                saved.phrases = saved.phrases == null ? DEFAULT_PHRASES : sanitize(saved.phrases);
                return saved;
            }
        } catch (RuntimeException | IOException e) {
            LOGGER.warn("Could not read Crit Pop settings; using defaults", e);
        }
        return new CritPopConfig();
    }

    public static List<String> sanitize(List<String> phrases) {
        List<String> cleaned = new ArrayList<>();
        for (String raw : phrases) {
            if (raw == null) continue;
            StringBuilder text = new StringBuilder();
            raw.codePoints().filter(c -> !Character.isISOControl(c) && c != '§'
                    && Character.getType(c) != Character.FORMAT
                    && Character.getType(c) != Character.LINE_SEPARATOR
                    && Character.getType(c) != Character.PARAGRAPH_SEPARATOR)
                    .limit(MAX_LENGTH).forEach(text::appendCodePoint);
            String phrase = text.toString().strip();
            if (phrase.isEmpty()) continue;
            cleaned.add(phrase);
            if (cleaned.size() >= MAX_PHRASES) break;
        }
        return List.copyOf(cleaned);
    }

    public void apply(boolean enabled, List<String> phrases) {
        this.enabled = enabled;
        this.phrases = sanitize(phrases);
        Path temp = null;
        try {
            Files.createDirectories(FILE.getParent());
            temp = Files.createTempFile(FILE.getParent(), "critpop-", ".tmp");
            try (var writer = Files.newBufferedWriter(temp)) {
                GSON.toJson(this, writer);
            }
            try {
                Files.move(temp, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not save Crit Pop settings", e);
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            }
        }
    }
}
