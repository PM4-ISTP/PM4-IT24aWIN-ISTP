package com.pm4.istp.course.validation;

public final class DockerImageReference {

  public static final String GHCR_IMAGE_REGEXP =
      "^ghcr\\.io/[\\w.-]+/[\\w./-]+((:[\\w.-]+)|(@sha256:[A-Fa-f0-9]{64}))?$";
  public static final String GHCR_IMAGE_MESSAGE =
      "Docker image must be a public GHCR reference "
          + "(e.g. ghcr.io/school-org/challenge:1.0.0 or "
          + "ghcr.io/school-org/challenge@sha256:<digest>)";

  private DockerImageReference() {}
}
