package io.vertx.docgen;

/**
 * Artifact coordinates.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Coordinate {

  private final String groupId;
  private final String artifactId;
  private final String version;

  public Coordinate(String groupId, String artifactId, String version) {
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
}
