package com.mule.whisperer.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import java.net.HttpURLConnection;

import org.apache.xmlbeans.impl.common.SystemCache;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;

import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import com.mule.whisperer.MuleChainVoiceConfiguration;
import com.mule.whisperer.helpers.STTParamsModelDetails;
import com.mule.whisperer.helpers.TTSParamsModelDetails;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;


/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class MuleChainVoiceOperations {
  private static final String API_URL = "https://api.openai.com/v1/audio/";
  /**
   * Converts speech to Text
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Speech-to-text")
  public String speechToText(String audioFilePath, @Optional String finetuningPrompt, @Config MuleChainVoiceConfiguration configuration, @ParameterGroup(name= "Additional properties") STTParamsModelDetails paramDetails) {
    JSONObject jsonResponse;
    
    try {
      // Prepare the audio file
      File audioFile = new File(audioFilePath);

      // Create the connection
      URL url = new URL(API_URL + "transcriptions");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Authorization", "Bearer " + configuration.getApiKey());
      connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");

      connection.setDoOutput(true);

      // Write the audio file and model parameter to the request
      try (OutputStream os = connection.getOutputStream()) {
          writeMultipartData(os, audioFile, paramDetails.getModelName(), finetuningPrompt, paramDetails.getResponseFormat(), (Double) paramDetails.getTemperature(), paramDetails.getLanguage());
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
    return jsonResponse.toString();
  }

  /**
   * Text speech to Speech
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Text-to-speech")
  public String textToSpeech(String textToConvert, String pathToOutputFile, @Config MuleChainVoiceConfiguration configuration,  @ParameterGroup(name= "Additional properties") TTSParamsModelDetails paramDetails){

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

    return responseJson.toString();
  }



  private static void writeMultipartData(OutputStream os, File audioFile, String model, String prompt, String responseFormat, double temperature, String language) throws IOException {
    String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);

    // Add audio file
    writer.append("--").append(boundary).append("\r\n");
    writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(audioFile.getName()).append("\"\r\n");
    writer.append("Content-Type: audio/mpeg\r\n\r\n").flush(); // Set the correct MIME type for MP3

    try (FileInputStream inputStream = new FileInputStream(audioFile)) {
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

}
