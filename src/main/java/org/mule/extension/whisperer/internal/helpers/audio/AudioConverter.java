package org.mule.extension.whisperer.internal.helpers.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Orchestrates audio format conversion using pure Java decoders with optional ByteDeco FFmpeg.
 *
 * <p>Core Formats (Always Available):
 * <ul>
 *   <li>MP3 - Pure Java (JLayer)</li>
 *   <li>WAV - No conversion needed</li>
 * </ul>
 *
 * <p>Extended Formats (Requires ByteDeco FFmpeg):
 * <ul>
 *   <li>M4A/AAC - Requires ByteDeco</li>
 *   <li>FLAC - Requires ByteDeco</li>
 *   <li>OGG - Requires ByteDeco</li>
 *   <li>WEBM - Requires ByteDeco</li>
 * </ul>
 */
public class AudioConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioConverter.class);

    private static final boolean BYTEDECO_AVAILABLE;

    static {
        // Detect ByteDeco FFmpeg availability at static initialization time
        boolean bytedecoFound = false;
        try {
            Class.forName("org.bytedeco.ffmpeg.global.avcodec");
            bytedecoFound = true;
            LOGGER.info("ByteDeco FFmpeg detected - Extended format support (M4A/AAC/FLAC/OGG/WEBM) available");
        } catch (ClassNotFoundException e) {
            LOGGER.debug("ByteDeco FFmpeg not found - Extended formats (M4A/AAC/FLAC/OGG/WEBM) will not be available");
        }
        BYTEDECO_AVAILABLE = bytedecoFound;
    }

    /**
     * Checks if ByteDeco FFmpeg is available for extended format conversion.
     *
     * @return true if ByteDeco FFmpeg is in the classpath
     */
    public static boolean isByteDecoAvailable() {
        return BYTEDECO_AVAILABLE;
    }

    /**
     * Converts an audio file to WAV format.
     * NOTE: Output sample rate and channels are preserved from the source file.
     * Use AudioFileReader.readFile() for automatic resampling to 16kHz mono (required by WhisperJNI).
     *
     * @param inputPath Path to the input audio file
     * @param outputPath Path where the WAV file should be written
     * @param format Audio format (mp3, m4a, aac, flac, ogg, webm, wav)
     * @throws IOException if conversion fails
     * @throws UnsupportedOperationException if the format requires ByteDeco but it's not available
     */
    public static void convertToWav(String inputPath, String outputPath, String format) throws IOException {
        String normalizedFormat = format.toLowerCase();

        LOGGER.debug("Converting {} file to WAV: {} -> {}", normalizedFormat, inputPath, outputPath);

        switch (normalizedFormat) {
            case "wav":
                // No conversion needed, just copy
                java.nio.file.Files.copy(
                    java.nio.file.Paths.get(inputPath),
                    java.nio.file.Paths.get(outputPath),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                LOGGER.debug("WAV file copied without conversion");
                break;

            case "mp3":
                // Use JLayer (always available) - preserves original sample rate
                try {
                    Mp3ToWavConverter.convertMp3ToWav(inputPath, outputPath);
                    LOGGER.debug("MP3 conversion completed using JLayer");
                } catch (javax.sound.sampled.UnsupportedAudioFileException e) {
                    throw new IOException("MP3 file format not supported", e);
                }
                break;

            case "m4a":
            case "aac":
            case "mp4":
                // M4A/AAC requires ByteDeco FFmpeg
                if (BYTEDECO_AVAILABLE) {
                    ByteDecoConverter.convertToWav(inputPath, outputPath);
                    LOGGER.debug("{} conversion completed using ByteDeco", normalizedFormat.toUpperCase());
                } else {
                    throw new UnsupportedOperationException(getExtendedFormatMissingDependencyMessage(normalizedFormat));
                }
                break;

            case "flac":
            case "ogg":
            case "webm":
                // These require ByteDeco
                if (BYTEDECO_AVAILABLE) {
                    ByteDecoConverter.convertToWav(inputPath, outputPath);
                    LOGGER.debug("{} conversion completed using ByteDeco", normalizedFormat.toUpperCase());
                } else {
                    throw new UnsupportedOperationException(getExtendedFormatMissingDependencyMessage(normalizedFormat));
                }
                break;

            default:
                throw new UnsupportedOperationException(
                    "Unsupported audio format: " + format + ". " +
                    "Supported formats: MP3, M4A, AAC, WAV (core) and FLAC, OGG, WEBM (with ByteDeco FFmpeg)"
                );
        }
    }

    private static String getExtendedFormatMissingDependencyMessage(String format) {
        return format.toUpperCase() + " format requires ByteDeco FFmpeg. Add this dependency to your Mule app pom.xml:\n" +
               "<dependency>\n" +
               "  <groupId>org.bytedeco</groupId>\n" +
               "  <artifactId>ffmpeg-platform</artifactId>\n" +
               "  <version>6.1.1-1.5.10</version>\n" +
               "</dependency>\n\n" +
               "Note: This adds ~150MB of native binaries. For smaller size, use platform-specific artifact:\n" +
               "  - ffmpeg (classifier: macosx-arm64) - ~30MB for Mac M1/M2/M3\n" +
               "  - ffmpeg (classifier: macosx-x86_64) - ~30MB for Intel Mac\n" +
               "  - ffmpeg (classifier: linux-x86_64) - ~30MB for Linux\n" +
               "  - ffmpeg (classifier: windows-x86_64) - ~30MB for Windows\n\n" +
               "Core formats (MP3, M4A, WAV) work without this dependency.\n" +
               "See: https://mac-project.ai/docs/mac-whisperer/audio-formats";
    }
}