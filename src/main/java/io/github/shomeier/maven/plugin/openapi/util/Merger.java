package io.github.shomeier.maven.plugin.openapi.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

public class Merger {

    private final File outputFile;
    private final ResolveOption resolveOption;

    public Merger(File outputFile, ResolveOption resolveFully) {
        this.outputFile = outputFile;
        this.resolveOption = resolveFully;
    }

    public void merge(List<Path> includedFiles) throws IOException {
        Map<Path, OpenAPI> filePaths = includedFiles.stream()
                .collect(Collectors.toMap(p -> p, this::parse));

        merge(outputFile.toPath(), filePaths);
    }

    private void merge(Path targetFile, Map<Path, OpenAPI> filePaths) throws IOException {

        OpenAPI target = parse(targetFile);

        Map<String, PathItem> allPaths = new HashMap<>();
        Components allComponents = new Components();
        for (Entry<Path, OpenAPI> filePathEntry : filePaths.entrySet()) {

            OpenAPI openApi = filePathEntry.getValue();
            if (resolveOption.equals(ResolveOption.RESOLVE)) {
                mergeComponents(allComponents, openApi.getComponents());
            }

            mergePaths(allPaths, targetFile, filePathEntry);
        }

        if (resolveOption.equals(ResolveOption.RESOLVE)) {
            target.components(allComponents);
        }

        List<String> sortedPathKeys = allPaths.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        for (String pathKey : sortedPathKeys) {
            target.path(pathKey, allPaths.get(pathKey));
        }

        String pathsString = Yaml.pretty().writeValueAsString(target);
        FileUtils.writeStringToFile(targetFile.toFile(), pathsString, Charset.defaultCharset(), false);
    }

    private OpenAPI parse(Path path) {
        final ParseOptions options = new ParseOptions();

        if (resolveOption.equals(ResolveOption.RESOLVE)) {
            options.setResolve(true);
        } else if (resolveOption.equals(ResolveOption.RESOLVE_FULLY)) {
            options.setResolve(true);
            options.setResolveFully(true);
        }

        return new OpenAPIV3Parser().read(path.toString(), null, options);
    }

    private String buildRef(Path relativePath, String oaPath) {
        return "./" + relativePath.toString() + "#/paths/" + oaPath.replace("/", "~1");
    }

    private void mergePaths(Map<String, PathItem> allPaths, Path targetFile, Entry<Path, OpenAPI> filePathEntry) {

        Paths paths = filePathEntry.getValue().getPaths();
        if (paths != null) {
            for (Entry<String, PathItem> sourcePaths : paths.entrySet()) {

                // relativize assumes the path is a directory that is why we need to relativize from parent
                Path relativePath = targetFile.getParent().relativize(filePathEntry.getKey());
                PathItem pathItem = sourcePaths.getValue();
                if (resolveOption.equals(ResolveOption.NO_RESOLVE)) {
                    String ref = buildRef(relativePath, sourcePaths.getKey());
                    pathItem = new PathItem().$ref(ref);
                }
                allPaths.put(sourcePaths.getKey(), pathItem);
            }
        }
    }

    private void mergeComponents(Components existingComponents, Components newComponents) {

        if (newComponents == null) {
            return;
        }

        mergeComponent(newComponents.getSchemas(), e -> existingComponents.addSchemas(e.getKey(), e.getValue()));
        mergeComponent(newComponents.getRequestBodies(),
                e -> existingComponents.addRequestBodies(e.getKey(), e.getValue()));
        mergeComponent(newComponents.getExamples(), e -> existingComponents.addExamples(e.getKey(), e.getValue()));
        mergeComponent(newComponents.getExtensions(), e -> existingComponents.addExtension(e.getKey(), e.getValue()));
        mergeComponent(newComponents.getHeaders(), e -> existingComponents.addHeaders(e.getKey(), e.getValue()));
        mergeComponent(newComponents.getLinks(), e -> existingComponents.addLinks(e.getKey(), e.getValue()));
        mergeComponent(newComponents.getParameters(), e -> existingComponents.addParameters(e.getKey(), e.getValue()));
        mergeComponent(newComponents.getResponses(), e -> existingComponents.addResponses(e.getKey(), e.getValue()));
        mergeComponent(newComponents.getSecuritySchemes(),
                e -> existingComponents.addSecuritySchemes(e.getKey(), e.getValue()));
        mergeComponent(newComponents.getCallbacks(), e -> existingComponents.addCallbacks(e.getKey(), e.getValue()));
    }


    private <T> void mergeComponent(Map<String, T> component, Consumer<Entry<String, T>> consumer) {
        if (component != null) {
            for (Entry<String, T> entry : component.entrySet()) {
                consumer.accept(entry);
            }
        }
    }
}
