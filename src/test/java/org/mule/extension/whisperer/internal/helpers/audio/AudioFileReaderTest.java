package org.mule.extension.whisperer.internal.helpers.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AudioFileReader and Mp3ToWavConverter.
 * Tests audio file reading, format conversion, and sample extraction.
 */
class AudioFileReaderTest {

    @TempDir
    Path tempDir;

    private File outputWavFile;

    @BeforeEach
    void setUp() {
        outputWavFile = tempDir.resolve("output.wav").toFile();
    }

    // ========================================
    // readFile() Tests
    // ========================================

    @Test
    void testReadFile_ValidWav16kHzMono() throws IOException, UnsupportedAudioFileException {
        // Given: A valid 16kHz mono WAV file
        File inputFile = getTestResourceFile("speech-sample-1.wav");

        // When: Reading the file
        float[] samples = AudioFileReader.readFile(inputFile);

        // Then: Should return samples
        assertNotNull(samples, "Samples should not be null");
        assertTrue(samples.length > 0, "Should have audio samples");

        // Verify samples are in valid range [-1.0, 1.0]
        for (float sample : samples) {
            assertTrue(sample >= -1.0f && sample <= 1.0f,
                "Sample should be in range [-1.0, 1.0], got: " + sample);
        }
    }

    @Test
    void testReadFile_NonExistent() {
        // Given: A non-existent file
        File nonExistentFile = new File("/fake/path/nonexistent.wav");

        // When/Then: Should throw IOException
        assertThrows(IOException.class, () -> AudioFileReader.readFile(nonExistentFile));
    }

    // ========================================
    // convertMp3ToWav() Tests
    // ========================================

    @Test
    void testConvertMp3ToWav_ValidMp3() throws IOException, UnsupportedAudioFileException {
        // Given: A valid MP3 file
        File inputFile = getTestResourceFile("speech-sample-3.mp3");

        // When: Converting MP3 to WAV
        AudioFileReader.convertMp3ToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath());

        // Then: Output file should exist and be valid WAV
        assertTrue(outputWavFile.exists(), "Output WAV file should exist");
        assertTrue(outputWavFile.length() > 0, "Output WAV file should not be empty");

        // Verify audio format
        AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(outputWavFile);
        assertEquals(AudioFileFormat.Type.WAVE, audioFileFormat.getType());
    }

    @Test
    void testConvertMp3ToWav_NonExistent() {
        // Given: A non-existent MP3 file
        String nonExistentFile = "/fake/path/nonexistent.mp3";

        // When/Then: Should throw IOException
        assertThrows(IOException.class,
            () -> AudioFileReader.convertMp3ToWav(nonExistentFile, outputWavFile.getAbsolutePath()));
    }

    // ========================================
    // convertM4AToWav() Tests
    // ========================================

    @Test
    void testConvertM4AToWav_WithByteDeco() throws IOException {
        // Given: ByteDeco is available
        if (!AudioConverter.isByteDecoAvailable()) {
            System.out.println("⚠️  Skipping M4A conversion test - ByteDeco FFmpeg not available");
            return;
        }

        // Given: A valid M4A file
        File inputFile = getTestResourceFile("speech-sample-2.m4a");

        // When: Converting M4A to WAV
        AudioFileReader.convertM4AToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath());

        // Then: Output file should exist and be valid WAV
        assertTrue(outputWavFile.exists(), "Output WAV file should exist");
        assertTrue(outputWavFile.length() > 0, "Output WAV file should not be empty");
    }

    @Test
    void testConvertM4AToWav_WithoutByteDeco() {
        // Given: ByteDeco is NOT available
        if (AudioConverter.isByteDecoAvailable()) {
            System.out.println("⚠️  Skipping 'missing ByteDeco' test - ByteDeco is available");
            return;
        }

        // Given: An M4A file
        File inputFile = getTestResourceFile("speech-sample-2.m4a");

        // When/Then: Should throw IOException wrapping UnsupportedOperationException
        IOException exception = assertThrows(IOException.class,
            () -> AudioFileReader.convertM4AToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath()));

        // Verify error message indicates missing dependency
        assertTrue(exception.getMessage().contains("Failed to convert M4A to WAV"));
    }

    // ========================================
    // convertAudioToWav() Generic Tests
    // ========================================

    @Test
    void testConvertAudioToWav_Mp3() throws IOException {
        // Given: A valid MP3 file
        File inputFile = getTestResourceFile("speech-sample-3.mp3");

        // When: Converting using generic method
        AudioFileReader.convertAudioToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath());

        // Then: Output file should exist
        assertTrue(outputWavFile.exists(), "Output WAV file should exist");
        assertTrue(outputWavFile.length() > 0, "Output WAV file should not be empty");
    }

    @Test
    void testConvertAudioToWav_Wav() throws IOException {
        // Given: A valid WAV file
        File inputFile = getTestResourceFile("speech-sample-1.wav");

        // When: Converting WAV (should just copy)
        AudioFileReader.convertAudioToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath());

        // Then: Output file should exist
        assertTrue(outputWavFile.exists(), "Output WAV file should exist");
        assertTrue(outputWavFile.length() > 0, "Output WAV file should not be empty");
    }

    @Test
    void testConvertAudioToWav_M4A_WithByteDeco() throws IOException {
        // Given: ByteDeco is available
        if (!AudioConverter.isByteDecoAvailable()) {
            System.out.println("⚠️  Skipping M4A generic conversion test - ByteDeco FFmpeg not available");
            return;
        }

        // Given: A valid M4A file
        File inputFile = getTestResourceFile("speech-sample-2.m4a");

        // When: Converting using generic method
        AudioFileReader.convertAudioToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath());

        // Then: Output file should exist
        assertTrue(outputWavFile.exists(), "Output WAV file should exist");
        assertTrue(outputWavFile.length() > 0, "Output WAV file should not be empty");
    }

    @Test
    void testConvertAudioToWav_UnsupportedFormat() {
        // Given: An unsupported format
        String fakeFile = tempDir.resolve("fake.xyz").toString();

        // When/Then: Should throw IOException
        IOException exception = assertThrows(IOException.class,
            () -> AudioFileReader.convertAudioToWav(fakeFile, outputWavFile.getAbsolutePath()));

        assertTrue(exception.getMessage().contains("Failed to convert"));
    }

    @Test
    void testConvertAudioToWav_NonExistentFile() {
        // Given: A non-existent file
        String nonExistentFile = "/fake/path/nonexistent.mp3";

        // When/Then: Should throw IOException
        assertThrows(IOException.class,
            () -> AudioFileReader.convertAudioToWav(nonExistentFile, outputWavFile.getAbsolutePath()));
    }

    // ========================================
    // convertToMono16kHz() Tests
    // ========================================

    @Test
    void testConvertToMono16kHz_ValidWav() throws IOException, UnsupportedAudioFileException {
        // Given: A valid WAV file (may not be 16kHz mono)
        File inputFile = getTestResourceFile("speech-sample-1.wav");

        // When: Converting to 16kHz mono
        File convertedFile = AudioFileReader.convertToMono16kHz(inputFile);

        // Then: Converted file should exist
        assertTrue(convertedFile.exists(), "Converted file should exist");
        assertTrue(convertedFile.length() > 0, "Converted file should not be empty");

        // Verify audio format
        AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(convertedFile);
        AudioFormat audioFormat = audioFileFormat.getFormat();

        assertEquals(AudioFileFormat.Type.WAVE, audioFileFormat.getType());
        assertEquals(16000, (int) audioFormat.getSampleRate(), "Should be 16kHz");
        assertEquals(1, audioFormat.getChannels(), "Should be mono");
        assertEquals(16, audioFormat.getSampleSizeInBits(), "Should be 16-bit");

        // Cleanup
        convertedFile.delete();
    }

    // ========================================
    // Mp3ToWavConverter Tests
    // ========================================

    @Test
    void testMp3ToWavConverter_ValidMp3() throws IOException, UnsupportedAudioFileException {
        // Given: A valid MP3 file
        File inputFile = getTestResourceFile("speech-sample-3.mp3");

        // When: Converting using Mp3ToWavConverter directly
        Mp3ToWavConverter.convertMp3ToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath());

        // Then: Output file should exist and be valid WAV
        assertTrue(outputWavFile.exists(), "Output WAV file should exist");
        assertTrue(outputWavFile.length() > 0, "Output WAV file should not be empty");

        // Verify it's a proper WAV file
        AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(outputWavFile);
        assertEquals(AudioFileFormat.Type.WAVE, audioFileFormat.getType());

        AudioFormat format = audioFileFormat.getFormat();
        assertTrue(format.getSampleRate() > 0, "Sample rate should be positive");
        assertEquals(16, format.getSampleSizeInBits(), "Should be 16-bit");
        assertTrue(format.isBigEndian() == false, "Should be little-endian");
    }

    @Test
    void testMp3ToWavConverter_NonExistentFile() {
        // Given: A non-existent MP3 file
        String nonExistentFile = "/fake/path/nonexistent.mp3";

        // When/Then: Should throw IOException
        assertThrows(IOException.class,
            () -> Mp3ToWavConverter.convertMp3ToWav(nonExistentFile, outputWavFile.getAbsolutePath()));
    }

    // ========================================
    // Integration Tests
    // ========================================

    @Test
    void testReadFile_AfterMp3Conversion() throws IOException, UnsupportedAudioFileException {
        // Given: Convert MP3 to WAV first
        File inputFile = getTestResourceFile("speech-sample-3.mp3");
        AudioFileReader.convertMp3ToWav(inputFile.getAbsolutePath(), outputWavFile.getAbsolutePath());

        // When: Reading the converted WAV file
        float[] samples = AudioFileReader.readFile(outputWavFile);

        // Then: Should successfully read samples
        assertNotNull(samples);
        assertTrue(samples.length > 0);

        // Verify samples are normalized
        for (float sample : samples) {
            assertTrue(sample >= -1.0f && sample <= 1.0f);
        }
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