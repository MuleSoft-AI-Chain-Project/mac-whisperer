package org.mule.extension.whisperer.internal.operation;

import org.mule.extension.whisperer.api.STTParamsModelDetails;
import org.mule.extension.whisperer.internal.connection.SpeechToTextConnection;
import org.mule.extension.whisperer.internal.error.TranscriptionErrorTypeProvider;
import org.mule.extension.whisperer.internal.metadata.TranscriptionOutputResolver;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.*;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class SpeechToTextOperations {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpeechToTextOperations.class);

    @DisplayName("Speech to Text")
    @Alias("speech-to-text")
    @OutputResolver(output = TranscriptionOutputResolver.class, attributes = TranscriptionOutputResolver.class)
    @MediaType(value = MediaType.TEXT_PLAIN, strict = false)
    @Throws(TranscriptionErrorTypeProvider.class)
    public void transcribe(@Connection SpeechToTextConnection connection,
                           @Content TypedValue<InputStream> audioContent,
                           @Optional String finetuningPrompt,
                           @MetadataKeyId @ParameterGroup(name = "Transcription Options") STTParamsModelDetails transcriptionOptions,
                           CompletionCallback<String, Object> callback) {
        connection.transcribe(audioContent, finetuningPrompt, transcriptionOptions).whenComplete((result, e) -> {
            if (null == e) {
                callback.success(Result.<String, Object>builder()
                        .output(result.getOutput())
                        .build());
            } else {
                callback.error(e.getCause());
            }
        });
    }
}
