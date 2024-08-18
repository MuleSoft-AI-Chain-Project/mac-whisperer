package com.mule.whisperer.helpers;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class TTSParamsModelDetails {
	@Parameter
	@Expression(ExpressionSupport.SUPPORTED)
	@OfValues(ModelNameProviderTTS.class)
	@Optional(defaultValue = "tts-1")
	private String modelName;

	public String getModelName() {
		return modelName;
	}

	@Parameter
	@Expression(ExpressionSupport.SUPPORTED)
	@OfValues(VoicesProvider.class)
	@Optional(defaultValue = "alloy")
	private String voice;

	public String getVoice() {	
		return voice;
	}

	@Parameter
	@Expression(ExpressionSupport.SUPPORTED)
	@Optional(defaultValue = "1.0")
	private Number speed;

	public Number getSpeed() {
		return speed;
	}


	@Parameter
	@Expression(ExpressionSupport.SUPPORTED)
	@OfValues(ResponseFormatSTT.class)
	@Optional(defaultValue = "mp3")
	private String responseFormat;

	public String getResponseFormat() {
		return responseFormat;
	}



	
}