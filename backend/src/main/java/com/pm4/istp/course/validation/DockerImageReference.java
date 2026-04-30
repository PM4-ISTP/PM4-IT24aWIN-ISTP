package com.pm4.istp.course.validation;

public final class DockerImageReference {

  public static final String GHCR_IMAGE_REGEXP = "^ghcr\\.io/[\\w.-]+/[\\w./-]+(:[\\w.-]+)?$";
  public static final String GHCR_IMAGE_MESSAGE =
      "Docker image must be a valid GHCR reference (e.g. ghcr.io/pm4-istp/test:latest)";

  private DockerImageReference() {}
}
