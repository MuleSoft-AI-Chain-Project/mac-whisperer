package org.mule.extension.whisperer.internal.config;

import org.mule.extension.whisperer.internal.operation.TextToSpeechOperations;
import org.mule.extension.whisperer.internal.connection.openai.OpenAiConnectionProvider;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;


@Configuration(name = "text-to-speech")
@Operations(TextToSpeechOperations.class)
@ConnectionProviders(OpenAiConnectionProvider.class)
public class TextToSpeechConfiguration {

}