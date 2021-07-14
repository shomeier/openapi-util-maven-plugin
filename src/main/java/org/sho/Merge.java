package org.sho;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = Merge.GOAL, defaultPhase = LifecyclePhase.INITIALIZE)
public class Merge extends AbstractMojo {
    public static final String GOAL = "merge";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(required = true)
    private File headerFile;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources", required = true)
    private File inputDirectory;

    @Parameter(required = true)
    private File outputFile;

    public void execute() throws MojoExecutionException {
        validateParameters();

        try {
            FileUtils.copyFile(headerFile, outputFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while merging", e);
        }

    }

    private void validateParameters() throws MojoExecutionException {
        validateFile(headerFile, "headerFile");

        validateDirectory(inputDirectory, "inputDirectory");
        if (inputDirectory.listFiles().length == 0) {
            throw new MojoExecutionException(
                    String.format("The specified inputDirectory '%s' is empty!", inputDirectory));
        }

        validateParam(outputFile, "outputFile");
    }

    private void validateFile(File file, String paramName) throws MojoExecutionException {
        validateParam(file, "paramName");
        if (!headerFile.exists()) {
            throw new MojoExecutionException(
                    String.format("The specified %s '%s' does not exist!", paramName, file));
        }
    }

    private void validateDirectory(File directory, String paramName)
            throws MojoExecutionException {

        validateParam(directory, paramName);
        if (!directory.exists()) {
            throw new MojoExecutionException(
                    String.format("The specified %s '%s' does not exist!", paramName, directory));
        }
        if (!directory.isDirectory()) {
            throw new MojoExecutionException(
                    String.format("The specified %s '%s' is not a directory!", paramName, directory));
        }
    }

    private void validateParam(Object param, String paramName) throws MojoExecutionException {
        if (param == null) {
            throw new MojoExecutionException(
                    String.format("The parameter '%s' is not set!", paramName));
        }
    }

}
