package org.mule.extension.whisperer.internal.helpers.audio;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.PointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swresample.*;

/**
 * ByteDeco FFmpeg-based audio converter for extended formats (M4A, AAC, FLAC, OGG, WEBM).
 * This class is only used when ByteDeco FFmpeg is available in the classpath.
 *
 * Uses ByteDeco's Java API (not command-line execution), eliminating PATH dependencies.
 * Preserves original sample rate and channels - use AudioFileReader.readFile() for 16kHz mono conversion.
 */
public class ByteDecoConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ByteDecoConverter.class);

    private static final int TARGET_SAMPLE_FORMAT = AV_SAMPLE_FMT_S16;  // 16-bit PCM

    static {
        // Initialize FFmpeg libraries
        try {
            Loader.load(org.bytedeco.ffmpeg.global.avutil.class);
            Loader.load(org.bytedeco.ffmpeg.global.avcodec.class);
            Loader.load(org.bytedeco.ffmpeg.global.avformat.class);
            Loader.load(org.bytedeco.ffmpeg.global.swresample.class);

            // Note: av_register_all() is deprecated in FFmpeg 6.x - no longer needed
            // Formats and codecs are registered automatically
            avformat_network_init();

            LOGGER.debug("ByteDeco FFmpeg libraries loaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to load ByteDeco FFmpeg libraries", e);
            throw new RuntimeException("ByteDeco FFmpeg initialization failed", e);
        }
    }

    /**
     * Converts audio file to WAV format (16-bit PCM) using ByteDeco FFmpeg.
     * Preserves original sample rate and channel count.
     *
     * @param inputPath Path to input audio file (M4A, AAC, FLAC, OGG, WEBM, etc.)
     * @param outputPath Path where WAV file should be written
     * @throws IOException if conversion fails
     */
    public static void convertToWav(String inputPath, String outputPath) throws IOException {
        LOGGER.debug("Starting audio conversion using ByteDeco FFmpeg: {} -> {}", inputPath, outputPath);

        AVFormatContext formatContext = null;
        AVCodecContext codecContext = null;
        SwrContext swrContext = null;
        AVFrame frame = null;
        AVFrame resampledFrame = null;
        AVPacket packet = null;

        try {
            // Step 1: Open input file
            formatContext = avformat_alloc_context();
            if (avformat_open_input(formatContext, inputPath, null, null) < 0) {
                throw new IOException("Could not open input file: " + inputPath);
            }

            // Step 2: Find stream information
            if (avformat_find_stream_info(formatContext, (PointerPointer<?>) null) < 0) {
                throw new IOException("Could not find stream information");
            }

            // Step 3: Find audio stream
            int audioStreamIndex = -1;
            AVStream audioStream = null;
            for (int i = 0; i < formatContext.nb_streams(); i++) {
                AVStream stream = formatContext.streams(i);
                if (stream.codecpar().codec_type() == AVMEDIA_TYPE_AUDIO) {
                    audioStreamIndex = i;
                    audioStream = stream;
                    break;
                }
            }

            if (audioStreamIndex == -1) {
                throw new IOException("Could not find audio stream in file");
            }

            // Step 4: Find decoder
            AVCodec codec = avcodec_find_decoder(audioStream.codecpar().codec_id());
            if (codec == null) {
                throw new IOException("Unsupported audio codec");
            }

            // Step 5: Allocate codec context
            codecContext = avcodec_alloc_context3(codec);
            if (avcodec_parameters_to_context(codecContext, audioStream.codecpar()) < 0) {
                throw new IOException("Could not copy codec parameters to context");
            }

            // Step 6: Open codec
            if (avcodec_open2(codecContext, codec, (PointerPointer<?>) null) < 0) {
                throw new IOException("Could not open codec");
            }

            // Preserve original sample rate and channels
            int outputSampleRate = codecContext.sample_rate();
            int outputChannels = codecContext.channels();

            LOGGER.debug("Input audio: {} Hz, {} channels, format: {}",
                    outputSampleRate, outputChannels, codecContext.sample_fmt());

            // Step 7: Setup resampler to convert to 16-bit PCM (preserve sample rate and channels)
            swrContext = swr_alloc_set_opts(
                    null,
                    av_get_default_channel_layout(outputChannels),   // Output channel layout (preserve)
                    TARGET_SAMPLE_FORMAT,                            // Output sample format (16-bit PCM)
                    outputSampleRate,                                // Output sample rate (preserve)
                    av_get_default_channel_layout(codecContext.channels()), // Input channel layout
                    codecContext.sample_fmt(),                       // Input sample format
                    codecContext.sample_rate(),                      // Input sample rate
                    0, null
            );

            if (swr_init(swrContext) < 0) {
                throw new IOException("Could not initialize resampler");
            }

            // Step 8: Allocate frames and packet
            frame = av_frame_alloc();
            resampledFrame = av_frame_alloc();
            packet = av_packet_alloc();

            // Configure resampled frame (preserves source sample rate and channels)
            resampledFrame.format(TARGET_SAMPLE_FORMAT);
            resampledFrame.channel_layout(av_get_default_channel_layout(outputChannels));
            resampledFrame.sample_rate(outputSampleRate);
            resampledFrame.nb_samples(outputSampleRate); // 1 second buffer

            av_frame_get_buffer(resampledFrame, 0);

            // Step 9: Read and decode all frames
            List<short[]> audioData = new ArrayList<>();
            int totalSamples = 0;

            while (av_read_frame(formatContext, packet) >= 0) {
                if (packet.stream_index() == audioStreamIndex) {
                    // Send packet to decoder
                    int ret = avcodec_send_packet(codecContext, packet);
                    if (ret < 0) {
                        LOGGER.warn("Error sending packet to decoder");
                        av_packet_unref(packet);
                        continue;
                    }

                    // Receive decoded frames
                    while (ret >= 0) {
                        ret = avcodec_receive_frame(codecContext, frame);
                        if (ret == AVERROR_EAGAIN() || ret == AVERROR_EOF) {
                            break;
                        } else if (ret < 0) {
                            throw new IOException("Error during decoding");
                        }

                        // Convert to 16-bit PCM (preserve sample rate and channels)
                        int outSamples = swr_convert(
                                swrContext,
                                resampledFrame.data(),
                                resampledFrame.nb_samples(),
                                frame.data(),
                                frame.nb_samples()
                        );

                        if (outSamples > 0) {
                            // Convert to short array (16-bit PCM samples)
                            BytePointer dataPointer = resampledFrame.data(0);
                            short[] samples = new short[outSamples];
                            byte[] bytes = new byte[outSamples * 2];
                            dataPointer.position(0).get(bytes);
                            // Use ByteBuffer for proper little-endian conversion
                            ByteBuffer.wrap(bytes)
                                    .order(ByteOrder.LITTLE_ENDIAN)
                                    .asShortBuffer()
                                    .get(samples);
                            audioData.add(samples);
                            totalSamples += outSamples;
                        }

                        av_frame_unref(frame);
                    }
                }
                av_packet_unref(packet);
            }

            // Flush decoder
            avcodec_send_packet(codecContext, null);
            while (avcodec_receive_frame(codecContext, frame) >= 0) {
                int outSamples = swr_convert(
                        swrContext,
                        resampledFrame.data(),
                        resampledFrame.nb_samples(),
                        frame.data(),
                        frame.nb_samples()
                );

                if (outSamples > 0) {
                    BytePointer dataPointer = resampledFrame.data(0);
                    short[] samples = new short[outSamples];
                    byte[] bytes = new byte[outSamples * 2];
                    dataPointer.position(0).get(bytes);
                    // Use ByteBuffer for proper little-endian conversion
                    ByteBuffer.wrap(bytes)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .asShortBuffer()
                            .get(samples);
                    audioData.add(samples);
                    totalSamples += outSamples;
                }

                av_frame_unref(frame);
            }

            LOGGER.debug("Decoded {} total samples", totalSamples);

            // Step 10: Combine all audio data
            short[] allSamples = new short[totalSamples];
            int offset = 0;
            for (short[] chunk : audioData) {
                System.arraycopy(chunk, 0, allSamples, offset, chunk.length);
                offset += chunk.length;
            }

            // Step 11: Convert to bytes (little-endian)
            byte[] audioBytes = new byte[allSamples.length * 2];
            ByteBuffer.wrap(audioBytes)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .put(allSamples);

            // Step 12: Write WAV file using Java Sound API (preserves original sample rate and channels)
            AudioFormat audioFormat = new AudioFormat(
                    outputSampleRate,        // Sample rate (preserved from source)
                    16,                      // Sample size in bits
                    outputChannels,          // Channels (preserved from source)
                    true,                    // Signed
                    false                    // Little-endian
            );

            try (AudioInputStream audioInputStream = new AudioInputStream(
                    new ByteArrayInputStream(audioBytes),
                    audioFormat,
                    allSamples.length
            )) {
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(outputPath));
            }

            LOGGER.debug("Successfully converted {} to WAV: {} Hz, {} channels",
                    inputPath, outputSampleRate, outputChannels);

        } catch (Exception e) {
            throw new IOException("Failed to convert audio file using ByteDeco FFmpeg: " + e.getMessage(), e);
        } finally {
            // Step 13: Clean up all resources
            if (packet != null) {
                av_packet_free(packet);
            }
            if (frame != null) {
                av_frame_free(frame);
            }
            if (resampledFrame != null) {
                av_frame_free(resampledFrame);
            }
            if (swrContext != null) {
                swr_free(swrContext);
            }
            if (codecContext != null) {
                avcodec_free_context(codecContext);
            }
            if (formatContext != null) {
                avformat_close_input(formatContext);
            }
        }
    }
}