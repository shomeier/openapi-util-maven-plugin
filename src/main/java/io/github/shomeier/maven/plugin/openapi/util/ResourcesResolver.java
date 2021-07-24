package io.github.shomeier.maven.plugin.openapi.util;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.Scanner;

public class ResourcesResolver {

    private static final String[] EMPTY_STRING_ARRAY = {};

    private static final String[] DEFAULT_INCLUDES = {"**/**"};

    private final List<Resource> resources;
    private final MavenProject project;
    private Log log;

    public ResourcesResolver(List<Resource> resources, MavenProject project, Log log) {
        this.resources = resources;
        this.project = project;
        this.log = log;
    }

    List<Path> getIncludedFiles() {

        List<Path> includedFiles = new ArrayList<>();

        for (Resource resource : resources) {

            File resourceDirectory = new File(resource.getDirectory());

            if (!resourceDirectory.isAbsolute()) {
                resourceDirectory = new File(project.getBasedir(), resourceDirectory.getPath());
            }

            if (!resourceDirectory.exists()) {
                log.info("skip non existing resourceDirectory " + resourceDirectory.getPath());
                continue;
            }

            // Scanner scanner = buildContext.newScanner(resourceDirectory, true);
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(resourceDirectory);
            setupScanner(resource, scanner, true);
            scanner.scan();

            // for use in lambda below
            final File _resourceDirectory = resourceDirectory;
            List<Path> paths = Arrays.asList(scanner.getIncludedFiles()).stream()
                    .map(s -> java.nio.file.Paths.get(_resourceDirectory.getAbsolutePath(), s))
                    .collect(Collectors.toList());

            includedFiles.addAll(paths);
        }

        return includedFiles;
    }

    private String[] setupScanner(Resource resource, Scanner scanner, boolean addDefaultExcludes) {
        String[] includes = null;
        if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
            includes = resource.getIncludes().toArray(EMPTY_STRING_ARRAY);
        } else {
            includes = DEFAULT_INCLUDES;
        }
        scanner.setIncludes(includes);

        String[] excludes = null;
        if (resource.getExcludes() != null && !resource.getExcludes().isEmpty()) {
            excludes = resource.getExcludes().toArray(EMPTY_STRING_ARRAY);
            scanner.setExcludes(excludes);
        }

        if (addDefaultExcludes) {
            scanner.addDefaultExcludes();
        }
        return includes;
    }
}
