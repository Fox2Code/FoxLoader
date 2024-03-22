package com.fox2code.foxloader.dev.tests;

import com.fox2code.foxloader.dev.GradlePlugin;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SampleGradlePluginTest {
    @Test
    public void sampleTest() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("foxloader.dev");
        // Test if java plugin is loaded
        for (String pluginName : new String[]{"foxloader.dev", "java"}) {
            Assertions.assertTrue(project.getPluginManager().hasPlugin(pluginName), () -> "Missing plugin " + pluginName);
        }
        // Test if extension work properly
        GradlePlugin.FoxLoaderConfig config = (GradlePlugin.FoxLoaderConfig)
                project.getExtensions().getByName("foxloader");
        Assertions.assertNotNull(config, "Missing FoxLoader Config Extension");
        config.setDecompileSources(false); // <- Disable source decompiling for tests
        // Run, "afterEvaluate" listeners
        project.evaluationDependsOn(":");
        // Test if tasks are injected properly
        for (String taskName : new String[]{"runClient", "runServer"}) {
            Assertions.assertNotNull(project.getTasks().getByName(taskName), () -> "Missing task " + taskName);
        }
    }
}
