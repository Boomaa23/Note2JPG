package com.boomaa.note2jpg.updater;

public class MavenDependency {
    private final String groupId;
    private final String artifactId;
    private final String version;

    public MavenDependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
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

    public String getJarName() {
        return this.artifactId + "-" + this.version + ".jar";
    }

    @Override
    public String toString() {
        return groupId + ':' + artifactId + ':' + version;
    }
}
