package io.github.shomeier.maven.plugin.openapi.util;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;

public class Validator {

    static void validateFile(File file, String paramName) throws MojoExecutionException {
        validateParam(file, "paramName");
        if (!file.exists()) {
            throw new MojoExecutionException(
                    String.format("The specified %s '%s' does not exist!", paramName, file));
        }
    }

    static void validateParam(Object param, String paramName) throws MojoExecutionException {
        if (param == null) {
            throw new MojoExecutionException(
                    String.format("The parameter '%s' is not set!", paramName));
        }
    }
}
