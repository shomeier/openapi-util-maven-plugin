package io.github.shomeier.maven.plugin.openapi.util;

import java.util.function.UnaryOperator;
import io.swagger.v3.oas.models.OpenAPI;
import pl.joegreen.lambdaFromString.LambdaCreationException;
import pl.joegreen.lambdaFromString.LambdaFactory;
import pl.joegreen.lambdaFromString.LambdaFactoryConfiguration;
import pl.joegreen.lambdaFromString.TypeReference;

public class Transformer {

    private String lambda;
    private String imports;

    public String getLambdaString() {
        return lambda;
    }

    public void setLambdaString(String lambdaString) {
        this.lambda = lambdaString;
    }

    public String getImports() {
        return imports;
    }

    public void setImports(String imports) {
        this.imports = imports;
    }

    public OpenAPI transform(OpenAPI openApi) throws LambdaCreationException {

        LambdaFactoryConfiguration conf = LambdaFactoryConfiguration.get()
                .withImports(imports.split(","));
        LambdaFactory lambdaFactory = LambdaFactory.get(conf);

        UnaryOperator<OpenAPI> unaryOperator =
                lambdaFactory.createLambda(this.lambda, new TypeReference<UnaryOperator<OpenAPI>>() {});

        return unaryOperator.apply(openApi);
    }
}
