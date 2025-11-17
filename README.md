# <img src="icon/icon.svg" width="6%" alt="banner"> MuleSoft  Whisperer Connector
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mulesoft-ai-chain-project/mule4-whisperer-connector)](https://central.sonatype.com/artifact/io.github.mulesoft-ai-chain-project/mule4-whisperer-connector/overview)

## <img src="https://raw.githubusercontent.com/MuleSoft-AI-Chain-Project/.github/main/profile/assets/mulechain-project-logo.png" width="6%" alt="banner">   [MuleSoft AI Chain (MAC) Project](https://mac-project.ai/docs/)

### <img src="icon/icon.svg" width="6%" alt="banner">   [MuleSoft Whisperer Connector](https://mac-project.ai/docs/mac-whisperer/connector-overview)

## Breaking Changes in v0.4.0

### Configuration Architecture Change

Version 0.4.0 introduces a breaking change to the configuration architecture. All applications using previous versions (v0.3.x or earlier) must update their XML configuration before upgrading.

The unified `<whisperer:config>` element has been removed and replaced with separate configuration elements:
- `<whisperer:speech-to-text-config>` for Speech-to-Text operations
- `<whisperer:text-to-speech-config>` for Text-to-Speech operations

This change enables better separation of concerns, future Speech-to-Speech (STS) support, improved type safety, and aligns with standard MuleSoft connector architecture patterns.

### Migration Guide

Before (v0.3.x):
```xml
<whisperer:config name="Whisperer_Config" apiKey="${openai.api.key}" />

<flow name="transcribe-audio-flow">
    <whisperer:speech-to-text config-ref="Whisperer_Config">
        <whisperer:audio-file>#[payload]</whisperer:audio-file>
    </whisperer:speech-to-text>
</flow>
```

After (v0.4.0+):
```xml
<whisperer:speech-to-text-config name="STT_Config" apiKey="${openai.api.key}" />

<flow name="transcribe-audio-flow">
    <whisperer:speech-to-text config-ref="STT_Config">
        <whisperer:audio-file>#[payload]</whisperer:audio-file>
    </whisperer:speech-to-text>
</flow>
```

Migration steps:
1. Replace `<whisperer:config>` with `<whisperer:speech-to-text-config>` or `<whisperer:text-to-speech-config>`
2. Update all `config-ref` attributes to reference new config names
3. Test all flows before deploying to production

See CHANGELOG.md for complete details.

MAC Whisperer supports 2 main use cases,
- **Speech-to-Text**: Converts audio files (wav, mp3, etc.) into text
- **Text-to-Speech**: Converts text to audio files (wav, mp3, etc.)

### Supported Audio Formats

#### Core Formats (Always Available)
- **MP3** - Pure Java decoder (JLayer)
- **WAV** - No conversion needed

#### Extended Formats (Optional ByteDeco FFmpeg)
- **M4A/AAC** - Requires ByteDeco FFmpeg
- **FLAC** - Requires ByteDeco FFmpeg
- **OGG** - Requires ByteDeco FFmpeg
- **WEBM** - Requires ByteDeco FFmpeg

To enable extended format support, add the ByteDeco FFmpeg dependency to your Mule application:

```xml
<!-- Full platform support (~150MB) -->
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>ffmpeg-platform</artifactId>
    <version>6.1.1-1.5.10</version>
</dependency>
```

Or use a platform-specific variant for smaller size (~30MB):

```xml
<!-- Platform-specific (choose your target) -->
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>ffmpeg</artifactId>
    <version>6.1.1-1.5.10</version>
    <classifier>macosx-arm64</classifier> <!-- Mac M1/M2/M3 -->
</dependency>
```

Available platform classifiers:
- `macosx-arm64` - Mac M1/M2/M3
- `macosx-x86_64` - Intel Mac
- `linux-x86_64` - Linux
- `windows-x86_64` - Windows

#### Mule Application Configuration

Configure the `mule-maven-plugin` in your Mule application's `pom.xml` to include ByteDeco as a shared library:

```xml
<dependencies>
    <!-- Add ByteDeco FFmpeg dependency -->
    <dependency>
        <groupId>org.bytedeco</groupId>
        <artifactId>ffmpeg</artifactId>
        <version>6.1.1-1.5.10</version>
        <classifier>macosx-arm64</classifier> <!-- Choose your platform -->
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.mule.tools.maven</groupId>
            <artifactId>mule-maven-plugin</artifactId>
            <version>4.3.0</version>
            <extensions>true</extensions>
            <configuration>
                <sharedLibraries>
                    <!-- Configure as shared library (groupId and artifactId only) -->
                    <sharedLibrary>
                        <groupId>org.bytedeco</groupId>
                        <artifactId>ffmpeg</artifactId>
                    </sharedLibrary>
                    <sharedLibrary>
                        <groupId>org.bytedeco</groupId>
                        <artifactId>javacpp</artifactId>
                    </sharedLibrary>
                </sharedLibraries>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Note**: The `sharedLibraries` configuration uses only `groupId` and `artifactId` (no version or classifier). The plugin automatically matches against dependencies declared in the `<dependencies>` section.

For full platform support, use `ffmpeg-platform` artifact instead of the platform-specific `ffmpeg` artifact.

**Why use sharedLibraries?**
- Native libraries must be loaded at the application classloader level
- Prevents classloader conflicts between connectors
- Required for proper ByteDeco FFmpeg initialization
- See [MuleSoft documentation](https://docs.mulesoft.com/mule-runtime/latest/mmp-concept#configure-shared-libraries) for details

### Requirements

#### Mule Runtime
Mulesoft Runtime >= 4.9.0

#### JDK

- The  supported version for Java SDK is JDK 17.
- Compilation with Java SDK must be done with JDK 17.

### Installation (using maven central dependency)

```xml
<dependency>
    <groupId>io.github.mulesoft-ai-chain-project</groupId>
    <artifactId>mule4-whisperer-connector</artifactId>
    <version>0.3.0</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

### Installation (building locally)

To use this connector, first [build and install](https://mac-project.ai/docs/mac-whisperer/getting-started) the connector into your local maven repository.
Then add the following dependency to your application's `pom.xml`:

```xml
<dependency>
    <groupId>com.mulesoft.connectors</groupId>
    <artifactId>mule4-whisperer-connector</artifactId>
    <version>{version}</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

### Installation into private Anypoint Exchange

You can also make this connector available as an asset in your Anyooint Exchange.

This process will require you to build the connector as above, but additionally you will need
to make some changes to the `pom.xml`.  For this reason, we recommend you fork the repository.

Then, follow the MuleSoft [documentation](https://docs.mulesoft.com/exchange/to-publish-assets-maven) to modify and publish the asset.

### Contribution
[How to contribute](https://mac-project.ai/docs/contribute)

### Documentation
- Check out the complete documentation in [mac-project.ai](https://mac-project.ai/docs/mac-whisperer/connector-overview)

---

### Stay tuned!

- 🌐 **Website**: [mac-project.ai](https://mac-project.ai)
- 📺 **YouTube**: [@MuleSoft-MAC-Project](https://www.youtube.com/@MuleSoft-MAC-Project)
- 💼 **LinkedIn**: [MAC Project Group](https://lnkd.in/gW3eZrbF)
