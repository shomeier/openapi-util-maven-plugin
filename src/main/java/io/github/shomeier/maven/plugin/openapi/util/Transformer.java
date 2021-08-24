package io.github.shomeier.maven.plugin.openapi.util;

import java.util.function.Consumer;
import io.swagger.v3.oas.models.OpenAPI;
import pl.joegreen.lambdaFromString.LambdaCreationException;
import pl.joegreen.lambdaFromString.LambdaFactory;
import pl.joegreen.lambdaFromString.LambdaFactoryConfiguration;
import pl.joegreen.lambdaFromString.TypeReference;

public class Transformer {

    private String lambda;
    private String imports;

    public String getLambda() {
        return lambda;
    }

    public void setLambda(String lambda) {
        this.lambda = lambda;
    }

    public String getImports() {
        return imports;
    }

    public void setImports(String imports) {
        this.imports = imports;
    }

    public void transform(OpenAPI openApi, String cp) throws LambdaCreationException {

        LambdaFactoryConfiguration conf = LambdaFactoryConfiguration.get()
                .withImports(imports.split(","))
                .withCompilationClassPath(cp);
        LambdaFactory lambdaFactory = LambdaFactory.get(conf);

        Consumer<OpenAPI> consumer =
                lambdaFactory.createLambda(this.lambda, new TypeReference<Consumer<OpenAPI>>() {});
        consumer.accept(openApi);
    }
}
