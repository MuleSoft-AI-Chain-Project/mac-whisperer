package org.mule.extension.whisperer.internal.connection.openai;

import org.mule.extension.whisperer.api.OpenAiTranscriptionAttributes;
import org.mule.extension.whisperer.api.STTParamsModelDetails;
import org.mule.extension.whisperer.api.TTSParamsModelDetails;
import org.mule.extension.whisperer.internal.connection.SpeechToTextConnection;
import org.mule.extension.whisperer.internal.connection.TextToSpeechConnection;
import org.mule.extension.whisperer.internal.error.GenerationException;
import org.mule.extension.whisperer.internal.error.TranscriptionException;
import org.mule.extension.whisperer.internal.helpers.audio.AudioUtils;
import org.json.JSONObject;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.entity.multipart.HttpPart;
import org.mule.runtime.http.api.domain.entity.multipart.MultipartHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;

public class OpenAiConnection implements SpeechToTextConnection, TextToSpeechConnection {
    private static Logger LOGGER = LoggerFactory.getLogger(OpenAiConnection.class);
    private final String apiKey;
    private final HttpClient httpClient;
    private final URI apiUri;
    public OpenAiConnection(String apiKey, HttpClient httpClient, URI apiUri) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.apiUri = apiUri;
    }

    public void validate() throws ConnectionException {
        // health check with https://platform.openai.com/docs/api-reference/models
        URI healthCheckEndpoint = apiUri.resolve("models");
        HttpRequest request = HttpRequest.builder()
                .addHeader("Authorization", "Bearer " + apiKey)
                .method(GET)
                .uri(healthCheckEndpoint)
                .build();
        try {
            HttpResponse response = httpClient.send(request);

            if (200 != response.getStatusCode()) {
                throw new ConnectionException("Unexpected status code " + response.getStatusCode() + " from " + healthCheckEndpoint.toString());
            }
            LOGGER.trace("Successfully validated connection " + healthCheckEndpoint.toString());
        } catch (IOException e) {
            throw new ConnectionException(e);
        } catch (TimeoutException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public CompletableFuture<Result<String, Object>> transcribe(TypedValue<InputStream> audioContent, String fineTuningPrompt, STTParamsModelDetails params) {
        URI transcriptionEndpoint = apiUri.resolve("audio/transcriptions");

        String responseFormat = "text";
        if (params.isVerbose()) {
            responseFormat = "verbose_json";
        }

        byte[] audioBytes = IOUtils.toByteArray(audioContent.getValue());
        ArrayList<HttpPart> parts = new ArrayList<>();

        parts.add(new HttpPart("model", params.getModelName().getBytes(), MediaType.TEXT_PLAIN, params.getModelName().getBytes().length));
        parts.add(new HttpPart("response_format", responseFormat.getBytes(), MediaType.TEXT_PLAIN, responseFormat.getBytes().length));
        parts.add(new HttpPart("file", "speech." + AudioUtils.guessAudioFileExtension(audioContent.getDataType().getMediaType()), audioBytes, audioContent.getDataType().getMediaType().toString(), audioBytes.length));

        if (null != fineTuningPrompt && !fineTuningPrompt.isEmpty()) {
            parts.add(new HttpPart("prompt", fineTuningPrompt.getBytes(), MediaType.TEXT_PLAIN, fineTuningPrompt.getBytes().length));
        }
        if (params.getTemperature().floatValue() > 0) {
            parts.add(new HttpPart("temperature", params.getTemperature().toString().getBytes(), MediaType.TEXT_PLAIN, params.getTemperature().toString().getBytes().length));
        }
        if (null != params.getLanguage() && !params.getLanguage().isEmpty()) {
            parts.add(new HttpPart("language", params.getLanguage().getBytes(), MediaType.TEXT_PLAIN, params.getLanguage().getBytes().length));
        }
        HttpRequest request = HttpRequest.builder()
                .addHeader("Authorization", "Bearer " + apiKey)
                .method(POST)
                .uri(transcriptionEndpoint)
                .entity(new MultipartHttpEntity(parts))
                .build();
        return httpClient.sendAsync(request)
                .thenApply(response -> {
                    if (200 != response.getStatusCode()) {
                        LOGGER.error(IOUtils.toString(response.getEntity().getContent()));
                        throw new TranscriptionException("Unexpected status code " + response.getStatusCode() + " from OpenAI API");
                    }
                    if (params.isVerbose()) {
                        JSONObject responseObject = new JSONObject(IOUtils.toString(response.getEntity().getContent()));
                        OpenAiTranscriptionAttributes attributes = new OpenAiTranscriptionAttributes();
                        attributes.setLanguage(responseObject.optString("language"));
                        attributes.setDuration(responseObject.optDouble("duration"));
                        // not yet supporting words and segments arrays
                        return Result.<String, Object>builder()
                                .output(responseObject.getString("text"))
                                .attributes(attributes)
                                .build();
                    } else {
                        return Result.<String, Object>builder()
                                .output(IOUtils.toString(response.getEntity().getContent()))
                                .build();
                    }
                });
    }

    @Override
    public CompletableFuture<InputStream> generate(String text, TTSParamsModelDetails params) {
        URI speechEndpoint = apiUri.resolve("audio/speech");

        JSONObject requestObject = new JSONObject();
        requestObject.put("model", params.getModelName());
        requestObject.put("input", text);
        requestObject.put("voice", params.getVoice());
        requestObject.put("response_format", params.getResponseFormat());
        requestObject.put("speed", params.getSpeed());

        HttpRequest request = HttpRequest.builder()
                .addHeader("Authorization", "Bearer " + apiKey)
                .method(POST)
                .uri(speechEndpoint)
                .addHeader("Content-Type", "application/json")
                .entity(new ByteArrayHttpEntity(requestObject.toString().getBytes()))
                .build();

        return httpClient.sendAsync(request).thenApply(response -> {
            if (200 != response.getStatusCode()) {
                LOGGER.error(IOUtils.toString(response.getEntity().getContent()));
                throw new GenerationException("Unexpected status code " + response.getStatusCode() + " from OpenAI API");
            }
            return response.getEntity().getContent();
        });
    }

}
