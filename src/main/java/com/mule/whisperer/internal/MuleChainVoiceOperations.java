package com.mule.whisperer.internal;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.net.HttpURLConnection;

import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.*;

import com.mule.whisperer.MuleChainVoiceConfiguration;
import com.mule.whisperer.helpers.STTParamsModelDetails;
import com.mule.whisperer.helpers.TTSParamsModelDetails;
import com.mule.whisperer.helpers.LocalSTTParamsModelDetails;
import com.mule.whisperer.helpers.AudioFileReader;
import com.mule.whisperer.helpers.WhisperContextHelper;

import org.mule.runtime.extension.api.annotation.Alias;

// WhisperJNI: These are imports related to the Whisper JNI library.
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class MuleChainVoiceOperations {
  private static final String API_URL = "https://api.openai.com/v1/audio/";
  private static final Logger LOGGER = LoggerFactory.getLogger(MuleChainVoiceOperations.class);


/**
   * Converts speech to Text in local mode
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Speech-to-text-local")
  public InputStream speechToTextLocal(@Content InputStream audioFile, @Config MuleChainVoiceConfiguration configuration, @ParameterGroup(name = "Local properties") LocalSTTParamsModelDetails localParams) {
      JSONObject jsonResponse = new JSONObject();
      File tempAudioFile = null; // Declare tempAudioFile here to access it in finally block

      LOGGER.info("Starting speech-to-text operation in local mode.");

      try {
          // Check if local Whisper mode is enabled in the configuration
          if (!configuration.isUseLocalWhisper()) {
              LOGGER.warn("Local Whisper mode not activated. Use the OpenAI API or enable the local option.");
              throw new RuntimeException("Local Whisper mode not activated. Use the OpenAI API or enable the local option.");
          } else {
              LOGGER.info("Local Whisper mode is enabled. Proceeding with loading the Whisper JNI library.");

              // Load the Whisper JNI library
              WhisperJNI.loadLibrary();
              WhisperJNI whisper = new WhisperJNI();

              // Initialize or reuse the Whisper context with the provided model path
              WhisperContext ctx = WhisperContextHelper.getOrCreateWhisperContext(whisper, localParams.getModelPath());
              if (ctx == null) {
                  LOGGER.error("Failed to initialize Whisper context.");
                  throw new RuntimeException("Failed to initialize Whisper context");
              } else {
                  // Set up Whisper parameters from the local parameters
                  WhisperFullParams params = new WhisperFullParams();
                  params.nThreads = localParams.getNThreads();
                  params.language = localParams.getLanguage();
                  params.translate = localParams.isTranslate();
                  params.printProgress = localParams.isPrintProgress();

                  LOGGER.info("Whisper context initialized successfully. Processing audio input.");

                  // Process the InputStream based on the audio format
                  String appHomePath = System.getProperty("app.home");
                  tempAudioFile = new File(appHomePath, "audio." + localParams.getAudioFormat().name().toLowerCase());
                  try (OutputStream outStream = new FileOutputStream(tempAudioFile)) {
                      byte[] buffer = new byte[1024];
                      int bytesRead;
                      while ((bytesRead = audioFile.read(buffer)) != -1) {
                          outStream.write(buffer, 0, bytesRead);
                      }
                  }
                  LOGGER.info("Audio file successfully saved to temporary file: {}", tempAudioFile.getAbsolutePath());

                  // Convert audio to WAV if needed
                  String processedFilePath = tempAudioFile.getAbsolutePath();
                  if (!localParams.getAudioFormat().equals(LocalSTTParamsModelDetails.AudioFormat.WAV)) {
                      LOGGER.info("Converting audio file to WAV format.");
                      // Convert to WAV format
                      String wavFilePath = processedFilePath.replaceAll("\\.\\w+$", ".wav");
                      AudioFileReader.convertMp3ToWav(processedFilePath, wavFilePath);
                      processedFilePath = wavFilePath;
                      LOGGER.info("Audio file converted to WAV format: {}", wavFilePath);
                  }

                  // Read the audio file and get the audio samples
                  LOGGER.info("Reading audio file and extracting samples.");
                  float[] samples = AudioFileReader.readFile(new File(processedFilePath));

                  // Perform the speech-to-text operation
                  LOGGER.info("Performing speech-to-text operation with Whisper.");
                  int result = whisper.full(ctx, params, samples, samples.length);
                  if (result != 0) {
                      LOGGER.error("Transcription failed with code {}", result);
                      jsonResponse.put("error", "Transcription failed with code " + result);
                  } else {
                      // Collect the transcribed text from all segments
                      StringBuilder transcription = new StringBuilder();
                      int nSegments = whisper.fullNSegments(ctx);

                      for (int i = 0; i < nSegments; ++i) {
                          transcription.append(whisper.fullGetSegmentText(ctx, i)).append(" ");
                      }

                      // Add the transcription result to the JSON response
                      jsonResponse.put("transcription", transcription.toString().trim());
                      LOGGER.info("Transcription completed successfully.");
                  }
              }
          }
      } catch (Exception e) {
          e.printStackTrace();
          LOGGER.error("Error during Speech-to-Text processing with Whisper: {}", e.getMessage(), e);
          jsonResponse.put("error", "Error during Speech-to-Text processing with Whisper: " + e.getMessage());
      }finally {
          // Ensure the temporary file is deleted after processing
          if (tempAudioFile != null && tempAudioFile.exists()) {
              if (tempAudioFile.delete()) {
                  LOGGER.info("Temporary file deleted successfully: {}", tempAudioFile.getAbsolutePath());
              } else {
                  LOGGER.warn("Failed to delete temporary file: {}", tempAudioFile.getAbsolutePath());
              }
          }
      } 

      // Return the JSON response as InputStream
      return toInputStream(jsonResponse.toString(), StandardCharsets.UTF_8);
    }



  /**
   * Converts speech to Text
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Speech-to-text")
  public InputStream speechToText(@Content TypedValue<InputStream> audioContent, @Optional String finetuningPrompt, @Config MuleChainVoiceConfiguration configuration, @ParameterGroup(name= "Additional properties") STTParamsModelDetails paramDetails) {
    JSONObject jsonResponse;
    
    try {
      // Create the connection
      URL url = new URL(API_URL + "transcriptions");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Authorization", "Bearer " + configuration.getApiKey());
      connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");

      connection.setDoOutput(true);

      // Write the audio file and model parameter to the request
      try (OutputStream os = connection.getOutputStream()) {
          writeMultipartData(os, audioContent, paramDetails.getModelName(), finetuningPrompt, paramDetails.getResponseFormat(), (Double) paramDetails.getTemperature(), paramDetails.getLanguage());
      }

      // Get the response
      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
          BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
          String inputLine;
          StringBuilder response = new StringBuilder();

          while ((inputLine = in.readLine()) != null) {
              response.append(inputLine);
          }
          in.close();

          // Parse and display the response
          jsonResponse = new JSONObject(response.toString());
          System.out.println("Transcription: "+ jsonResponse.toString(2));
      } else {
          BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
          String errorLine;
          StringBuilder errorResponse = new StringBuilder();

          while ((errorLine = errorReader.readLine()) != null) {
              errorResponse.append(errorLine);
              System.out.println(errorLine);
          }
          errorReader.close();
          
          jsonResponse = new JSONObject();

          System.out.println("POST request failed. Response Code: " + responseCode);
          System.out.println("Error: " + errorResponse.toString());
      }

  
  
    } catch (Exception e) {
        e.printStackTrace();
        jsonResponse = new JSONObject();

    }    
    return toInputStream(jsonResponse.toString(), StandardCharsets.UTF_8);

  }

  /**
   * Text speech to Speech
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Text-to-speech")
  public InputStream textToSpeech(String textToConvert, String pathToOutputFile, @Config MuleChainVoiceConfiguration configuration,  @ParameterGroup(name= "Additional properties") TTSParamsModelDetails paramDetails){

    try {
      // Create JSON payload
      JSONObject jsonPayload = new JSONObject();
      jsonPayload.put("model", paramDetails.getModelName());
      jsonPayload.put("input", textToConvert);
      jsonPayload.put("voice", paramDetails.getVoice());
      jsonPayload.put("response_format", paramDetails.getResponseFormat());
      jsonPayload.put("speed", paramDetails.getSpeed());

      // Create the connection
      URL url = new URL(API_URL + "speech");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Authorization", "Bearer " + configuration.getApiKey());
      connection.setRequestProperty("Content-Type", "application/json");

      connection.setDoOutput(true);

      // Write JSON payload to the request
      try (OutputStream os = connection.getOutputStream()) {
          byte[] input = jsonPayload.toString().getBytes("utf-8");
          os.write(input, 0, input.length);
      }

      // Get the response
      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
          // Read response and save it to a file
          InputStream in = connection.getInputStream();
          FileOutputStream fos = new FileOutputStream(pathToOutputFile);

          byte[] buffer = new byte[4096];
          int bytesRead;
          while ((bytesRead = in.read(buffer)) != -1) {
              fos.write(buffer, 0, bytesRead);
          }
          fos.close();
          in.close();

          System.out.println("Speech synthesis succeeded. Output saved to " + pathToOutputFile);
      } else {
          BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
          String errorLine;
          StringBuilder errorResponse = new StringBuilder();

          while ((errorLine = errorReader.readLine()) != null) {
              errorResponse.append(errorLine);
          }
          errorReader.close();
          
          System.out.println("POST request failed. Response Code: " + responseCode);
          System.out.println("Error: " + errorResponse.toString());
      }

    } catch (Exception e) {
        e.printStackTrace();
    }

    JSONObject responseJson = new JSONObject();

    responseJson.put("outputDirectory", pathToOutputFile);

    return toInputStream(responseJson.toString(), StandardCharsets.UTF_8);

  }



  private static void writeMultipartData(OutputStream os, TypedValue<InputStream> audioContent, String model, String prompt, String responseFormat, double temperature, String language) throws IOException {
    String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);

    org.mule.runtime.api.metadata.MediaType audioContentType = audioContent.getDataType().getMediaType();
    // The OpenAI transcription API determines the audio codec using the file extension we provide
    String extension = guessAudioFileExtension(audioContentType);

      // Add audio file
    writer.append("--").append(boundary).append("\r\n");
    writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append("speech." + extension).append("\"\r\n");
    writer.append("Content-Type: " + audioContentType.toRfcString() + "\r\n\r\n").flush();

    try (InputStream inputStream = audioContent.getValue()) {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        os.flush();
    }

    writer.append("\r\n").flush();

    // Add model parameter
    writer.append("--").append(boundary).append("\r\n");
    writer.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
    writer.append(model).append("\r\n").flush();

    // Add prompt parameter
    if (prompt != null && !prompt.isEmpty()) {
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"prompt\"\r\n\r\n");
        writer.append(prompt).append("\r\n").flush();
    }

    // Add response_format parameter
    writer.append("--").append(boundary).append("\r\n");
    writer.append("Content-Disposition: form-data; name=\"response_format\"\r\n\r\n");
    writer.append(responseFormat).append("\r\n").flush();

    // Add temperature parameter
    writer.append("--").append(boundary).append("\r\n");
    writer.append("Content-Disposition: form-data; name=\"temperature\"\r\n\r\n");
    writer.append(String.valueOf(temperature)).append("\r\n").flush();

    if (!"auto".equals(language)) {
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"language\"\r\n\r\n");
        writer.append(language).append("\r\n").flush();
    }

    writer.append("--").append(boundary).append("--\r\n").flush();
}

    private static String guessAudioFileExtension(org.mule.runtime.api.metadata.MediaType audioContentType) {
        String extension = "mp3";
        switch (audioContentType.withoutParameters().toString()) {
            case "audio/m4a":
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
            case "audio.webm":
                extension = "weba";
                break;
        }
        return extension;
    }

}
