package com.pm4.istp.controller;

import com.pm4.istp.exception.StorageException;
import com.pm4.istp.service.KubeconfigStorageService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/api/kubeconfig")
public class KubeconfigUploadController {
  private final KubeconfigStorageService kubeconfigStorageService;

  public KubeconfigUploadController(KubeconfigStorageService kubeconfigStorageService) {
    Objects.requireNonNull(kubeconfigStorageService, "kubeconfigStorageService must not be null");
    this.kubeconfigStorageService = kubeconfigStorageService;
  }

  @PostMapping
  public ResponseEntity<String> uploadKubeconfig(
      @RequestParam("kubeconfig") MultipartFile kubeconfig) {
    kubeconfigStorageService.storeKubeconfig(kubeconfig);
    return ResponseEntity.accepted().body("Kubeconfig successfully uploaded");
  }

  @ExceptionHandler(StorageException.class)
  public ResponseEntity<String> handleStorageException(StorageException storageException) {
    return ResponseEntity.internalServerError().body(storageException.getMessage());
  }
}
