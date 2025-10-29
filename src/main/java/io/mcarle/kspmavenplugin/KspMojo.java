package io.mcarle.kspmavenplugin;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "ksp",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class KspMojo extends AbstractKspMojo {

    @Parameter(property = "ksp.skip", defaultValue = "false")
    private boolean skip;

    @Override
    boolean isTestScope() {
        return false;
    }

    @Override
    boolean isSkip() {
        return skip;
    }
}
