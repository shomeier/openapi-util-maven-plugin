package io.github.shomeier.maven.plugin.openapi.util;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import java.util.Optional;
import java.util.StringJoiner;

public class DependencyArtifactResolver {

    private final String PUGIN_KEY = "io.github.shomeier:openapi-util-maven-plugin";
    private final MavenProject project;
    private final Settings settings;

    public DependencyArtifactResolver(MavenProject project, Settings settings) {
        this.project = project;
        this.settings = settings;
    }

    public String buildClassPathFromDependencies() {

        final String localRepo = getLocalRepositoryBasePath();

        String classpath = System.getProperty("java.class.path");
        StringJoiner compilationClassPathJoiner = new StringJoiner(":")
                .add(classpath);

        Optional.ofNullable(project.getBuild()).ifPresent(b -> {

            Plugin thisPlugin = b.getPluginsAsMap().get(PUGIN_KEY);
            thisPlugin.getDependencies().forEach(d -> {

                Artifact artifact = buildArtifact(d);
                for (ArtifactRepository repo : project.getRemoteArtifactRepositories()) {
                    Artifact foundArtifact = repo.find(artifact);
                    if(foundArtifact != null) {
                        Optional.ofNullable(foundArtifact.getFile()).ifPresent(f -> compilationClassPathJoiner.add(localRepo + f.getPath()));
                        break;
                    }
                }
            });
        });

        return compilationClassPathJoiner.toString();
    }

    private Artifact buildArtifact(Dependency d) {
        return new DefaultArtifact(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getScope(), d.getType(),
                d.getClassifier(), new DefaultArtifactHandler(d.getType()));
    }

    private String getLocalRepositoryBasePath() {
        if (settings.getLocalRepository() != null) {
            return settings.getLocalRepository();
        }
        return System.getProperty("user.home") + "/.m2/repository";
    }
}
