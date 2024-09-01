package com.mule.whisperer;

import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import com.mule.whisperer.internal.MuleChainVoiceOperations;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Operations(MuleChainVoiceOperations.class)
public class MuleChainVoiceConfiguration {

  @Parameter
  private String apiKey;

  @Parameter
   private boolean useLocalWhisper;

  public String getApiKey(){
    return apiKey;
  }

  public boolean isUseLocalWhisper() {
    return this.useLocalWhisper;
  }
}
