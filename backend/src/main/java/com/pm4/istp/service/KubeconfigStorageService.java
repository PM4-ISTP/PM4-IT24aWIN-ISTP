package com.pm4.istp.service;

import com.pm4.istp.util.FileStorageHandler;
import com.pm4.istp.util.StorageHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Objects;

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

  public void storeKubeconfig(MultipartFile kubeconfig) {
    Objects.requireNonNull(kubeconfig, "kubeconfig must not be null");
    storageHandler.store(kubeconfig, kubeconfigPath);
  }
}
