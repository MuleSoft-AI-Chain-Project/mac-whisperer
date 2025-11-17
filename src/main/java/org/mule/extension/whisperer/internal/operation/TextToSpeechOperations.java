package org.mule.extension.whisperer.internal.operation;

import org.mule.extension.whisperer.api.TTSParamsModelDetails;
import org.mule.extension.whisperer.internal.connection.TextToSpeechConnection;
import org.mule.extension.whisperer.internal.error.GenerationErrorTypeProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class TextToSpeechOperations {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextToSpeechOperations.class);

    @DisplayName("Text to Speech")
    @Alias("text-to-speech")
    @MediaType(value = "audio/mp3", strict = false)
    @Throws(GenerationErrorTypeProvider.class)
    public void generateSpeech(@Connection TextToSpeechConnection connection,
                               @Content String text,
                               @ParameterGroup(name="Generation Options") TTSParamsModelDetails generationOptions,
                               CompletionCallback<InputStream, Void> callback) {

        connection.generate(text, generationOptions).whenComplete((audioData, e) -> {

            if (null == e) {
                callback.success(Result.<InputStream, Void>builder()
                        .output(audioData)
                        .mediaType(inferMediaType(generationOptions))
                        .build());
            } else {
                callback.error(e.getCause());
            }
        });
    }

    private org.mule.runtime.api.metadata.MediaType inferMediaType(TTSParamsModelDetails params) {
        switch (params.getResponseFormat()) {
            case "mp3":
                return org.mule.runtime.api.metadata.MediaType.create("audio", "mp3");
            case "ogg":
                return org.mule.runtime.api.metadata.MediaType.create("audio", "ogg");
            case "aac":
                return org.mule.runtime.api.metadata.MediaType.create("audio", "aac");
            case "flac":
                return org.mule.runtime.api.metadata.MediaType.create("audio", "flac");
            case "pcm":
                return org.mule.runtime.api.metadata.MediaType.create("audio", "pcm");
            case "wav":
                return org.mule.runtime.api.metadata.MediaType.create("audio", "wav");
            default:
                // normally this will be impossible
                LOGGER.warn("unknown media type for speech response format " + params.getResponseFormat());
                return org.mule.runtime.api.metadata.MediaType.BINARY;
        }
    }
}
