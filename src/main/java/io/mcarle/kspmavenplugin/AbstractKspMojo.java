package io.mcarle.kspmavenplugin;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

abstract class AbstractKspMojo extends AbstractMojo {

    /**
     * If set, overrides the used JDK.
     * <br/>
     * When not defined, <code>System.getProperty("java.home")</code> is used
     */
    @Parameter
    private String jdkHome;

    /**
     * Kotlin language version to use.
     * <br/>
     * Default is <code>2.2</code>
     */
    @Parameter(defaultValue = "2.2")
    private String languageVersion;

    /**
     * Kotlin api version to use.
     * <br/>
     * Default is <code>2.2</code>
     */
    @Parameter(defaultValue = "2.2")
    private String apiVersion;

    /**
     * The target JVM version.
     * <br/>
     * Default is <code>11</code>>
     */
    @Parameter(defaultValue = "11")
    private String jvmTarget;

    /**
     * If true, add verbose flag to java execution
     * <br/>
     * Default is <code>false</code>
     */
    @Parameter(defaultValue = "false")
    private boolean verbose;

    /**
     * Sets the "treat all warnings as errors" flag of KSP
     * <br/>
     * Default is <code>false</code>
     */
    @Parameter(defaultValue = "false")
    private boolean allWarningsAsErrors;

    /**
     * The KSP log level.
     * <br/>
     * Valid values: <code>error</code>, <code>warning</code> or <code>warn</code>, <code>info</code>, <code>debug</code>
     * <br/>
     * Default is <code>warn</code>
     */
    @Parameter(defaultValue = "warn")
    private String kspLogLevel;

    /**
     * Any options you want to give to the KSP processors
     * <pre>
     * &lt;processorOptions&gt;
     * &nbsp;&nbsp;&lt;option&gt;key1=value1&lt;/option&gt;
     * &nbsp;&nbsp;&lt;option&gt;key2=value2&lt;/option&gt;
     * &lt;/processorOptions&gt;
     * </pre>
     */
    @Parameter
    private List<String> processorOptions;

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private File buildDirectory;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(readonly = true, defaultValue = "${plugin.artifacts}")
    private List<Artifact> pluginDependencies;


    abstract boolean isTestScope();

    abstract boolean isSkip();


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (isSkip()) {
            getLog().info("KSP execution is skipped.");
            return;
        }

        CommandLine commandLine = new CommandLine(determineJavaExecutable());
        commandLine.addArguments(args(), false);

        PumpStreamHandler psh = new PumpStreamHandler(System.out, System.err, System.in);

        Executor exec = DefaultExecutor.builder()
            .setWorkingDirectory(basedir)
            .get();

        exec.setStreamHandler(psh);

        try {
            try {
                psh.start();
                exec.execute(commandLine, System.getenv());
            } finally {
                psh.stop();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        registerSourceRoot();
    }

    private String determineJavaExecutable() {
        String javaHome = determineJdkHome();

        Path javaBin = Paths.get(javaHome, "bin", "java");
        if (Files.exists(javaBin)) {
            return javaBin.toString();
        }

        // Fallback – rely on the OS PATH
        return "java";
    }

    private String determineJdkHome() {
        String javaHome;
        if (jdkHome != null && !jdkHome.isBlank()) {
            javaHome = jdkHome;
        } else {
            javaHome = System.getProperty("java.home");
        }
        return javaHome;
    }

    private String[] args() {
        List<String> arguments = new ArrayList<>();

        if (verbose) {
            arguments.add("-verbose");
        }
        if (!Objects.equals(kspLogLevel, "warn")) {
            arguments.add("-Dksp.logging=" + kspLogLevel);
        }

        arguments.add("-classpath");
        arguments.add(classpath());

        arguments.add("com.google.devtools.ksp.cmdline.KSPJvmMain");

        arguments.add("-jvm-target=" + jvmTarget);
        arguments.add("-module-name=" + project.getName());

        if (isTestScope()) {
            arguments.add("-source-roots=" + String.join(File.pathSeparator, project.getTestCompileSourceRoots()));
        } else {
            arguments.add("-source-roots=" + String.join(File.pathSeparator, project.getCompileSourceRoots()));
        }

        arguments.add("-project-base-dir=" + basedir.getAbsolutePath());
        arguments.add("-output-base-dir=" + buildDirectory.getAbsolutePath());

        arguments.add("-caches-dir=" + buildDirectory.getAbsoluteFile().toPath().resolve("ksp-caches"));
        arguments.add("-class-output-dir=" + generatedSrcDir("class"));
        arguments.add("-kotlin-output-dir=" + generatedSrcDir("kotlin"));
        arguments.add("-java-output-dir=" + generatedSrcDir("java"));
        arguments.add("-resource-output-dir=" + generatedSrcDir("resources"));

        arguments.add("-language-version=" + languageVersion);
        arguments.add("-api-version=" + apiVersion);

        arguments.add("-jdk-home=" + determineJdkHome());

        if (allWarningsAsErrors) {
            arguments.add("-all-warnings-as-errors=true");
        }

        if (!processorOptions.isEmpty()) {
            arguments.add("-processor-options=" + String.join(File.pathSeparator, processorOptions));
        }

        arguments.add("-libraries");
        arguments.add(libraries());

        return arguments.toArray(new String[0]);
    }

    private Path generatedSrcDir(String sub) {
        if (isTestScope()) {
            return buildDirectory.toPath().resolve("generated-test-sources").resolve("ksp-" + sub);
        } else {
            return buildDirectory.toPath().resolve("generated-sources").resolve("ksp-" + sub);
        }
    }

    private void registerSourceRoot() {
        var generatedKotlinSources = generatedSrcDir("kotlin");
        if (Files.exists(generatedKotlinSources)) {
            if (isTestScope()) {
                project.addTestCompileSourceRoot(generatedKotlinSources.toFile().getAbsolutePath());
            } else {
                project.addCompileSourceRoot(generatedKotlinSources.toFile().getAbsolutePath());
            }
        }

        var generatedJavaSources = generatedSrcDir("java");
        if (Files.exists(generatedJavaSources)) {
            if (isTestScope()) {
                project.addTestCompileSourceRoot(generatedJavaSources.toFile().getAbsolutePath());
            } else {
                project.addCompileSourceRoot(generatedJavaSources.toFile().getAbsolutePath());
            }
        }

        // TODO: is this the correct way to add resources?
        var generatedResourcesSources = generatedSrcDir("resources");
        if (Files.exists(generatedResourcesSources)) {
            if (isTestScope()) {
                project.addTestCompileSourceRoot(generatedResourcesSources.toFile().getAbsolutePath());
            } else {
                project.addCompileSourceRoot(generatedResourcesSources.toFile().getAbsolutePath());
            }
        }
    }

    private List<Artifact> filterForPluginRuntimeDependencies() {
        return pluginDependencies.stream()
            .filter(it -> it.getScope().equals("runtime"))
            .collect(Collectors.toList());
    }

    private List<Artifact> collectProjectArtifactsAndClasspath() {
        if (isTestScope()) {
            return project.getTestArtifacts();
        } else {
            return project.getRuntimeArtifacts();
        }
    }

    private String classpath() {
        return filterForPluginRuntimeDependencies()
            .stream()
            .map(it -> it.getFile().getAbsolutePath())
            .collect(Collectors.joining(File.pathSeparator));
    }

    private String libraries() {
        return collectProjectArtifactsAndClasspath()
            .stream()
            .map(it -> it.getFile().getAbsolutePath())
            .collect(Collectors.joining(File.pathSeparator));
    }

}
