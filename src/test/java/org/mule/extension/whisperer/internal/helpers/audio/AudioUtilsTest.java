package org.mule.extension.whisperer.internal.helpers.audio;

import org.junit.jupiter.api.Test;
import org.mule.runtime.api.metadata.MediaType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AudioUtils utility methods.
 * Tests MediaType detection and audio format guessing.
 */
class AudioUtilsTest {

    // ========================================
    // isWav() Tests
    // ========================================

    @Test
    void testIsWav_AudioWav() {
        MediaType mediaType = MediaType.parse("audio/wav");
        assertTrue(AudioUtils.isWav(mediaType), "audio/wav should be recognized as WAV");
    }

    @Test
    void testIsWav_AudioVndWav() {
        MediaType mediaType = MediaType.parse("audio/vnd.wav");
        assertTrue(AudioUtils.isWav(mediaType), "audio/vnd.wav should be recognized as WAV");
    }

    @Test
    void testIsWav_AudioVndWave() {
        MediaType mediaType = MediaType.parse("audio/vnd.wave");
        assertTrue(AudioUtils.isWav(mediaType), "audio/vnd.wave should be recognized as WAV");
    }

    @Test
    void testIsWav_AudioWave() {
        MediaType mediaType = MediaType.parse("audio/wave");
        assertTrue(AudioUtils.isWav(mediaType), "audio/wave should be recognized as WAV");
    }

    @Test
    void testIsWav_AudioXWav() {
        MediaType mediaType = MediaType.parse("audio/x-wav");
        assertTrue(AudioUtils.isWav(mediaType), "audio/x-wav should be recognized as WAV");
    }

    @Test
    void testIsWav_AudioXPnWav() {
        MediaType mediaType = MediaType.parse("audio/x-pn-wav");
        assertTrue(AudioUtils.isWav(mediaType), "audio/x-pn-wav should be recognized as WAV");
    }

    @Test
    void testIsWav_NotWav() {
        MediaType mediaType = MediaType.parse("audio/mp3");
        assertFalse(AudioUtils.isWav(mediaType), "audio/mp3 should NOT be recognized as WAV");
    }

    @Test
    void testIsWav_WithParameters() {
        MediaType mediaType = MediaType.parse("audio/wav; charset=UTF-8");
        assertTrue(AudioUtils.isWav(mediaType), "audio/wav with parameters should be recognized as WAV");
    }

    // ========================================
    // guessAudioFormat() Tests
    // ========================================

    @Test
    void testGuessAudioFormat_M4A() {
        assertEquals("m4a", AudioUtils.guessAudioFormat(MediaType.parse("audio/m4a")));
        assertEquals("m4a", AudioUtils.guessAudioFormat(MediaType.parse("audio/x-m4a")));
        assertEquals("m4a", AudioUtils.guessAudioFormat(MediaType.parse("audio/mp4")));
    }

    @Test
    void testGuessAudioFormat_FLAC() {
        assertEquals("flac", AudioUtils.guessAudioFormat(MediaType.parse("audio/flac")));
        assertEquals("flac", AudioUtils.guessAudioFormat(MediaType.parse("audio/x-flac")));
    }

    @Test
    void testGuessAudioFormat_WAV() {
        assertEquals("wav", AudioUtils.guessAudioFormat(MediaType.parse("audio/wav")));
        assertEquals("wav", AudioUtils.guessAudioFormat(MediaType.parse("audio/vnd.wav")));
        assertEquals("wav", AudioUtils.guessAudioFormat(MediaType.parse("audio/vnd.wave")));
        assertEquals("wav", AudioUtils.guessAudioFormat(MediaType.parse("audio/wave")));
        assertEquals("wav", AudioUtils.guessAudioFormat(MediaType.parse("audio/x-wav")));
        assertEquals("wav", AudioUtils.guessAudioFormat(MediaType.parse("audio/x-pn-wav")));
    }

    @Test
    void testGuessAudioFormat_OGG() {
        assertEquals("ogg", AudioUtils.guessAudioFormat(MediaType.parse("audio/ogg")));
    }

    @Test
    void testGuessAudioFormat_WEBM() {
        assertEquals("webm", AudioUtils.guessAudioFormat(MediaType.parse("audio/webm")));
    }

    @Test
    void testGuessAudioFormat_AAC() {
        assertEquals("aac", AudioUtils.guessAudioFormat(MediaType.parse("audio/aac")));
    }

    @Test
    void testGuessAudioFormat_MP3() {
        assertEquals("mp3", AudioUtils.guessAudioFormat(MediaType.parse("audio/mp3")));
        assertEquals("mp3", AudioUtils.guessAudioFormat(MediaType.parse("audio/mpeg")));
    }

    @Test
    void testGuessAudioFormat_Unknown() {
        assertNull(AudioUtils.guessAudioFormat(MediaType.parse("audio/unknown")));
        assertNull(AudioUtils.guessAudioFormat(MediaType.parse("video/mp4")));
    }

    @Test
    void testGuessAudioFormat_WithParameters() {
        assertEquals("mp3", AudioUtils.guessAudioFormat(MediaType.parse("audio/mpeg; charset=UTF-8")));
    }

    // ========================================
    // guessAudioFileExtension() Tests
    // ========================================

    @Test
    void testGuessAudioFileExtension_KnownFormat() {
        assertEquals("mp3", AudioUtils.guessAudioFileExtension(MediaType.parse("audio/mp3")));
        assertEquals("m4a", AudioUtils.guessAudioFileExtension(MediaType.parse("audio/m4a")));
        assertEquals("wav", AudioUtils.guessAudioFileExtension(MediaType.parse("audio/wav")));
    }

    @Test
    void testGuessAudioFileExtension_UnknownFormat() {
        assertEquals("unknown", AudioUtils.guessAudioFileExtension(MediaType.parse("audio/xyz")));
        assertEquals("unknown", AudioUtils.guessAudioFileExtension(MediaType.parse("video/mp4")));
    }

    @Test
    void testGuessAudioFileExtension_WithParameters() {
        assertEquals("flac", AudioUtils.guessAudioFileExtension(MediaType.parse("audio/flac; rate=44100")));
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    void testAllFormats_CaseInsensitive() {
        // MediaType.parse() handles case sensitivity
        // This test ensures our switch statements work with standard MediaType formatting
        MediaType mp3Upper = MediaType.parse("audio/MP3");
        // MediaType normalizes to lowercase
        assertEquals("mp3", AudioUtils.guessAudioFormat(mp3Upper));
    }
}