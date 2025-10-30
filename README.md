# KSP Maven Plugin

This is a Maven plugin for the Kotlin Symbol Processing (KSP) API. It allows you to integrate KSP into your Maven build process, enabling
you to use KSP processors in your Kotlin projects.

## Usage

To use the KSP Maven Plugin in your project, you need to add it to your `pom.xml` file. Below is an example configuration:

```xml

<build>
    <plugins>
        <plugin>
            <groupId>io.mcarle</groupId>
            <artifactId>ksp-maven-plugin</artifactId>
            <version>2.3.0-1</version>
            <executions>
                <execution>
                    <goals>
                        <goal>ksp</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <!-- Optional: Define processor options -->
                <processorOptions>
                    <option>key=value</option>
                </processorOptions>
            </configuration>
            <dependencies>
                <!-- Add your KSP processors here, e.g. konvert -->
                <dependency>
                    <groupId>io.mcarle</groupId>
                    <artifactId>konvert</artifactId>
                    <version>4.3.2</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
```

## How It Works

Since KSP2, the execution of KSP processors is not integrated into the Kotlin compiler anymore.
Instead, it is a separate step in the build lifecycle, that runs in advance to the Kotlin compiler

This plugin executes KSP in the `generate-sources` phase of the Maven build lifecycle, before the Kotlin compilation takes place.

To do that, it executes a separate Java process that runs the KSP processor(s) on the source files.

The generated sources are then added to the Maven project as additional source directories, so that they are included in the subsequent
Kotlin compilation.

### Include generated sources in IDE

This plugin generates Kotlin sources into the `target/generated-sources/ksp-kotlin` (or `target/generated-test-sources/ksp-kotlin` for test
sources) directory.

You may need to manually mark those directories as "Generated Sources Root" in your IDE to ensure that the IDE recognizes the generated
code.

## Configuration

The KSP Maven Plugin supports several configuration options that can be defined in the `<configuration>` section of the plugin in your
`pom.xml`:

* `jdkHome`

  By default the plugin uses the home of the Java instance that is used to run Maven.
  In case you want to use a different JDK to run KSP, you can specify the path to the JDK installation directory here.
  Also, this is passed to the KSP process as the `-jdk-home` parameter to resolve built-in types.
* `languageVersion`

  This specifies the Kotlin language version to be used by KSP.
* `apiVersion`

  This specifies the Kotlin api version to be used by KSP.
* `jvmTarget`

  This specifies the JVM target version to be used by KSP.
* `allWarningsAsErrors`

  This specifies the `-all-warnings-as-errors` flag for KSP.
* `kspLogLevel`

  This customizes the log level of KSP. Possible values are: `error`, `warn`, `info`, `debug`
* `processorOptions`

  This allows you to specify custom processor options that will be passed to the KSP processors.
  You can define multiple options as key-value pairs (e.g., `<option>key=value</option>`).

* `verbose`
  This enables verbose logging for separate Java process execution.

## Goals

The following goals are provided:
* `ksp`

  This goal runs the KSP processors on the source files.
  It is typically bound to the `generate-sources` phase of the Maven build lifecycle.
* `test-ksp`

  This goal runs the KSP processors on the test source files.
  It is typically bound to the `generate-test-sources` phase of the Maven build lifecycle.

## Version numbering

The version numbering follows the pattern: `<ksp-version>-<plugin-version>`.

The `<ksp-version>` matches the KSP version that the plugin is using (e.g., `2.3.0`).
To keep the versioning simple, the `<plugin-version>` is (typically) just a single number (e.g., `1`).

Given this, a version upgrade from e.g. `2.3.0-1` to `2.3.0-2` indicates a new plugin version for the same KSP version
that should be backward compatible and should not introduce breaking changes.

## How to Build

To build the KSP Maven Plugin from source, you can clone the repository and run the following command in the project directory:

```bash
mvn clean install
```

This will compile the plugin and install it to your local Maven repository.
The built plugin version is typically 0.1-SNAPSHOT (extracted from the [.mvn/maven.config](.mvn/maven.config) file),
but can be changed adding `-Drevision=x.y.z` parameter to the command.

## How to Release

To release a new version of the KSP Maven Plugin, follow these steps:

1. Run the `install` phase with the desired new version using the `-Drevision` parameter:

   ```bash
   mvn clean install -Drevision=x.y.z
   ```

2. Deploy the new version to Maven Central:

   ```bash
   mvn deploy -Drevision=x.y.z
   ```

