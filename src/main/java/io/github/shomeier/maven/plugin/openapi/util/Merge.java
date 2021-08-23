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
import pl.joegreen.lambdaFromString.LambdaCreationException;

@Mojo(name = Merge.GOAL, defaultPhase = LifecyclePhase.INITIALIZE)
public class Merge extends AbstractMojo {
    public static final String GOAL = "merge";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(required = true)
    private File headerFile;

    @Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
    private List<Resource> resources;

    @Parameter(required = false, readonly = true)
    private List<Transformer> transformers;

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
        Validator.validateParam(outputFile, "outputFile");

        try {
            List<Path> includedFiles = new ResourcesResolver(resources, project, getLog())
                    .getIncludedFiles();
            Validator.validateIncludedFiles(includedFiles);

            // we only need the header file if we merge more than one yaml file
            if (includedFiles.size() > 1) {
                Validator.validateFile(headerFile, "headerFile");
            } else {
                if (headerFile == null) {
                    headerFile = includedFiles.get(0).toFile();
                }
            }
            FileUtils.copyFile(headerFile, outputFile);

            OpenAPI openApi = new Merger(outputFile)
                    .merge(includedFiles);

            new Excluder(exclude).exclude(openApi);

            if (transformers != null) {
                for (Transformer transformer : transformers) {
                    transformer.transform(openApi);
                }
            }

            OpenAPI resolvedApi = new YamlResolver(getResolveOption())
                    .resolve(openApi);

            // org.openapitools.codegen.serializer.SerializerUtils sorts properties alphabetically
            // String yamlAsString = SerializerUtils.toYamlString(resolvedApi);
            String yamlAsString = Yaml.pretty().writeValueAsString(resolvedApi);
            FileUtils.writeStringToFile(outputFile, yamlAsString, Charset.defaultCharset(), false);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while merging", e);
        } catch (LambdaCreationException e) {
            throw new MojoExecutionException("Error while creating lambda transformation", e);
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
