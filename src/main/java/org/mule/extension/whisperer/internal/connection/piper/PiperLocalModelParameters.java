package org.mule.extension.whisperer.internal.connection.piper;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;

/**
 * Parameters for Piper local TTS model configuration.
 * Supports both filesystem and classpath:// URIs for model files.
 */
public class PiperLocalModelParameters {

    @Parameter
    @DisplayName("Voice Name")
    @Alias("voiceName")
    @Expression(ExpressionSupport.SUPPORTED)
    @Optional(defaultValue = "en_US-lessac-medium")
    @Example("\"en_US-lessac-medium\"")
    private String voiceName;

    @Parameter
    @DisplayName("Model File Path")
    @Alias("modelFilePath")
    @Expression(ExpressionSupport.SUPPORTED)
    @Optional(defaultValue = "classpath://models/piper/en_US-lessac-medium.onnx")
    @Example("\"classpath://models/piper/en_US-lessac-medium.onnx\"")
    private String modelFilePath;

    @Parameter
    @DisplayName("Config File Path")
    @Alias("configFilePath")
    @Expression(ExpressionSupport.SUPPORTED)
    @Optional(defaultValue = "classpath://models/piper/en_US-lessac-medium.onnx.json")
    @Example("\"classpath://models/piper/en_US-lessac-medium.onnx.json\"")
    private String configFilePath;

    public String getVoiceName() {
        return voiceName;
    }

    public void setVoiceName(String voiceName) {
        this.voiceName = voiceName;
    }

    public String getModelFilePath() {
        return modelFilePath;
    }

    public void setModelFilePath(String modelFilePath) {
        this.modelFilePath = modelFilePath;
    }

    public String getConfigFilePath() {
        return configFilePath;
    }

    public void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }
}
