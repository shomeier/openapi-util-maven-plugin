package io.github.shomeier.maven.plugin.openapi.util;

import java.util.Optional;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Yaml;
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
        return new PathItem().$ref((String) pathItem.getExtensions().get("x-openapi-util-ref"));
    }

    private OpenAPI flattenPaths(OpenAPI openApi) {

        openApi.getPaths().replaceAll((k, p) -> ref(p));
        return openApi;
    }

    private OpenAPI removeRefs(OpenAPI openApi) {

        openApi.getPaths().values()
                .forEach(p -> Optional.ofNullable(p.getExtensions()).ifPresent(e -> e.remove("x-openapi-util-ref")));
        return openApi;
    }

    private OpenAPI sortPaths(OpenAPI openApi) {

        Paths oldPaths = openApi.getPaths();
        Paths sortedPaths = new Paths();
        oldPaths.keySet().stream()
                .sorted()
                .forEach(k -> sortedPaths.put(k, oldPaths.get(k)));

        return openApi.paths(sortedPaths);
    }
}
