package com.pm4.istp.service;

import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KubeconfigStorageService {

  private final FileStorageService fileStorageService;

  @Value("${k8s.kubeconfig.path}")
  private String kubeconfigPathString;

  private Path kubeconfigPath;

  public KubeconfigStorageService(@NonNull FileStorageService fileStorageService) {
    this.fileStorageService = fileStorageService;
  }

  @PostConstruct
  public void init() {
    kubeconfigPath = Path.of(kubeconfigPathString);
  }

  public String storeKubeconfig(@NonNull byte[] kubeconfig) {
    fileStorageService.store(kubeconfig, kubeconfigPath);
    return kubeconfigPath.toString();
  }
}
