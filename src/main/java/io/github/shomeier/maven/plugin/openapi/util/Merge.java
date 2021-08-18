package io.github.shomeier.maven.plugin.openapi.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;

@Mojo(name = Merge.GOAL, defaultPhase = LifecyclePhase.INITIALIZE)
public class Merge extends AbstractMojo {
    public static final String GOAL = "merge";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(required = true)
    private File headerFile;

    @Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
    private List<Resource> resources;

    @Parameter(required = true)
    private File outputFile;

    @Parameter(defaultValue = "false", required = false)
    private boolean resolve;

    @Parameter(defaultValue = "false", required = false)
    private boolean resolveFully;

    @Parameter(defaultValue = "false", required = false)
    private String exclude;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    public void execute() throws MojoExecutionException {
        validateParameters();

        try {
            FileUtils.copyFile(headerFile, outputFile);

            ResourcesResolver resolver = new ResourcesResolver(resources, project, getLog());
            List<Path> includedFiles = resolver.getIncludedFiles();

            Merger merger = new Merger(outputFile, getResolveOption());
            OpenAPI mergedApi = merger.merge(includedFiles);

            Excluder excluder = new Excluder(exclude);
            excluder.exclude(mergedApi);

            String yamlAsString = Yaml.pretty().writeValueAsString(mergedApi);
            FileUtils.writeStringToFile(outputFile, yamlAsString, Charset.defaultCharset(), false);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while merging", e);
        }
    }

    private void validateParameters() throws MojoExecutionException {
        Validator.validateFile(headerFile, "headerFile");
        Validator.validateParam(outputFile, "outputFile");
    }

    private ResolveOption getResolveOption() {
        if (resolveFully) {
            return ResolveOption.RESOLVE_FULLY;
        }

        if (resolve) {
            return ResolveOption.RESOLVE;
        }

        return ResolveOption.NO_RESOLVE;
    }
}
