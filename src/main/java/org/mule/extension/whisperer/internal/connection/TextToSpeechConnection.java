package org.mule.extension.whisperer.internal.connection;

import org.mule.extension.whisperer.api.TTSParamsModelDetails;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public interface TextToSpeechConnection {
    CompletableFuture<InputStream> generate(String text, TTSParamsModelDetails params);
}
