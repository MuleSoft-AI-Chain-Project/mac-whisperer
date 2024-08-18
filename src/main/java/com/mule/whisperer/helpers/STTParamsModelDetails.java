package com.mule.whisperer.helpers;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class STTParamsModelDetails {
	@Parameter
	@Expression(ExpressionSupport.SUPPORTED)
	@OfValues(ModelNameProvider.class)
	@Optional(defaultValue = "whisper-1")
	private String modelName;

	public String getModelName() {
		return modelName;
	}

	@Parameter
	@Expression(ExpressionSupport.SUPPORTED)
	@Optional(defaultValue = "auto")
	private String language;

	public String getLanguage() {	
		return language;
	}

	@Parameter
	@Expression(ExpressionSupport.SUPPORTED)
	@Optional(defaultValue = "0.9")
	private Number temperature;

	public Number getTemperature() {
		return temperature;
	}


	@Parameter
	@Expression(ExpressionSupport.SUPPORTED)
	@OfValues(ResponseFormatTTS.class)
	@Optional(defaultValue = "json")
	private String responseFormat;

	public String getResponseFormat() {
		return responseFormat;
	}



	
}