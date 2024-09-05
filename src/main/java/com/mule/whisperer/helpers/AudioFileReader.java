package com.mule.whisperer.helpers;

import javazoom.jl.decoder.*;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.*;
import org.slf4j.*;

public class AudioFileReader {

    /**
     * Reads an audio file from InputStream and converts its data into an array of float samples.
     *
     * @param audioFile the File object of the audio file to be read.
     * @return an array of float values representing the audio samples.
     * @throws UnsupportedAudioFileException if the audio file format is not supported.
     * @throws IOException                   if an I/O error occurs during file reading.
     */
    public static float[] readFile(File audioFile) throws UnsupportedAudioFileException, IOException {
        // Open the audio file and create an AudioInputStream
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
        
        // Create a ByteBuffer to read the audio data into
        ByteBuffer captureBuffer = ByteBuffer.allocate(audioInputStream.available());
        captureBuffer.order(ByteOrder.LITTLE_ENDIAN); // Ensure the buffer uses little-endian byte order
        
        // Read the audio data into the buffer
        int bytesRead = audioInputStream.read(captureBuffer.array());
        if (bytesRead == -1) {
            throw new IOException("Unable to read audio file: " + audioFile.getAbsolutePath());
        }

        // Convert the ByteBuffer to a ShortBuffer for easier processing
        ShortBuffer shortBuffer = captureBuffer.asShortBuffer();
        
        // Create a float array to store the converted audio samples
        float[] samples = new float[shortBuffer.capacity()];
        int i = 0;
        
        // Convert each short sample to a float value between -1.0f and 1.0f
        while (shortBuffer.hasRemaining()) {
            samples[i++] = Math.max(-1f, Math.min(((float) shortBuffer.get()) / (float) Short.MAX_VALUE, 1f));
        }
        
        return samples; // Return the array of audio samples
    }

    
    /**
     * Converts an MP3 file to WAV format.
     *
     * @param mp3FilePath the path to the source MP3 file.
     * @param wavFilePath the path where the converted WAV file should be saved.
     * @throws IOException                   if an I/O error occurs during conversion.
     * @throws UnsupportedAudioFileException if the MP3 file format is not supported.
     */
    public static void convertMp3ToWav(String mp3FilePath, String wavFilePath) throws IOException, UnsupportedAudioFileException {
        Mp3ToWavConverter.convertMp3ToWav(mp3FilePath, wavFilePath);
    }
}

class Mp3ToWavConverter {

    public static void convertMp3ToWav(String mp3FilePath, String wavFilePath) throws IOException, UnsupportedAudioFileException {
        try (FileInputStream mp3Stream = new FileInputStream(mp3FilePath);
             FileOutputStream wavStream = new FileOutputStream(wavFilePath)) {

            Bitstream bitstream = new Bitstream(mp3Stream);
            Decoder decoder = new Decoder();

            AudioFormat baseFormat = null;

            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            long totalFrames = 0;
            Header header;
            
            while ((header = bitstream.readFrame()) != null) {
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);

                if (baseFormat == null) {
                    baseFormat = new AudioFormat(
                        (float) header.frequency(),
                        16, // 16-bit samples
                        output.getChannelCount(),
                        true, // signed
                        false // little-endian
                    );
                }

                byte[] buffer = new byte[output.getBufferLength() * 2]; // Buffer size * 2 for bytes
                for (int i = 0; i < output.getBufferLength(); i++) {
                    short val = output.getBuffer()[i];
                    buffer[2 * i] = (byte) (val & 0x00ff);
                    buffer[2 * i + 1] = (byte) ((val & 0xff00) >> 8);
                }
                byteOutputStream.write(buffer, 0, buffer.length); // write all bytes
                totalFrames += output.getBufferLength(); // Use buffer length directly

                bitstream.closeFrame();
            }

            byte[] audioData = byteOutputStream.toByteArray();
            InputStream byteInputStream = new ByteArrayInputStream(audioData);
            AudioInputStream audioInputStream = new AudioInputStream(byteInputStream, baseFormat, audioData.length / baseFormat.getFrameSize());

            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavStream);

        } catch (JavaLayerException e) {
            e.printStackTrace();
        }
    }
}
