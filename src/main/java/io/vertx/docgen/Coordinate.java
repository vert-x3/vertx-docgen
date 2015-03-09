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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Coordinate[");
    if (groupId != null) {
      sb.append("groupId:").append(groupId);
    }
    if (artifactId != null) {
      if (groupId != null) {
        sb.append(",");
      }
      sb.append("artifactId:").append(artifactId);
    }
    if (version != null) {
      if (groupId != null || artifactId != null) {
        sb.append(",");
      }
      sb.append("version:").append(version);
    }
    sb.append("]");
    return sb.toString();
  }
}
