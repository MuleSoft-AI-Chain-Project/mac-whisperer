# Contributing to MuleSoft Whisperer Connector

Thank you for your interest in contributing! Please see the [general contribution guide](https://mac-project.ai/docs/contribute) for the MAC Project.

## Technical Requirements

### Maven Configuration

This connector uses `mule-modules-parent` version `1.9.0`, which requires explicit Maven Surefire plugin configuration to avoid module path resolution issues during unit testing.

#### Required POM Configuration

**Mule SDK API Version**: Must use version `0.11.4` (stable release) to align with other MAC connectors using the same parent version:

```xml
<dependency>
    <groupId>org.mule.sdk</groupId>
    <artifactId>mule-sdk-api</artifactId>
    <version>0.11.4</version>
</dependency>
```

**Maven Surefire Plugin**: Must be explicitly configured to override the parent POM's problematic module path configuration:

```xml
<properties>
    <maven.surefire.version>2.22.2</maven.surefire.version>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>${maven.surefire.version}</version>
            <configuration>
                <argLine>-Dfile.encoding=UTF-8</argLine>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Why These Are Required

**Background**: The `mule-modules-parent` 1.9.0 POM introduced a module path configuration with unresolved Maven property placeholders (e.g., `${org.slf4j:slf4j-api:jar}`). Without explicit Surefire configuration, unit tests fail with:

```
Module org.slf4j not found, required by org.mule.runtime.boot
Error: --module-path=${org.slf4j:slf4j-api:jar}:...
```

This configuration pattern follows the **mule-vectors-connector**, which uses the same parent version (1.9.0) and requires identical workarounds.

### Version Alignment

| Connector | Parent Version | mule-sdk-api | Surefire Config |
|-----------|----------------|--------------|-----------------|
| **Whisperer** | 1.9.0 | 0.11.4 | ✅ Required |
| **Vectors** | 1.9.0 | 0.11.4 | ✅ Required |
| **Inference** | 1.9.6 | 0.11.4 | ✅ Required |
| **Einstein** | 1.6.9 | 0.10.3 | ✅ Required |
| **Bedrock** | 1.8.0 | (parent default) | ❌ Not needed (older parent) |

## Testing

Run unit tests:
```bash
mvn clean test
```

Run MUnit integration tests:
```bash
mvn clean verify
```

Both should pass without module path errors after applying the required configuration above.