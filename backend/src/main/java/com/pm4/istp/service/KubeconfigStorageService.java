package com.pm4.istp.service;

import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KubeconfigStorageService {

  private final FileStorageService fileStorageService;

  @Value("${k8s.kubeconfig.path}")
  private String kubeconfigPathString;

  private Path kubeconfigPath;

  public KubeconfigStorageService(FileStorageService fileStorageService) {
    this.fileStorageService =
        Objects.requireNonNull(fileStorageService, "fileStorageService must not be null");
  }

  @PostConstruct
  public void init() {
    kubeconfigPath = Path.of(kubeconfigPathString);
  }

  public String storeKubeconfig(MultipartFile kubeconfig) {
    Objects.requireNonNull(kubeconfig, "kubeconfig must not be null");
    fileStorageService.store(kubeconfig, kubeconfigPath);
    return kubeconfigPath.toString();
  }
}
