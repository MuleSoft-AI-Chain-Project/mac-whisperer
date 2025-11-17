package org.mule.extension.whisperer.internal.helpers.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for audio conversion using ByteDeco FFmpeg architecture.
 *
 * Tests all supported formats:
 * - Core formats (always available): MP3, WAV
 * - Extended formats (require ByteDeco): M4A, AAC, FLAC, OGG, WEBM
 */
class AudioConverterTest {

    @TempDir
    Path tempDir;

    private File outputWavFile;

    @BeforeEach
    void setUp() {
        outputWavFile = tempDir.resolve("output.wav").toFile();
    }

    // ========================================
    // Core Format Tests (Always Available)
    // ========================================

    @Test
    void testConvertWav_ShouldJustCopy() throws IOException, javax.sound.sampled.UnsupportedAudioFileException {
        // Given: A WAV file
        File inputFile = getTestResourceFile("speech-sample-1.wav");

        // When: Converting WAV to WAV
        AudioConverter.convertToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath(), "wav");

        // Then: Output file exists and is valid
        assertTrue(outputWavFile.exists(), "Output WAV file should exist");
        assertTrue(outputWavFile.length() > 0, "Output WAV file should not be empty");

        // Verify audio format
        AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(outputWavFile);
        AudioFormat audioFormat = audioFileFormat.getFormat();

        assertEquals(AudioFileFormat.Type.WAVE, audioFileFormat.getType());
        assertTrue(audioFormat.getSampleRate() > 0);
    }

    @Test
    void testConvertMp3_UsingJLayer() throws IOException, javax.sound.sampled.UnsupportedAudioFileException {
        // Given: An MP3 file
        File inputFile = getTestResourceFile("speech-sample-3.mp3");

        // When: Converting MP3 to WAV using JLayer (pure Java)
        AudioConverter.convertToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath(), "mp3");

        // Then: Output file exists and is valid WAV (preserves source sample rate/channels)
        assertTrue(outputWavFile.exists(), "Output WAV file should exist");
        assertTrue(outputWavFile.length() > 0, "Output WAV file should not be empty");

        // Verify audio format
        AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(outputWavFile);
        AudioFormat audioFormat = audioFileFormat.getFormat();

        assertEquals(AudioFileFormat.Type.WAVE, audioFileFormat.getType());
        assertTrue(audioFormat.getSampleRate() > 0, "Should have valid sample rate");
        assertTrue(audioFormat.getChannels() > 0, "Should have valid channel count");
        assertEquals(16, audioFormat.getSampleSizeInBits(), "Should be 16-bit");
    }

    // ========================================
    // Extended Format Tests (Require ByteDeco)
    // ========================================

    @Test
    void testConvertM4A_UsingByteDeco() throws IOException, javax.sound.sampled.UnsupportedAudioFileException {
        // Given: ByteDeco FFmpeg is available
        if (!AudioConverter.isByteDecoAvailable()) {
            System.out.println("⚠️  Skipping M4A test - ByteDeco FFmpeg not available");
            System.out.println("   Add dependency: org.bytedeco:ffmpeg-platform:6.1.1-1.5.10");
            return; // Skip test if ByteDeco not available
        }

        // Given: An M4A file
        File inputFile = getTestResourceFile("speech-sample-2.m4a");

        // When: Converting M4A to WAV using ByteDeco FFmpeg
        AudioConverter.convertToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath(), "m4a");

        // Then: Output file exists and is valid WAV (preserves source sample rate/channels)
        assertTrue(outputWavFile.exists(), "Output WAV file should exist");
        assertTrue(outputWavFile.length() > 0, "Output WAV file should not be empty");

        // Verify audio format
        AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(outputWavFile);
        AudioFormat audioFormat = audioFileFormat.getFormat();

        assertEquals(AudioFileFormat.Type.WAVE, audioFileFormat.getType());
        assertTrue(audioFormat.getSampleRate() > 0, "Should have valid sample rate");
        assertTrue(audioFormat.getChannels() > 0, "Should have valid channel count");
        assertEquals(16, audioFormat.getSampleSizeInBits(), "Should be 16-bit");
    }

    @Test
    void testConvertFLAC_UsingByteDeco() throws IOException, javax.sound.sampled.UnsupportedAudioFileException {
        // Given: ByteDeco FFmpeg is available
        if (!AudioConverter.isByteDecoAvailable()) {
            System.out.println("⚠️  Skipping FLAC test - ByteDeco FFmpeg not available");
            return;
        }

        // Given: A FLAC file
        File inputFile = getTestResourceFile("speech-sample-4.flac");

        // When: Converting FLAC to WAV
        AudioConverter.convertToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath(), "flac");

        // Then: Output file exists and is valid WAV
        assertTrue(outputWavFile.exists(), "Output WAV file should exist");
        assertTrue(outputWavFile.length() > 0, "Output WAV file should not be empty");

        // Verify audio format
        AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(outputWavFile);
        assertEquals(AudioFileFormat.Type.WAVE, audioFileFormat.getType());
    }

    @Test
    void testConvertOGG_UsingByteDeco() throws IOException, javax.sound.sampled.UnsupportedAudioFileException {
        // Given: ByteDeco FFmpeg is available
        if (!AudioConverter.isByteDecoAvailable()) {
            System.out.println("⚠️  Skipping OGG test - ByteDeco FFmpeg not available");
            return;
        }

        // Given: An OGG file
        File inputFile = getTestResourceFile("speech-sample-5.ogg");

        // When: Converting OGG to WAV
        AudioConverter.convertToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath(), "ogg");

        // Then: Output file exists and is valid WAV
        assertTrue(outputWavFile.exists(), "Output WAV file should exist");
        assertTrue(outputWavFile.length() > 0, "Output WAV file should not be empty");

        // Verify audio format
        AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(outputWavFile);
        assertEquals(AudioFileFormat.Type.WAVE, audioFileFormat.getType());
    }

    @Test
    void testConvertWEBM_UsingByteDeco() throws IOException, javax.sound.sampled.UnsupportedAudioFileException {
        // Given: ByteDeco FFmpeg is available
        if (!AudioConverter.isByteDecoAvailable()) {
            System.out.println("⚠️  Skipping WEBM test - ByteDeco FFmpeg not available");
            return;
        }

        // Given: A WEBM file
        File inputFile = getTestResourceFile("speech-sample-6.webm");

        // When: Converting WEBM to WAV
        AudioConverter.convertToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath(), "webm");

        // Then: Output file exists and is valid WAV
        assertTrue(outputWavFile.exists(), "Output WAV file should exist");
        assertTrue(outputWavFile.length() > 0, "Output WAV file should not be empty");

        // Verify audio format
        AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(outputWavFile);
        assertEquals(AudioFileFormat.Type.WAVE, audioFileFormat.getType());
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    void testConvertM4A_WithoutByteDeco_ShouldThrowException() {
        // Given: ByteDeco is NOT available (simulated by checking)
        if (AudioConverter.isByteDecoAvailable()) {
            // Can't test this scenario when ByteDeco is actually available
            System.out.println("⚠️  Skipping 'missing ByteDeco' test - ByteDeco is available");
            return;
        }

        // Given: An M4A file
        File inputFile = getTestResourceFile("speech-sample-2.m4a");

        // When/Then: Should throw UnsupportedOperationException with helpful message
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> AudioConverter.convertToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath(), "m4a")
        );

        // Verify error message guides user to add dependency
        String message = exception.getMessage();
        assertTrue(message.contains("ByteDeco FFmpeg"), "Error should mention ByteDeco FFmpeg");
        assertTrue(message.contains("org.bytedeco"), "Error should show groupId");
        assertTrue(message.contains("ffmpeg-platform"), "Error should show artifactId");
        assertTrue(message.contains("6.1.1-1.5.10"), "Error should show version");
    }

    @Test
    void testConvertUnsupportedFormat_ShouldThrowException() {
        // When/Then: Should throw UnsupportedOperationException
        File fakeFile = new File("/fake/path/audio.xyz");

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> AudioConverter.convertToWav(fakeFile.getAbsolutePath(), outputWavFile.getAbsolutePath(), "xyz")
        );

        // Verify error message lists supported formats
        String message = exception.getMessage();
        assertTrue(message.contains("MP3"), "Should list MP3");
        assertTrue(message.contains("M4A"), "Should list M4A");
        assertTrue(message.contains("WAV"), "Should list WAV");
        assertTrue(message.contains("FLAC"), "Should list FLAC");
    }

    @Test
    void testConvertNonExistentFile_ShouldThrowIOException() {
        // Given: A non-existent file
        File nonExistentFile = new File("/fake/path/nonexistent.mp3");

        // When/Then: Should throw IOException
        assertThrows(
                IOException.class,
                () -> AudioConverter.convertToWav(nonExistentFile.getAbsolutePath(), outputWavFile.getAbsolutePath(), "mp3")
        );
    }

    // ========================================
    // Availability Detection Tests
    // ========================================

    @Test
    void testByteDecoAvailability() {
        // When: Checking ByteDeco availability
        boolean available = AudioConverter.isByteDecoAvailable();

        // Then: Should return consistent result
        System.out.println("ByteDeco FFmpeg available: " + available);

        if (available) {
            System.out.println("✅ ByteDeco FFmpeg detected - All formats supported");
        } else {
            System.out.println("⚠️  ByteDeco FFmpeg not detected - Extended formats not available");
            System.out.println("   Core formats (MP3, WAV) still work via pure Java");
        }

        // Verify detection is deterministic
        assertEquals(available, AudioConverter.isByteDecoAvailable(),
                "ByteDeco availability should be consistent");
    }

    // ========================================
    // Case Sensitivity Tests
    // ========================================

    @Test
    void testConvertMp3_UpperCase() throws IOException {
        // Given: An MP3 file
        File inputFile = getTestResourceFile("speech-sample-3.mp3");

        // When: Using uppercase format
        AudioConverter.convertToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath(), "MP3");

        // Then: Should work (format is normalized)
        assertTrue(outputWavFile.exists());
        assertTrue(outputWavFile.length() > 0);
    }

    @Test
    void testConvertMp3_MixedCase() throws IOException {
        // Given: An MP3 file
        File inputFile = getTestResourceFile("speech-sample-3.mp3");

        // When: Using mixed case format
        AudioConverter.convertToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath(), "Mp3");

        // Then: Should work (format is normalized)
        assertTrue(outputWavFile.exists());
        assertTrue(outputWavFile.length() > 0);
    }

    // ========================================
    // Helper Methods
    // ========================================

    private File getTestResourceFile(String filename) {
        String resourcePath = "src/test/resources/" + filename;
        File file = new File(resourcePath);
        assertTrue(file.exists(), "Test resource file should exist: " + resourcePath);
        return file;
    }
}