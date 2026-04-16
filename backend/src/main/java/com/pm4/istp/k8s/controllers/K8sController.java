package com.pm4.istp.k8s.controllers;

import com.pm4.istp.k8s.dto.PodCreationRequest;
import com.pm4.istp.k8s.dto.PodCreationResponse;
import com.pm4.istp.k8s.exceptions.K8sException;
import com.pm4.istp.k8s.services.K8sService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
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
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @DeleteMapping("/pods/{instanceName}")
  public ResponseEntity<Void> deletePod(@PathVariable String instanceName) {
    k8sService.deletePod(instanceName);
    return ResponseEntity.noContent().build();
  }

  @ExceptionHandler(K8sException.class)
  public ResponseEntity<String> handleK8sException(K8sException ex) {
    log.error("Handling K8sException: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Kubernetes operation failed: " + ex.getMessage());
  }
}
