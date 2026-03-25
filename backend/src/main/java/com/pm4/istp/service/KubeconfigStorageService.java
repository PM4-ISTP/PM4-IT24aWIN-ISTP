package com.pm4.istp.service;

import com.pm4.istp.exception.StorageException;
import com.pm4.istp.util.FileStorageHandler;
import com.pm4.istp.util.StorageHandler;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KubeconfigStorageService {
  private final StorageHandler storageHandler = new FileStorageHandler();

  @Value("${k8s.kubeconfig.path}")
  private String kubeconfigPathString;

  private Path kubeconfigPath;

  @PostConstruct
  public void init() {
    kubeconfigPath = Path.of(kubeconfigPathString);
  }

  public void storeKubeconfig(MultipartFile kubeconfig) throws StorageException {
    Objects.requireNonNull(kubeconfig, "kubeconfig must not be null");
    try {
      storageHandler.store(kubeconfig, kubeconfigPath);
    } catch (StorageException e) {
      throw new StorageException(e.getMessage(), e);
    }
  }
}
