package com.pm4.istp.course.services;

import com.pm4.istp.admin.services.AdminConfigurationService;
import com.pm4.istp.course.validation.DockerImageReference;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DockerImageAvailabilityService {

  private static final String GHCR_PREFIX = "ghcr.io/";
  private static final Pattern GHCR_IMAGE_PATTERN =
      Pattern.compile(DockerImageReference.GHCR_IMAGE_REGEXP);
  private static final String MANIFEST_ACCEPT_HEADER =
      String.join(
          ", ",
          "application/vnd.oci.image.manifest.v1+json",
          "application/vnd.oci.image.index.v1+json",
          "application/vnd.docker.distribution.manifest.v2+json",
          "application/vnd.docker.distribution.manifest.list.v2+json");
  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("\"(?:token|access_token)\"\\s*:\\s*\"([^\"]+)\"");

  private final HttpClient httpClient;
  private final AdminConfigurationService adminConfigurationService;

  @Autowired
  public DockerImageAvailabilityService(AdminConfigurationService adminConfigurationService) {
    this(
        adminConfigurationService,
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
  }

  DockerImageAvailabilityService(
      AdminConfigurationService adminConfigurationService, HttpClient httpClient) {
    this.adminConfigurationService = adminConfigurationService;
    this.httpClient = httpClient;
  }

  public void assertImageExists(String imageReference) {
    checkImageAvailability(imageReference);
  }

  public DockerImageAvailabilityResult checkImageAvailability(String imageReference) {
    if (imageReference == null || !GHCR_IMAGE_PATTERN.matcher(imageReference).matches()) {
      throw new IllegalArgumentException(DockerImageReference.GHCR_IMAGE_MESSAGE);
    }

    GhcrImageReference reference = parseGhcrReference(imageReference);
    try {
      HttpResponse<Void> response = sendManifestRequest(reference.manifestUri(), null);
      if (response.statusCode() == 401) {
        Optional<String> token = fetchAnonymousBearerToken(response);
        if (token.isPresent()) {
          response = sendManifestRequest(reference.manifestUri(), token.get());
        }
      }

      int status = response.statusCode();
      if (status == 200) {
        return DockerImageAvailabilityResult.publicGhcrImage();
      }
      if (status == 401 || status == 403) {
        if (hasImagePullSecretConfigured()) {
          return DockerImageAvailabilityResult.privateGhcrImage();
        }
        throw new IllegalArgumentException(
            "Docker image must be public and anonymously readable. Private GHCR images are not "
                + "supported unless an image pull secret is configured: "
                + imageReference);
      }
      if (status == 404) {
        throw new IllegalArgumentException("Docker image does not exist: " + imageReference);
      }
      throw new IllegalArgumentException(
          "Could not verify Docker image "
              + imageReference
              + " because GHCR returned HTTP "
              + status);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not reach GHCR to verify Docker image.", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalArgumentException("Docker image verification was interrupted.", e);
    }
  }

  private HttpResponse<Void> sendManifestRequest(URI manifestUri, String bearerToken)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(manifestUri)
            .timeout(Duration.ofSeconds(5))
            .header("Accept", MANIFEST_ACCEPT_HEADER)
            .method("HEAD", HttpRequest.BodyPublishers.noBody());
    if (bearerToken != null) {
      builder.header("Authorization", "Bearer " + bearerToken);
    }
    return httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
  }

  private Optional<String> fetchAnonymousBearerToken(HttpResponse<Void> response)
      throws IOException, InterruptedException {
    Optional<String> authenticateHeader = response.headers().firstValue("WWW-Authenticate");
    if (authenticateHeader.isEmpty() || !authenticateHeader.get().startsWith("Bearer ")) {
      return Optional.empty();
    }

    String header = authenticateHeader.get();
    Optional<String> realm = bearerChallengeParameter(header, "realm");
    if (realm.isEmpty()) {
      return Optional.empty();
    }

    StringBuilder tokenUrl = new StringBuilder(realm.get());
    Optional<String> service = bearerChallengeParameter(header, "service");
    Optional<String> scope = bearerChallengeParameter(header, "scope");
    String separator = realm.get().contains("?") ? "&" : "?";
    if (service.isPresent()) {
      tokenUrl
          .append(separator)
          .append("service=")
          .append(URLEncoder.encode(service.get(), StandardCharsets.UTF_8));
      separator = "&";
    }
    if (scope.isPresent()) {
      tokenUrl
          .append(separator)
          .append("scope=")
          .append(URLEncoder.encode(scope.get(), StandardCharsets.UTF_8));
    }

    HttpRequest tokenRequest =
        HttpRequest.newBuilder(URI.create(tokenUrl.toString()))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();
    String body = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString()).body();
    Matcher matcher = TOKEN_PATTERN.matcher(body);
    return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
  }

  private Optional<String> bearerChallengeParameter(String header, String name) {
    Matcher matcher = Pattern.compile(name + "=\"([^\"]+)\"").matcher(header);
    return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
  }

  private boolean hasImagePullSecretConfigured() {
    return adminConfigurationService
        .getAdminConfiguration()
        .map(
            config ->
                config.getImagePullSecretName() != null
                    && !config.getImagePullSecretName().isBlank())
        .orElse(false);
  }

  private GhcrImageReference parseGhcrReference(String imageReference) {
    String pathAndTag = imageReference.substring(GHCR_PREFIX.length());
    int lastSlash = pathAndTag.lastIndexOf('/');
    int digestSeparator = pathAndTag.lastIndexOf("@sha256:");
    int tagSeparator = pathAndTag.lastIndexOf(':');

    String repository;
    String manifestReference;
    if (digestSeparator > lastSlash) {
      repository = pathAndTag.substring(0, digestSeparator);
      manifestReference = pathAndTag.substring(digestSeparator + 1);
    } else {
      repository = tagSeparator > lastSlash ? pathAndTag.substring(0, tagSeparator) : pathAndTag;
      manifestReference =
          tagSeparator > lastSlash ? pathAndTag.substring(tagSeparator + 1) : "latest";
    }

    return new GhcrImageReference(
        URI.create("https://ghcr.io/v2/" + repository + "/manifests/" + manifestReference));
  }

  private record GhcrImageReference(URI manifestUri) {}

  public record DockerImageAvailabilityResult(boolean privateImage) {
    public static DockerImageAvailabilityResult publicGhcrImage() {
      return new DockerImageAvailabilityResult(false);
    }

    public static DockerImageAvailabilityResult privateGhcrImage() {
      return new DockerImageAvailabilityResult(true);
    }
  }
}
