package org.mule.extension.whisperer.internal.connection.piper;

import io.github.givimad.piperjni.PiperJNI;
import io.github.givimad.piperjni.PiperVoice;
import org.mule.extension.whisperer.api.TTSParamsModelDetails;
import org.mule.extension.whisperer.internal.connection.TextToSpeechConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;

/**
 * Piper TTS connection implementation.
 * Wraps piper-jni to provide text-to-speech capabilities.
 */
public class PiperTTSConnection implements TextToSpeechConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(PiperTTSConnection.class);

    private final PiperJNI piper;
    private final PiperVoice voice;
    private final String voiceName;

    public PiperTTSConnection(PiperJNI piper, PiperVoice voice, String voiceName) {
        this.piper = piper;
        this.voice = voice;
        this.voiceName = voiceName;
        LOGGER.info("PiperTTSConnection created with voice: {}", voiceName);
    }

    @Override
    public CompletableFuture<InputStream> generate(String text, TTSParamsModelDetails params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.debug("Generating speech for text length: {} characters", text.length());

                // Generate PCM audio samples
                short[] samples = piper.textToAudio(voice, text);
                int sampleRate = voice.getSampleRate();

                LOGGER.debug("Generated {} samples at {} Hz", samples.length, sampleRate);

                // Convert PCM samples to WAV format
                byte[] wavData = createWAVFile(samples, sampleRate);

                return new ByteArrayInputStream(wavData);

            } catch (Exception e) {
                LOGGER.error("Failed to generate speech: {}", e.getMessage(), e);
                throw new RuntimeException("Text-to-speech generation failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Disconnect is a no-op for Piper connection.
     * Resources are managed by the connection provider's lifecycle methods.
     */
    public void disconnect() {
        LOGGER.debug("Piper connection disconnect called (no-op)");
    }

    /**
     * Create a WAV file from PCM samples.
     * Format: PCM, 16-bit, mono
     *
     * @param samples PCM audio samples (16-bit signed)
     * @param sampleRate Sample rate in Hz
     * @return Complete WAV file as byte array
     */
    private byte[] createWAVFile(short[] samples, int sampleRate) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Write WAV header
        byte[] header = createWAVHeader(samples.length, sampleRate);
        outputStream.write(header);

        // Write PCM data (little-endian)
        ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (short sample : samples) {
            buffer.putShort(sample);
        }
        outputStream.write(buffer.array());

        return outputStream.toByteArray();
    }

    /**
     * Create WAV file header (44 bytes).
     * Format: RIFF/WAVE, PCM, 16-bit, mono
     *
     * @param numSamples Number of PCM samples
     * @param sampleRate Sample rate in Hz
     * @return WAV header bytes
     */
    private byte[] createWAVHeader(int numSamples, int sampleRate) throws IOException {
        ByteArrayOutputStream headerStream = new ByteArrayOutputStream();

        int dataSize = numSamples * 2; // 2 bytes per sample (16-bit)
        int fileSize = 36 + dataSize;  // Total file size minus 8 bytes for RIFF header

        // RIFF header
        headerStream.write("RIFF".getBytes());
        writeInt(headerStream, fileSize);
        headerStream.write("WAVE".getBytes());

        // fmt chunk
        headerStream.write("fmt ".getBytes());
        writeInt(headerStream, 16);              // Chunk size
        writeShort(headerStream, (short) 1);     // Audio format (1 = PCM)
        writeShort(headerStream, (short) 1);     // Number of channels (1 = mono)
        writeInt(headerStream, sampleRate);      // Sample rate
        writeInt(headerStream, sampleRate * 2);  // Byte rate (sampleRate * channels * bitsPerSample/8)
        writeShort(headerStream, (short) 2);     // Block align (channels * bitsPerSample/8)
        writeShort(headerStream, (short) 16);    // Bits per sample

        // data chunk
        headerStream.write("data".getBytes());
        writeInt(headerStream, dataSize);

        return headerStream.toByteArray();
    }

    /**
     * Write a 32-bit integer in little-endian format.
     */
    private void writeInt(ByteArrayOutputStream stream, int value) throws IOException {
        stream.write(value & 0xFF);
        stream.write((value >> 8) & 0xFF);
        stream.write((value >> 16) & 0xFF);
        stream.write((value >> 24) & 0xFF);
    }

    /**
     * Write a 16-bit short in little-endian format.
     */
    private void writeShort(ByteArrayOutputStream stream, short value) throws IOException {
        stream.write(value & 0xFF);
        stream.write((value >> 8) & 0xFF);
    }
}
