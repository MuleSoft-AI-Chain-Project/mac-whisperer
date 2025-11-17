package org.mule.extension.whisperer.internal.helpers.audio;

import org.mule.runtime.api.metadata.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioUtils.class);

    public static boolean isWav(MediaType mediaType) {

        LOGGER.debug(String.format("MediaType: %s", mediaType.withoutParameters().toString()));

        switch (mediaType.withoutParameters().toString()) {
            case "audio/wav":
            case "audio/vnd.wav":
            case "audio/vnd.wave":
            case "audio/wave":
            case "audio/x-wav":
            case "audio/x-pn-wav":
                return true;
        }
        return false;
    }

    public static String guessAudioFileExtension(MediaType mediaType) {
        String audioType = guessAudioFormat(mediaType);
        return audioType != null ? audioType : "unknown";
    }

    public static String guessAudioFormat(MediaType mediaType) {
        String extension = null;
        switch (mediaType.withoutParameters().toString()) {
            case "audio/m4a":
            case "audio/x-m4a":
            case "audio/mp4":
                extension = "m4a";
                break;
            case "audio/flac":
            case "audio/x-flac":
                extension = "flac";
                break;
            case "audio/wav":
            case "audio/vnd.wav":
            case "audio/vnd.wave":
            case "audio/wave":
            case "audio/x-wav":
            case "audio/x-pn-wav":
                extension = "wav";
                break;
            case "audio/ogg":
                extension = "ogg";
                break;
            case "audio/webm":
                extension = "webm";
                break;
            case "audio/aac":
                extension = "aac";
                break;
            case "audio/mp3":
            case "audio/mpeg":
                extension = "mp3";
                break;
        }
        return extension;
    }
}
