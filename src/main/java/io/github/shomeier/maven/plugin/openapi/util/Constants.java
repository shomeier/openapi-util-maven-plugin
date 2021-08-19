package io.github.shomeier.maven.plugin.openapi.util;

public final class Constants {

    private Constants() {}

    // This constant is used to temporarily store a ref to an external file on an OpenAPI Path
    // extension. It is only for internal use and will be not be written to the output file
    public static final String INTERNAL_REF_EXTENSION = "x-openapi-util-ref";
}
