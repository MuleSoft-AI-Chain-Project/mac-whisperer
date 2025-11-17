package org.mule.extension.whisperer.internal.connection.whisperjni;

import org.mule.extension.whisperer.api.STTParamsModelDetails;
import org.mule.extension.whisperer.api.error.ConnectorError;
import org.mule.extension.whisperer.internal.connection.SpeechToTextConnection;
import org.mule.extension.whisperer.internal.error.TranscriptionException;
import org.mule.extension.whisperer.internal.helpers.audio.AudioFileReader;
import org.mule.extension.whisperer.internal.helpers.audio.AudioUtils;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.concurrent.CompletableFuture;

public class WhisperJNIConnection implements SpeechToTextConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhisperJNIConnection.class);

    private final WhisperJNI whisper;
    private final WhisperContext whisperContext;
    private final int threads;
    private final boolean translate;
    private final boolean printProgress;

    public WhisperJNIConnection(WhisperJNI whisper, WhisperContext whisperContext, int threads, boolean translate, boolean printProgress) {
        this.whisper = whisper;
        this.whisperContext = whisperContext;
        this.threads = threads;
        this.translate = translate;
        this.printProgress = printProgress;
    }

    @Override
    public CompletableFuture<Result<String, Object>> transcribe(TypedValue<InputStream> audioContent, String fineTuningPrompt, STTParamsModelDetails params) {
        WhisperFullParams whisperParams = new WhisperFullParams();
        whisperParams.nThreads = threads;
        whisperParams.translate = translate;
        whisperParams.printProgress = printProgress;
        whisperParams.language = params.getLanguage();
        whisperParams.temperature = params.getTemperature().floatValue();
        whisperParams.initialPrompt = fineTuningPrompt;

        LOGGER.debug("Whisper context initialized successfully. Processing audio input.");

        // Save InputStream content to a temporary file
        String appHomePath = System.getProperty("app.home");
        File tempAudioFile = new File(appHomePath, "audio." + AudioUtils.guessAudioFileExtension(audioContent.getDataType().getMediaType()));

        try (OutputStream outStream = new FileOutputStream(tempAudioFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = audioContent.getValue().read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            return CompletableFuture.supplyAsync(() -> {
                throw new TranscriptionException("Unable to buffer audio data for transcription", e);
            });
        }
        LOGGER.trace("Audio file successfully saved to temporary file: {}", tempAudioFile.getAbsolutePath());

        // Convert audio to WAV format (supports all formats: MP3, M4A, WAV, FLAC, OGG, WEBM)
        String processedFilePath = tempAudioFile.getAbsolutePath();
        String wavFilePath = processedFilePath.replaceAll("\\.\\w+$", ".wav");

        try {
            LOGGER.trace("Converting audio file to WAV format via AudioConverter pipeline.");
            AudioFileReader.convertAudioToWav(processedFilePath, wavFilePath);
            processedFilePath = wavFilePath;
            LOGGER.debug("Audio file converted to WAV format: {}", wavFilePath);
        } catch (IOException e) {
            return CompletableFuture.supplyAsync(() -> {
                throw new TranscriptionException("Error converting audio content to WAV format", e);
            });
        }

        // Read audio file and extract samples
        LOGGER.trace("Reading audio file and extracting samples.");
        try {
            float[] samples = AudioFileReader.readFile(new File(processedFilePath));

            // Perform transcription
            LOGGER.debug("Performing speech-to-text operation with local Whisper.");
            int result = whisper.full(whisperContext, whisperParams, samples, samples.length);

            if (result != 0) {
                return CompletableFuture.supplyAsync(() -> {
                    throw new TranscriptionException("Transcription failed with code " + result);
                });
            }

            int segments = whisper.fullNSegments(whisperContext);
            // Collect the transcribed text from all segments
            // TODO: add segments attribute similar to OpenAI verbose
            StringBuilder transcription = new StringBuilder();
            for (int i = 0; i < segments; ++i) {
                transcription.append(whisper.fullGetSegmentText(whisperContext, i)).append(" ");
            }

            return CompletableFuture.supplyAsync(() -> Result.<String, Object>builder()
                .output(transcription.toString())
                .build());
        } catch (UnsupportedAudioFileException | IOException e) {
            return CompletableFuture.supplyAsync(() -> {
                throw new TranscriptionException(e);
            });
        }
    }
    public WhisperContext getWhisperContext() {
        return whisperContext;
    }
}
