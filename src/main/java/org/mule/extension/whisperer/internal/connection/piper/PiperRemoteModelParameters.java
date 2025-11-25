package org.mule.extension.whisperer.internal.connection.piper;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * Parameters for Piper remote TTS model configuration.
 * Downloads voice models from Hugging Face and caches them locally.
 */
public class PiperRemoteModelParameters {

    @Parameter
    @DisplayName("Model Repository URL")
    @Expression(ExpressionSupport.SUPPORTED)
    @Placement(order = 1)
    @Example("https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/en_US-lessac-medium.onnx")
    private String modelURL;

    @Parameter
    @DisplayName("Config Repository URL")
    @Expression(ExpressionSupport.SUPPORTED)
    @Placement(order = 2)
    @Example("https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/en_US-lessac-medium.onnx.json")
    private String configURL;

    @Parameter
    @DisplayName("Installation Directory Path")
    @Expression(ExpressionSupport.SUPPORTED)
    @Placement(order = 3)
    @Example("mule.home ++ \"/apps/\" ++ app.name ++ \"/piper-models\"")
    private String installationFilePath;

    public String getModelURL() {
        return modelURL;
    }

    public void setModelURL(String modelURL) {
        this.modelURL = modelURL;
    }

    public String getConfigURL() {
        return configURL;
    }

    public void setConfigURL(String configURL) {
        this.configURL = configURL;
    }

    public String getInstallationFilePath() {
        return installationFilePath;
    }

    public void setInstallationFilePath(String installationFilePath) {
        this.installationFilePath = installationFilePath;
    }

    /**
     * Extract filename from URL
     */
    public String getModelFileName() {
        return extractFileName(modelURL);
    }

    /**
     * Extract filename from URL
     */
    public String getConfigFileName() {
        return extractFileName(configURL);
    }

    private String extractFileName(String url) {
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String path = urlObj.getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);

            if (fileName.isEmpty()) {
                throw new IllegalArgumentException("No filename found in URL: " + url);
            }

            return fileName;
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }
}
