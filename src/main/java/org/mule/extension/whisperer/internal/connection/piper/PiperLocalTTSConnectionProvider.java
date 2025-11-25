package org.mule.extension.whisperer.internal.connection.piper;

import io.github.givimad.piperjni.PiperJNI;
import io.github.givimad.piperjni.PiperVoice;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.core.api.lifecycle.StartException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Piper local TTS connection provider.
 * Supports loading voice models from filesystem or classpath resources.
 */
@Alias("piperlocal")
@DisplayName("Piper (Local .onnx)")
public class PiperLocalTTSConnectionProvider implements CachedConnectionProvider<PiperTTSConnection>, Startable, Stoppable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PiperLocalTTSConnectionProvider.class);

    @ParameterGroup(name = "Model")
    @Expression(ExpressionSupport.SUPPORTED)
    private PiperLocalModelParameters model;

    private PiperJNI piper;
    private PiperVoice voice;
    private Path tempModelFile;
    private Path tempConfigFile;

    @Override
    public PiperTTSConnection connect() throws ConnectionException {
        return new PiperTTSConnection(piper, voice, model.getVoiceName());
    }

    @Override
    public void disconnect(PiperTTSConnection piperTTSConnection) {
        // No-op: cleanup handled in stop()
    }

    @Override
    public ConnectionValidationResult validate(PiperTTSConnection piperTTSConnection) {
        return ConnectionValidationResult.success();
    }

    /**
     * Resolves the model file path, handling both classpath resources and file system paths.
     * If the path starts with "classpath://", the resource is extracted to a temporary file.
     *
     * @param filePath The file path (can be classpath:// or absolute file path)
     * @param prefix Prefix for temp file name (e.g., "piper-model-")
     * @return Path to the file on the file system
     * @throws IOException if the file cannot be resolved or extracted
     */
    private Path resolveFilePath(String filePath, String prefix) throws IOException {
        if (filePath.startsWith("classpath://")) {
            String resourcePath = filePath.substring("classpath://".length());
            LOGGER.debug("Loading file from classpath resource: {}", resourcePath);

            InputStream resourceStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath);

            if (resourceStream == null) {
                throw new IOException("File not found in classpath: " + resourcePath);
            }

            // Extract to temporary file
            String fileName = Paths.get(resourcePath).getFileName().toString();
            Path tempFile = Files.createTempFile(prefix, "-" + fileName);

            LOGGER.debug("Extracting classpath resource to temporary file: {}", tempFile);
            Files.copy(resourceStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            resourceStream.close();

            // Mark for deletion on JVM exit as backup cleanup
            tempFile.toFile().deleteOnExit();

            return tempFile;
        } else {
            // Regular file system path
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new IOException("File not found: " + filePath);
            }
            return path;
        }
    }

    @Override
    public void start() throws MuleException {
        try {
            LOGGER.info("Initializing Piper TTS with voice: {}", model.getVoiceName());

            // Initialize piper-jni
            piper = new PiperJNI();
            piper.initialize(true, false);

            // Resolve model and config paths (handles both classpath and file system)
            Path modelPath = resolveFilePath(model.getModelFilePath(), "piper-model-");
            Path configPath = resolveFilePath(model.getConfigFilePath(), "piper-config-");

            // Track temp files for cleanup
            if (model.getModelFilePath().startsWith("classpath://")) {
                tempModelFile = modelPath;
            }
            if (model.getConfigFilePath().startsWith("classpath://")) {
                tempConfigFile = configPath;
            }

            // Load voice model
            voice = piper.loadVoice(modelPath, configPath);

            LOGGER.info("Piper TTS initialized successfully. Voice: {}, Sample Rate: {} Hz",
                model.getVoiceName(), voice.getSampleRate());

        } catch (Exception e) {
            throw new StartException(e, this);
        }
    }

    @Override
    public void stop() throws MuleException {
        LOGGER.info("Stopping Piper TTS connection provider");

        // Clean up voice
        if (voice != null) {
            try {
                voice.close();
                LOGGER.debug("Piper voice closed");
            } catch (Exception e) {
                LOGGER.warn("Error closing Piper voice: {}", e.getMessage(), e);
            }
        }

        // Clean up piper
        if (piper != null) {
            try {
                piper.terminate();
                LOGGER.debug("Piper JNI terminated");
            } catch (Exception e) {
                LOGGER.warn("Error terminating Piper JNI: {}", e.getMessage(), e);
            }
        }

        // Clean up temporary model file if it was created
        if (tempModelFile != null && Files.exists(tempModelFile)) {
            try {
                Files.delete(tempModelFile);
                LOGGER.info("Deleted temporary model file: {}", tempModelFile);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete temporary model file: {}", tempModelFile, e);
            }
        }

        // Clean up temporary config file if it was created
        if (tempConfigFile != null && Files.exists(tempConfigFile)) {
            try {
                Files.delete(tempConfigFile);
                LOGGER.info("Deleted temporary config file: {}", tempConfigFile);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete temporary config file: {}", tempConfigFile, e);
            }
        }
    }
}
