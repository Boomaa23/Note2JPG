package com.boomaa.note2jpg.dependencies;

import org.w3c.dom.Node;

public class MavenDependency {
    private final String groupId;
    private final String artifactId;
    private final String version;

    public MavenDependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    private String nodeToString(Node input) {
        return input.getTextContent().replaceAll("[\\t\\n]", "");
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "MavenDependency{" +
            "groupId='" + groupId + '\'' +
            ", artifactId='" + artifactId + '\'' +
            ", version='" + version + '\'' +
            '}';
    }
}
