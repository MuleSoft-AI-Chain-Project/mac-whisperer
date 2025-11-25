package org.mule.extension.whisperer.internal.connection.piper;

import io.github.givimad.piperjni.PiperJNI;
import io.github.givimad.piperjni.PiperVoice;
import org.mule.extension.whisperer.internal.helpers.models.PiperModelConfigurer;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Piper remote TTS connection provider.
 * Downloads voice models from Hugging Face and caches them locally.
 */
@Alias("piperurl")
@DisplayName("Piper (Remote .onnx)")
public class PiperRemoteTTSConnectionProvider implements CachedConnectionProvider<PiperTTSConnection>, Startable, Stoppable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PiperRemoteTTSConnectionProvider.class);

    @ParameterGroup(name = "Model")
    @Expression(ExpressionSupport.SUPPORTED)
    private PiperRemoteModelParameters model;

    private PiperJNI piper;
    private PiperVoice voice;

    @Override
    public PiperTTSConnection connect() throws ConnectionException {
        // Extract voice name from model URL for logging
        String voiceName = extractVoiceName();
        return new PiperTTSConnection(piper, voice, voiceName);
    }

    @Override
    public void disconnect(PiperTTSConnection piperTTSConnection) {
        // No-op: cleanup handled in stop()
    }

    @Override
    public ConnectionValidationResult validate(PiperTTSConnection piperTTSConnection) {
        return ConnectionValidationResult.success();
    }

    @Override
    public void start() throws MuleException {
        try {
            String installationDir = model.getInstallationFilePath();
            String modelFileName = model.getModelFileName();
            String configFileName = model.getConfigFileName();

            Path modelFilePath = Paths.get(installationDir, modelFileName);
            Path configFilePath = Paths.get(installationDir, configFileName);

            LOGGER.info("Checking for Piper voice model at: {}", modelFilePath);

            // Download model and config if they don't exist (double-checked locking)
            if (!Files.exists(modelFilePath) || !Files.exists(configFilePath)) {
                synchronized (PiperRemoteTTSConnectionProvider.class) {
                    if (!Files.exists(modelFilePath) || !Files.exists(configFilePath)) {
                        LOGGER.info("Downloading Piper voice model from remote repository");
                        PiperModelConfigurer.setup(
                            model.getModelURL(),
                            model.getConfigURL(),
                            installationDir,
                            modelFileName,
                            configFileName
                        );
                    }
                }
            } else {
                LOGGER.info("Piper voice model found in cache, skipping download");
            }

            // Initialize piper-jni
            piper = new PiperJNI();
            piper.initialize(true, false);

            // Load voice model
            voice = piper.loadVoice(modelFilePath, configFilePath);

            LOGGER.info("Piper TTS initialized successfully. Voice: {}, Sample Rate: {} Hz",
                extractVoiceName(), voice.getSampleRate());

        } catch (Exception e) {
            throw new StartException(e, this);
        }
    }

    @Override
    public void stop() throws MuleException {
        LOGGER.info("Stopping Piper TTS remote connection provider");

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

        // Note: We intentionally DO NOT delete downloaded model files
        // They should persist across restarts for caching
    }

    /**
     * Extract a friendly voice name from the model filename.
     * Example: "en_US-lessac-medium.onnx" → "en_US-lessac-medium"
     */
    private String extractVoiceName() {
        String modelFileName = model.getModelFileName();
        if (modelFileName.endsWith(".onnx")) {
            return modelFileName.substring(0, modelFileName.length() - 5);
        }
        return modelFileName;
    }
}
