package org.sho.maven.plugin.openapi.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;

@Mojo(name = Merge.GOAL, defaultPhase = LifecyclePhase.INITIALIZE)
public class Merge extends AbstractMojo {
    public static final String GOAL = "merge";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(required = true)
    private File headerFile;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources", required = true)
    private File inputDirectory;

    @Parameter(required = true)
    private File outputFile;

    public void execute() throws MojoExecutionException {
        validateParameters();

        try {
            FileUtils.copyFile(headerFile, outputFile);
            try (Stream<Path> files = Files.walk(inputDirectory.toPath())) {
                Map<Path, OpenAPI> filePaths = files
                        .filter(p -> p.toFile().isFile() &&
                                (p.toString().endsWith(".yaml") || p.toString().endsWith(".yml")))
                        .collect(Collectors.toMap(p -> p, this::parse));

                merge(outputFile.toPath(), filePaths);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error while merging", e);
        }
    }

    private OpenAPI parse(Path path) {
        return new OpenAPIV3Parser().read(path.toString());
    }

    private void merge(Path targetFile, Map<Path, OpenAPI> filePaths) throws IOException {

        OpenAPI target = new OpenAPIV3Parser().read(targetFile.toString());

        // JsonFactory factory = Yaml.mapper().getFactory();
        // factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        // .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        // .enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS);

        for (Entry<Path, OpenAPI> filePathEntry : filePaths.entrySet()) {

            for (Entry<String, PathItem> sourcePaths : filePathEntry.getValue().getPaths().entrySet()) {

                // relativize assumes the path is a directory that is why we need to relativize from parnet
                Path relativePath = targetFile.getParent().relativize(filePathEntry.getKey());
                String ref = buildRef(relativePath, sourcePaths.getKey());
                PathItem pathItem = new PathItem().$ref(ref);
                target.path(sourcePaths.getKey(), pathItem);
            }
        }
        String pathsString = Yaml.pretty().writeValueAsString(target);
        FileUtils.writeStringToFile(targetFile.toFile(), pathsString, Charset.defaultCharset(), false);
    }

    private String buildRef(Path relativePath, String oaPath) {
        return "./" + relativePath.toString() + "#/paths/" + oaPath.replace("/", "~1");
    }

    private void validateParameters() throws MojoExecutionException {
        validateFile(headerFile, "headerFile");

        validateDirectory(inputDirectory, "inputDirectory");
        if (inputDirectory.listFiles().length == 0) {
            throw new MojoExecutionException(
                    String.format("The specified inputDirectory '%s' is empty!", inputDirectory));
        }

        validateParam(outputFile, "outputFile");
    }

    private void validateFile(File file, String paramName) throws MojoExecutionException {
        validateParam(file, "paramName");
        if (!headerFile.exists()) {
            throw new MojoExecutionException(
                    String.format("The specified %s '%s' does not exist!", paramName, file));
        }
    }

    private void validateDirectory(File directory, String paramName)
            throws MojoExecutionException {

        validateParam(directory, paramName);
        if (!directory.exists()) {
            throw new MojoExecutionException(
                    String.format("The specified %s '%s' does not exist!", paramName, directory));
        }
        if (!directory.isDirectory()) {
            throw new MojoExecutionException(
                    String.format("The specified %s '%s' is not a directory!", paramName, directory));
        }
    }

    private void validateParam(Object param, String paramName) throws MojoExecutionException {
        if (param == null) {
            throw new MojoExecutionException(
                    String.format("The parameter '%s' is not set!", paramName));
        }
    }

}
