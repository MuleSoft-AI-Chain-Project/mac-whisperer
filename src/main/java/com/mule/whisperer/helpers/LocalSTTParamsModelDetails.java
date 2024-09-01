package com.mule.whisperer.helpers;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

public class LocalSTTParamsModelDetails {

   @Parameter
   @Expression(ExpressionSupport.SUPPORTED)
   @Optional(defaultValue = "4")
   private int nThreads;

   @Parameter
   @Expression(ExpressionSupport.SUPPORTED)
   @Optional(defaultValue = "en")
   private String language;

   @Parameter
   @Expression(ExpressionSupport.SUPPORTED)
   @Optional(defaultValue = "false")
   private boolean translate;

   @Parameter
   @Expression(ExpressionSupport.SUPPORTED)
   @Optional(defaultValue = "true")
   private boolean printProgress;

   @Parameter
   @Expression(ExpressionSupport.SUPPORTED)
   @Optional(defaultValue = "path/to/your/whisper/model.bin")
   private String modelPath;

   @Parameter
   @DisplayName("Audio Format")
   @Optional(defaultValue = "WAV")
   private AudioFormat audioFormat;

   public int getNThreads() {
      return this.nThreads;
   }

   public String getLanguage() {
      return this.language;
   }

   public boolean isTranslate() {
      return this.translate;
   }

   public boolean isPrintProgress() {
      return this.printProgress;
   }

   public Path getModelPath() {
      return Paths.get(this.modelPath);
   }

   public AudioFormat getAudioFormat() {
      return this.audioFormat;
   }

   public enum AudioFormat {
      WAV, MP3
   }
}
