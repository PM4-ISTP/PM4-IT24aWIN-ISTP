package com.pm4.istp.service;

import com.pm4.istp.dto.PodCreationRequest;
import com.pm4.istp.dto.PodCreationResponse;
import com.pm4.istp.exception.K8sException;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class K8sService {

  private static final Logger LOG = LoggerFactory.getLogger(K8sService.class);

  @Value("${k8s.kubeconfig.path}")
  private String kubeconfigPath;

  @Value("${k8s.default.namespace}")
  private String defaultNamespace;

  private final K8sTemplateService templateService;

  public K8sService(K8sTemplateService templateService) {
    this.templateService = templateService;
  }

  public PodCreationResponse createPod(PodCreationRequest request) {
    try {
      File kubeconfigFile = new File(kubeconfigPath);
      if (!kubeconfigFile.exists()) {
        throw new K8sException("Kubeconfig file not found at: " + kubeconfigPath);
      }
      String kubeconfigContent = Files.readString(kubeconfigFile.toPath());

      KubernetesClient client = new KubernetesClientBuilder().withConfig(kubeconfigContent).build();
      String podYaml = templateService.processPodTemplate(request);
      ByteArrayInputStream yamlStream =
          new ByteArrayInputStream(podYaml.getBytes(StandardCharsets.UTF_8));
      Pod pod = client.pods().inNamespace(defaultNamespace).load(yamlStream).create();

      String podName = pod.getMetadata().getName();

      LOG.info("Pod created successfully: {}", podName);
      return new PodCreationResponse(
          "CREATED", podName, defaultNamespace, "Pod created successfully");
    } catch (K8sException e) {
      LOG.error("K8s operation failed: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      LOG.error("Unexpected error while creating pod", e);
      throw new K8sException("Failed to create pod: " + e.getMessage(), e);
    }
  }
}
