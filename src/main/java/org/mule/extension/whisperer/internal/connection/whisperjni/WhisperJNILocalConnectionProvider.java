package org.mule.extension.whisperer.internal.connection.whisperjni;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperJNI;
import org.mule.extension.whisperer.internal.helpers.models.WhisperJNICloudhubConfigurer;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.core.api.lifecycle.StartException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Alias("whisperjnifile")
@DisplayName("Whisper JNI (Local .bin)")
public class WhisperJNILocalConnectionProvider implements CachedConnectionProvider<WhisperJNIConnection>, Startable, Stoppable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhisperJNILocalConnectionProvider.class);

    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @Optional(defaultValue = "4")
    private int threads;

    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @Optional(defaultValue = "false")
    private boolean translate;

    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @Optional(defaultValue = "false")
    private boolean printProgress;

    @ParameterGroup(name ="Model")
    @Expression(ExpressionSupport.SUPPORTED)
    private WhisperJNILocalModelParameters model;

    private WhisperJNI whisper;
    private WhisperContext whisperContext;
    private Path tempModelFile;

    @Override
    public WhisperJNIConnection connect() throws ConnectionException {
        return new WhisperJNIConnection(whisper, whisperContext, threads, translate, printProgress);
    }

    @Override
    public void disconnect(WhisperJNIConnection whisperJNIConnection) {
    }

    @Override
    public ConnectionValidationResult validate(WhisperJNIConnection whisperJNIConnection) {
        return ConnectionValidationResult.success();
    }

    /**
     * Resolves the model file path, handling both classpath resources and file system paths.
     * If the path starts with "classpath://", the resource is extracted to a temporary file.
     *
     * @param modelPath The model file path (can be classpath:// or absolute file path)
     * @return Path to the model file on the file system
     * @throws IOException if the file cannot be resolved or extracted
     */
    private Path resolveModelPath(String modelPath) throws IOException {
        if (modelPath.startsWith("classpath://")) {
            String resourcePath = modelPath.substring("classpath://".length());
            LOGGER.debug("Loading model from classpath resource: {}", resourcePath);

            InputStream resourceStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath);

            if (resourceStream == null) {
                throw new IOException("Model file not found in classpath: " + resourcePath);
            }

            // Extract to temporary file
            String fileName = Paths.get(resourcePath).getFileName().toString();
            tempModelFile = Files.createTempFile("whisper-model-", "-" + fileName);

            LOGGER.debug("Extracting classpath model to temporary file: {}", tempModelFile);
            Files.copy(resourceStream, tempModelFile, StandardCopyOption.REPLACE_EXISTING);
            resourceStream.close();

            // Mark for deletion on JVM exit as backup cleanup
            tempModelFile.toFile().deleteOnExit();

            return tempModelFile;
        } else {
            // Regular file system path
            Path filePath = Paths.get(modelPath);
            if (!Files.exists(filePath)) {
                throw new IOException("Model file not found: " + modelPath);
            }
            return filePath;
        }
    }

    @Override
    public void start() throws MuleException {
        try {

            if(WhisperJNICloudhubConfigurer.isCloudHubDeployment()) {
                LOGGER.info("CloudHub deployment detected. Performing CloudHub specific setup.");
                Path dependenciesPath = Paths.get(WhisperJNICloudhubConfigurer.WHISPER_DEPENDENCY_LIBS_PATH);
                if(!Files.exists(dependenciesPath)) {
                    synchronized (WhisperJNIRemoteConnectionProvider.class) {
                        if (!Files.exists(dependenciesPath)) {
                            WhisperJNICloudhubConfigurer.setup();;
                        }
                    }
                }
            }

            WhisperJNI.loadLibrary();
            whisper = new WhisperJNI();

            // Resolve model path (handles both classpath and file system paths)
            Path modelPath = resolveModelPath(model.getModelFilePath());
            whisperContext = whisper.init(modelPath);

            LOGGER.info("WhisperJNI initialized successfully with model: {}", modelPath);

        } catch (IOException e) {
            throw new StartException(e, this);
        }
    }

    @Override
    public void stop() throws MuleException {
        if (null != whisperContext) {
            whisperContext.close();
        }

        // Clean up temporary model file if it was created
        if (tempModelFile != null && Files.exists(tempModelFile)) {
            try {
                Files.delete(tempModelFile);
                LOGGER.info("Deleted temporary model file: {}", tempModelFile);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete temporary model file: {}", tempModelFile, e);
            }
        }
    }
}
