package io.github.shomeier.maven.plugin.openapi.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.ObjectMapperFactory;
import io.swagger.v3.parser.OpenAPIV3Parser;

@Mojo(name = Merge.GOAL, defaultPhase = LifecyclePhase.INITIALIZE)
public class Merge extends AbstractMojo {
    public static final String GOAL = "merge";

    private static final String[] EMPTY_STRING_ARRAY = {};

    private static final String[] DEFAULT_INCLUDES = {"**/**"};

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(required = true)
    private File headerFile;

    @Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
    private List<Resource> resources;

    @Parameter(required = true)
    private File outputFile;

    @Parameter(defaultValue = "false", required = false)
    private boolean expandPaths;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Requirement
    private BuildContext buildContext;

    public void execute() throws MojoExecutionException {
        validateParameters();

        try {
            FileUtils.copyFile(headerFile, outputFile);

            List<Path> includedFiles = getIncludedFiles();
            Map<Path, OpenAPI> filePaths = includedFiles.stream()
                    .collect(Collectors.toMap(p -> p, this::parse));

            merge(outputFile.toPath(), filePaths);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while merging", e);
        }
    }

    List<Path> getIncludedFiles() {

        List<Path> includedFiles = new ArrayList<>();

        for (Resource resource : getResources()) {

            File resourceDirectory = new File(resource.getDirectory());

            if (!resourceDirectory.isAbsolute()) {
                resourceDirectory = new File(project.getBasedir(), resourceDirectory.getPath());
            }

            if (!resourceDirectory.exists()) {
                getLog().info("skip non existing resourceDirectory " + resourceDirectory.getPath());
                continue;
            }

            // Scanner scanner = buildContext.newScanner(resourceDirectory, true);
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(resourceDirectory);
            setupScanner(resource, scanner, true);
            scanner.scan();

            // for use in lambda below
            final File _resourceDirectory = resourceDirectory;
            List<Path> paths = Arrays.asList(scanner.getIncludedFiles()).stream()
                    .map(s -> java.nio.file.Paths.get(_resourceDirectory.getAbsolutePath(), s))
                    .collect(Collectors.toList());

            includedFiles.addAll(paths);
        }

        return includedFiles;
    }

    private String[] setupScanner(Resource resource, Scanner scanner, boolean addDefaultExcludes) {
        String[] includes = null;
        if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
            includes = resource.getIncludes().toArray(EMPTY_STRING_ARRAY);
        } else {
            includes = DEFAULT_INCLUDES;
        }
        scanner.setIncludes(includes);

        String[] excludes = null;
        if (resource.getExcludes() != null && !resource.getExcludes().isEmpty()) {
            excludes = resource.getExcludes().toArray(EMPTY_STRING_ARRAY);
            scanner.setExcludes(excludes);
        }

        if (addDefaultExcludes) {
            scanner.addDefaultExcludes();
        }
        return includes;
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

            Paths paths = filePathEntry.getValue().getPaths();
            if (paths != null) {
                for (Entry<String, PathItem> sourcePaths : paths.entrySet()) {

                    // relativize assumes the path is a directory that is why we need to relativize from parnet
                    Path relativePath = targetFile.getParent().relativize(filePathEntry.getKey());
                    String ref = buildRef(relativePath, sourcePaths.getKey());
                    PathItem pathItem = sourcePaths.getValue();
                    if (!expandPaths) {
                        pathItem = new PathItem().$ref(ref);
                    }
                    target.path(sourcePaths.getKey(), pathItem);
                }
            }
        }

        String pathsString = Yaml.pretty().writeValueAsString(target);
        FileUtils.writeStringToFile(targetFile.toFile(), pathsString, Charset.defaultCharset(), false);

        // Another option to write yamls here
        // ObjectMapper yamlMapper = createYaml();
        // String pathsString = yamlMapper.writer(new DefaultPrettyPrinter()).writeValueAsString(target);
        // FileUtils.writeStringToFile(targetFile.toFile(), pathsString, StandardCharsets.UTF_8, false);
    }

    protected static ObjectMapper createYaml() {
        ObjectMapper yamlMapper = ObjectMapperFactory.createYaml();
        ((YAMLFactory) yamlMapper.getFactory()).disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        ((YAMLFactory) yamlMapper.getFactory()).enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        ((YAMLFactory) yamlMapper.getFactory()).enable(YAMLGenerator.Feature.SPLIT_LINES);
        ((YAMLFactory) yamlMapper.getFactory()).enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS);
        ((YAMLFactory) yamlMapper.getFactory()).enable(YAMLGenerator.Feature.INDENT_ARRAYS);
        return yamlMapper;
    }

    private String buildRef(Path relativePath, String oaPath) {
        return "./" + relativePath.toString() + "#/paths/" + oaPath.replace("/", "~1");
    }

    private void validateParameters() throws MojoExecutionException {
        Validator.validateFile(headerFile, "headerFile");
        Validator.validateParam(outputFile, "outputFile");
    }

    /**
     * @return {@link #resources}
     */
    public List<Resource> getResources() {
        return resources;
    }

    /**
     * @param resources set {@link #resources}
     */
    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }
}
