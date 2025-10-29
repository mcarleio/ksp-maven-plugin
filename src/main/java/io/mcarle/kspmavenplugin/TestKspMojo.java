package io.mcarle.kspmavenplugin;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "test-ksp",
    defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
    requiresDependencyResolution = ResolutionScope.TEST
)
public class TestKspMojo extends AbstractKspMojo {

    @Parameter(property = "ksp.skipTest", defaultValue = "false")
    private boolean skipTest;

    @Override
    boolean isTestScope() {
        return true;
    }

    @Override
    boolean isSkip() {
        return skipTest;
    }
}
