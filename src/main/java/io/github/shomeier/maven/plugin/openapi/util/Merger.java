package io.github.shomeier.maven.plugin.openapi.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;

public class Merger {

    private final File outputFile;

    public Merger(File outputFile) {
        this.outputFile = outputFile;
    }

    public OpenAPI merge(List<Path> includedFiles) throws IOException {
        Map<Path, OpenAPI> filePaths = includedFiles.stream()
                .collect(Collectors.toMap(p -> p, this::parse));

        return merge(outputFile.toPath(), filePaths);
    }

    private OpenAPI merge(Path targetFile, Map<Path, OpenAPI> filePaths) throws IOException {

        OpenAPI target = parse(targetFile);

        Map<String, PathItem> allPaths = new HashMap<>();
        Components allComponents = new Components();
        for (Entry<Path, OpenAPI> filePathEntry : filePaths.entrySet()) {

            OpenAPI openApi = filePathEntry.getValue();
            mergeComponents(allComponents, openApi.getComponents());

            mergePaths(allPaths, targetFile, filePathEntry);
        }

        target.components(allComponents);
        allPaths.entrySet().forEach(e -> target.path(e.getKey(), e.getValue()));

        return target;
    }

    private OpenAPI parse(Path path) {
        return new OpenAPIV3Parser().read(path.toString());
    }

    private String buildRef(Path relativePath, String oaPath) {
        return "./" + relativePath.toString() + "#/paths/" + oaPath.replace("/", "~1");
    }

    private void mergePaths(Map<String, PathItem> allPaths, Path targetFile, Entry<Path, OpenAPI> filePathEntry) {

        Paths paths = filePathEntry.getValue().getPaths();
        if (paths != null) {
            for (Entry<String, PathItem> sourcePaths : paths.entrySet()) {

                PathItem pathItem = sourcePaths.getValue();
                // relativize assumes the path is a directory that is why we need to relativize from parent
                Path relativePath = targetFile.getParent().relativize(filePathEntry.getKey());
                String ref = buildRef(relativePath, sourcePaths.getKey());
                pathItem.addExtension(Constants.INTERNAL_REF_EXTENSION, ref);
                // TODO: Log Warning that duplicate paths might exist
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
