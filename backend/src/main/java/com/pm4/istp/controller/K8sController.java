package com.pm4.istp.controller;

import com.pm4.istp.dto.PodCreationRequest;
import com.pm4.istp.dto.PodCreationResponse;
import com.pm4.istp.exception.K8sException;
import com.pm4.istp.service.K8sService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/k8s")
public class K8sController {

  private final K8sService k8sService;

  public K8sController(K8sService k8sService) {
    this.k8sService = k8sService;
  }

  @PostMapping("/pods")
  public ResponseEntity<PodCreationResponse> createPod(
      @Valid @RequestBody PodCreationRequest request) {
    PodCreationResponse response = k8sService.createPod(request);
    return ResponseEntity.ok(response);
  }

  @ExceptionHandler(K8sException.class)
  public ResponseEntity<String> handleK8sException(K8sException ex) {
    log.error("Handling K8sException: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Kubernetes operation failed: " + ex.getMessage());
  }
}
