package org.mule.extension.whisperer.internal.helpers.models;

import org.apache.commons.io.FileUtils;
import org.mule.extension.whisperer.api.error.ConnectorError;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper class to download and configure Piper voice models.
 */
public class PiperModelConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PiperModelConfigurer.class);

    /**
     * Download a single file (model or config) from URL to local path.
     *
     * @param fileURL URL to download from
     * @param filePath Local path to save to
     */
    public static void downloadFile(String fileURL, String filePath) {
        try {
            LOGGER.info("Downloading file from: {}", fileURL);
            FileUtils.copyURLToFile(new URL(fileURL), new File(filePath));
            LOGGER.info("File downloaded from {} and installed at {}", fileURL, filePath);

        } catch (Exception e) {
            throw new ModuleException(
                String.format("Failed to download file from URL: %s", fileURL),
                ConnectorError.MODEL_SETUP_FAILURE,
                e);
        }
    }

    /**
     * Download both model and config files to the installation directory.
     *
     * @param modelURL URL for .onnx model file
     * @param configURL URL for .onnx.json config file
     * @param installationDir Directory to install files into
     * @param modelFileName Name for downloaded model file
     * @param configFileName Name for downloaded config file
     */
    public static void setup(String modelURL, String configURL, String installationDir,
                            String modelFileName, String configFileName) {

        try {
            // Create installation directory if it doesn't exist
            File dir = new File(installationDir);
            if (!dir.exists()) {
                dir.mkdirs();
                LOGGER.info("Created installation directory: {}", installationDir);
            }

            // Download model file
            Path modelPath = Paths.get(installationDir, modelFileName);
            downloadFile(modelURL, modelPath.toString());

            // Download config file
            Path configPath = Paths.get(installationDir, configFileName);
            downloadFile(configURL, configPath.toString());

            LOGGER.info("Piper voice model setup complete at {}", installationDir);

        } catch (Exception e) {
            throw new ModuleException(
                String.format("Failed to setup Piper model from URLs: %s, %s", modelURL, configURL),
                ConnectorError.MODEL_SETUP_FAILURE,
                e);
        }
    }
}
