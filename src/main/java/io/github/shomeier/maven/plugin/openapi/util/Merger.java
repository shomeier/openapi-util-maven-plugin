package io.github.shomeier.maven.plugin.openapi.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.commons.io.FileUtils;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.ObjectMapperFactory;
import io.swagger.v3.parser.OpenAPIV3Parser;

public class Merger {

    private final File outputFile;
    private final boolean expandPaths;

    public Merger(File outputFile, boolean expandPaths) {
        this.outputFile = outputFile;
        this.expandPaths = expandPaths;
    }

    public void merge(List<Path> includedFiles) throws IOException {
        Map<Path, OpenAPI> filePaths = includedFiles.stream()
                .collect(Collectors.toMap(p -> p, this::parse));

        merge(outputFile.toPath(), filePaths);
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

        Map<String, PathItem> allPaths = new HashMap<>();
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
                    allPaths.put(sourcePaths.getKey(), pathItem);
                }
            }
        }

        List<String> sortedPathKeys = allPaths.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        for (String pathKey : sortedPathKeys) {
            target.path(pathKey, allPaths.get(pathKey));
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
}
