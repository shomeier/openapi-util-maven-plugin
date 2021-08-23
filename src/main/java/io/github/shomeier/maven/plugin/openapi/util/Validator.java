package io.github.shomeier.maven.plugin.openapi.util;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
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

    static void validateIncludedFiles(List<Path> includedFiles) throws MojoExecutionException {
        if (includedFiles.isEmpty()) {
            throw new MojoExecutionException("No resources found to process!");
        }
    }
}
