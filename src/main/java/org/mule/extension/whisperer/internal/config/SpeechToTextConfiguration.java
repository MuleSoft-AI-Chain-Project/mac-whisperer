package org.mule.extension.whisperer.internal.config;

import org.mule.extension.whisperer.internal.operation.SpeechToTextOperations;
import org.mule.extension.whisperer.internal.connection.whisperjni.WhisperJNILocalConnectionProvider;
import org.mule.extension.whisperer.internal.connection.whisperjni.WhisperJNIRemoteConnectionProvider;
import org.mule.extension.whisperer.internal.connection.openai.OpenAiConnectionProvider;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;


@Configuration(name = "speech-to-text")
@Operations(SpeechToTextOperations.class)
@ConnectionProviders({OpenAiConnectionProvider.class, WhisperJNILocalConnectionProvider.class, WhisperJNIRemoteConnectionProvider.class})
public class SpeechToTextConfiguration {

}