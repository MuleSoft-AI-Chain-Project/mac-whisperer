package com.mule.whisperer.helpers;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperJNI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class WhisperContextHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhisperContextHelper.class);

    // Use a ConcurrentHashMap to manage concurrent access to contexts
    private static final Map<Path, WhisperContext> whisperContextCache = new ConcurrentHashMap<>();

    /**
     * Gets an existing WhisperContext for the given model path or creates a new one if it doesn't exist.
     */
    public static WhisperContext getOrCreateWhisperContext(WhisperJNI whisper, Path modelPath) throws IOException {
        WhisperContext ctx = whisperContextCache.get(modelPath);
    
        if (ctx == null) {
            LOGGER.info("No existing context for model path: {}. Creating a new context.", modelPath);
            ctx = whisper.init(modelPath); // Cette ligne peut lancer IOException
    
            if (ctx != null) {
                whisperContextCache.put(modelPath, ctx);
                LOGGER.info("New Whisper context created and cached for model path: {}", modelPath);
            } else {
                LOGGER.error("Failed to create Whisper context for model path: {}", modelPath);
            }
        }
        return ctx;
    }

    /**
     * Closes and removes a WhisperContext for a given model path.
     * This method should be called when the model path changes or the application shuts down.
     */
    public static void closeWhisperContext(Path modelPath) {
        WhisperContext ctx = whisperContextCache.remove(modelPath);
        if (ctx != null) {
            ctx.close();
            LOGGER.info("Whisper context for model path {} has been closed and removed from the cache.", modelPath);
        }
    }
}
