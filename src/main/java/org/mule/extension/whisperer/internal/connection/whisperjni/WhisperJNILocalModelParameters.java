package org.mule.extension.whisperer.internal.connection.whisperjni;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;

public class WhisperJNILocalModelParameters {

  @Parameter
  @DisplayName("File path")
  @Alias("modelFilePath")
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("\"classpath://models/ggml-model-whisper-tiny.en-q8_0.bin\"")
  private String modelFilePath;

  public String getModelFilePath() {
      return modelFilePath;
  }

  public void setModelFilePath(String modelFilePath) {
      this.modelFilePath = modelFilePath;
  }
}
