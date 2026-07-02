package dev.createrecipehooks.internal.debug;

import dev.createrecipehooks.CreateRecipeHooksMod;
import dev.createrecipehooks.api.RecipeFinishedContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Dedicated file logger for CRH debug mode.
 *
 * <p>Activated by {@code -Dcrh.debug=true}. Writes one line per recipe event to
 * {@code logs/crh-events.log} using a separate Log4j2 logger so output never
 * mingles with the main game log.
 *
 * <p>Line format:
 * <pre>yyyy-MM-dd HH:mm:ss | SOURCE | recipeId | player | NxItem, ...</pre>
 *
 * <p><strong>Internal — not part of the public API. May change without notice.</strong>
 */
public final class CrhDebugLogger {

    private static final String LOGGER_NAME = "CRH-Events";
    private static final String LOG_FILE    = "logs/crh-events.log";

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private static volatile Logger fileLogger;

    /** Call once at mod startup when debug mode is active. */
    public static void init() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration cfg = ctx.getConfiguration();

            PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%msg%n")
                .withConfiguration(cfg)
                .build();

            FileAppender appender = FileAppender.newBuilder()
                .setName("CrhEventsFile")
                .withFileName(LOG_FILE)
                .withAppend(true)
                .setIgnoreExceptions(true)
                .setLayout(layout)
                .setConfiguration(cfg)
                .build();
            appender.start();

            cfg.addAppender(appender);

            LoggerConfig loggerCfg = new LoggerConfig(LOGGER_NAME, Level.INFO, false);
            loggerCfg.addAppender(appender, Level.INFO, null);
            cfg.addLogger(LOGGER_NAME, loggerCfg);
            ctx.updateLoggers();

            fileLogger = ctx.getLogger(LOGGER_NAME);
            CreateRecipeHooksMod.LOGGER.info("[CRH] Event log → {}", LOG_FILE);
        } catch (Exception e) {
            CreateRecipeHooksMod.LOGGER.error("[CRH] Failed to init event file logger", e);
        }
    }

    /**
     * Writes one log line for the given context.
     * No-op if {@link #init()} was not called or failed.
     */
    public static void log(RecipeFinishedContext ctx) {
        Logger logger = fileLogger;
        if (logger == null) return;

        String ts       = FMT.format(Instant.now());
        String source   = ctx.getSource().name();
        String recipeId = ctx.getRecipeId() != null ? ctx.getRecipeId().toString() : "-";
        String player   = ctx.getPlayer() != null ? ctx.getPlayer().getScoreboardName() : "-";
        String result   = ctx.getItemOutputs().stream()
            .map(s -> {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
                return s.getCount() + "x " + (id != null ? id : "unknown");
            })
            .collect(Collectors.joining(", "));
        if (result.isEmpty()) result = "-";

        logger.info("{} | {} | {} | {} | {}", ts, source, recipeId, player, result);
    }

    private CrhDebugLogger() {}
}
