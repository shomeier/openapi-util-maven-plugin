package io.github.shomeier.maven.plugin.openapi.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

public class YamlResolver {

    private final ResolveOption resolveOption;

    public YamlResolver(ResolveOption resolveOption) {
        this.resolveOption = resolveOption;
    }

    public OpenAPI resolve(OpenAPI openApi) throws JsonProcessingException {
        OpenAPI resultApi = parse(openApi);

        if (resolveOption.equals(ResolveOption.NO_RESOLVE)) {
            flattenPaths(resultApi).setComponents(null);
        } else if (resolveOption.equals(ResolveOption.RESOLVE_FULLY)) {
            resultApi.setComponents(null);
        }

        removeRefs(resultApi);
        sortPaths(resultApi);
        sortComponents(resultApi);
        return resultApi;
    }

    private OpenAPI parse(OpenAPI openApi) throws JsonProcessingException {
        final ParseOptions options = new ParseOptions();

        if (resolveOption.equals(ResolveOption.RESOLVE)) {
            options.setResolve(true);
        } else if (resolveOption.equals(ResolveOption.RESOLVE_FULLY)) {
            options.setResolve(true);
            options.setResolveFully(true);
        }

        String yamlAsString = Yaml.mapper().writeValueAsString(openApi);
        return new OpenAPIV3Parser().readContents(yamlAsString, null, options).getOpenAPI();
    }

    private PathItem ref(PathItem pathItem) {
        return new PathItem().$ref((String) pathItem.getExtensions().get(Constants.INTERNAL_REF_EXTENSION));
    }

    private OpenAPI flattenPaths(OpenAPI openApi) {

        openApi.getPaths().replaceAll((k, p) -> ref(p));
        return openApi;
    }

    private OpenAPI removeRefs(OpenAPI openApi) {

        Optional.ofNullable(openApi.getPaths()).ifPresent(paths -> {
            paths.values()
                    .forEach(p -> Optional.ofNullable(p.getExtensions())
                            .ifPresent(e -> e.remove(Constants.INTERNAL_REF_EXTENSION)));
        });
        return openApi;
    }

    private OpenAPI sortPaths(OpenAPI openApi) {

        Paths sortedPaths = new Paths();
        Optional.ofNullable(openApi.getPaths()).ifPresent(paths -> {
            paths.keySet().stream()
                    .sorted()
                    .forEach(k -> sortedPaths.put(k, paths.get(k)));
        });

        return openApi.paths(sortedPaths);
    }

    private OpenAPI sortComponents(OpenAPI openApi) {

        final Components components = openApi.getComponents();
        if (components != null) {
            components.setSchemas(sort(components.getSchemas()));
            components.setExtensions(sort(components.getExtensions()));
            components.setCallbacks(sort(components.getCallbacks()));
            components.setRequestBodies(sort(components.getRequestBodies()));
            components.setExamples(sort(components.getExamples()));
            components.setHeaders(sort(components.getHeaders()));
            components.setLinks(sort(components.getLinks()));
            components.setParameters(sort(components.getParameters()));
            components.setResponses(sort(components.getResponses()));
            components.setSecuritySchemes(sort(components.getSecuritySchemes()));
        }

        return openApi;
    }

    private <T> Map<String, T> sort(Map<String, T> map) {
        if (map != null) {
            return map.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        }
        return null;
    }
}
