package com.pm4.istp.service;

import com.pm4.istp.exception.StorageException;
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
    Objects.requireNonNull(fileStorageService, "fileStorageHandler must not be null");
    this.fileStorageService = fileStorageService;
  }

  @PostConstruct
  public void init() {
    kubeconfigPath = Path.of(kubeconfigPathString);
  }

  public void storeKubeconfig(MultipartFile kubeconfig) throws StorageException {
    Objects.requireNonNull(kubeconfig, "kubeconfig must not be null");
    try {
      fileStorageService.store(kubeconfig, kubeconfigPath);
    } catch (StorageException e) {
      throw new StorageException(e.getMessage(), e);
    }
  }
}
