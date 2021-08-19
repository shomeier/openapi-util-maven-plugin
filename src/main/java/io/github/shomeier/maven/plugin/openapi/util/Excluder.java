package io.github.shomeier.maven.plugin.openapi.util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Paths;

public class Excluder {

    private final String exclude;

    public Excluder(String exclude) {
        this.exclude = exclude;
    }

    public OpenAPI exclude(OpenAPI openApi) {

        if ((exclude != null) && (!exclude.trim().isEmpty())) {
            for (Entry<String, PathItem> pathEntry : openApi.getPaths().entrySet()) {

                PathItem pathItem = pathEntry.getValue();
                Map<HttpMethod, Operation> operationsToExclude = findOperationsToExclude(pathItem);
                for (Entry<HttpMethod, Operation> excludedOperation : operationsToExclude.entrySet()) {
                    pathItem.operation(excludedOperation.getKey(), null);
                }
            }
        }

        return removeEmptyPaths(openApi);
    }

    private Map<HttpMethod, Operation> findOperationsToExclude(PathItem pathItem) {

        return pathItem.readOperationsMap().entrySet().stream()
                .filter(e -> Util.withNonNull(e.getValue().getExtensions(),
                        d -> d.keySet().stream().anyMatch(k -> k.matches(exclude)), false))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private OpenAPI removeEmptyPaths(OpenAPI openApi) {
        Paths paths = openApi.getPaths();
        List<String> pathsToRemove = paths.entrySet().stream()
                .filter(e -> e.getValue().readOperations().isEmpty())
                .map(Entry::getKey)
                .collect(Collectors.toList());
        pathsToRemove.forEach(paths::remove);

        return openApi;
    }
}
