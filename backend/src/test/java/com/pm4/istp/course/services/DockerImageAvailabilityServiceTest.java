package com.pm4.istp.course.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pm4.istp.admin.db.AdminConfig;
import com.pm4.istp.admin.services.AdminConfigurationService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DockerImageAvailabilityServiceTest {

  private AdminConfigurationService adminConfigurationService;
  private HttpClient httpClient;
  private DockerImageAvailabilityService service;

  @BeforeEach
  void setUp() {
    adminConfigurationService = mock(AdminConfigurationService.class);
    httpClient = mock(HttpClient.class);
    service = new DockerImageAvailabilityService(adminConfigurationService, httpClient);
  }

  @Test
  void checkImageAvailability_publicManifest_returnsPublicResult() throws Exception {
    HttpResponse<Void> manifestResponse = response(200);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(manifestResponse);

    var result = service.checkImageAvailability("ghcr.io/acme/training/lab:latest");

    assertThat(result.privateImage()).isFalse();
  }

  @Test
  void checkImageAvailability_unauthorizedThenTokenSucceeds_sendsBearerToken() throws Exception {
    HttpResponse<Void> unauthorized =
        response(
            401,
            Map.of(
                "WWW-Authenticate",
                List.of(
                    "Bearer realm=\"https://tokens.example.test/auth\",service=\"ghcr.io\",scope=\"repository:acme/lab:pull\"")));
    HttpResponse<String> token = stringResponse("{\"token\":\"abc123\"}");
    HttpResponse<Void> success = response(200);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(
            unauthorized,
            token,
            success);

    var result =
        service.checkImageAvailability(
            "ghcr.io/acme/lab@sha256:abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd");

    assertThat(result.privateImage()).isFalse();
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    org.mockito.Mockito.verify(httpClient, org.mockito.Mockito.times(3))
        .send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
    assertThat(requestCaptor.getAllValues().get(2).headers().firstValue("Authorization"))
        .contains("Bearer abc123");
  }

  @Test
  void checkImageAvailability_privateImageWithPullSecret_returnsPrivateResult() throws Exception {
    AdminConfig config = new AdminConfig();
    config.setImagePullSecretName("ghcr-secret");
    when(adminConfigurationService.getAdminConfiguration()).thenReturn(Optional.of(config));
    HttpResponse<Void> forbidden = response(403);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(forbidden);

    var result = service.checkImageAvailability("ghcr.io/acme/lab:1.0.0");

    assertThat(result.privateImage()).isTrue();
  }

  @Test
  void checkImageAvailability_privateImageWithoutPullSecret_throws() throws Exception {
    when(adminConfigurationService.getAdminConfiguration()).thenReturn(Optional.empty());
    HttpResponse<Void> unauthorized = response(401);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(unauthorized);

    assertThatThrownBy(() -> service.checkImageAvailability("ghcr.io/acme/lab"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be public");
  }

  @Test
  void checkImageAvailability_notFoundAndUnexpectedStatus_throwHelpfulMessages() throws Exception {
    HttpResponse<Void> notFound = response(404);
    HttpResponse<Void> serverError = response(500);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(notFound, serverError);

    assertThatThrownBy(() -> service.checkImageAvailability("ghcr.io/acme/missing:latest"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not exist");

    assertThatThrownBy(() -> service.checkImageAvailability("ghcr.io/acme/broken:latest"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("HTTP 500");
  }

  @Test
  void checkImageAvailability_invalidImageAndIoAndInterrupt_throw() throws Exception {
    assertThatThrownBy(() -> service.assertImageExists("docker.io/library/alpine"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ghcr.io");

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new IOException("offline"))
        .thenThrow(new InterruptedException("stop"));

    assertThatThrownBy(() -> service.checkImageAvailability("ghcr.io/acme/lab:latest"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Could not reach GHCR");

    assertThatThrownBy(() -> service.checkImageAvailability("ghcr.io/acme/lab:latest"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("interrupted");
    assertThat(Thread.currentThread().isInterrupted()).isTrue();
    Thread.interrupted();
  }

  private static HttpResponse<Void> response(int status) {
    return response(status, Map.of());
  }

  private static HttpResponse<Void> response(int status, Map<String, List<String>> headers) {
    return new FakeHttpResponse<>(status, HttpHeaders.of(headers, (name, value) -> true), null);
  }

  private static HttpResponse<String> stringResponse(String body) {
    return new FakeHttpResponse<>(200, HttpHeaders.of(Map.of(), (name, value) -> true), body);
  }

  private record FakeHttpResponse<T>(int statusCode, HttpHeaders headers, T body)
      implements HttpResponse<T> {
    @Override
    public HttpRequest request() {
      return null;
    }

    @Override
    public Optional<HttpResponse<T>> previousResponse() {
      return Optional.empty();
    }

    @Override
    public Optional<SSLSession> sslSession() {
      return Optional.empty();
    }

    @Override
    public URI uri() {
      return URI.create("https://ghcr.io");
    }

    @Override
    public HttpClient.Version version() {
      return HttpClient.Version.HTTP_2;
    }
  }
}
