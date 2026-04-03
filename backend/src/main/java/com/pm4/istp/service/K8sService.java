package com.pm4.istp.service;

import com.pm4.istp.dto.PodCreationRequest;
import com.pm4.istp.dto.PodCreationResponse;
import com.pm4.istp.exception.K8sException;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class K8sService {

  @Value("${k8s.kubeconfig.path}")
  private String kubeconfigPath;

  @Value("${k8s.default.namespace}")
  private String defaultNamespace;

  @Value("${istp.domain}")
  private String domain;

  @Value("${istp.tls}")
  private boolean tls;

  public PodCreationResponse createPod(PodCreationRequest request) {
    try {
      File kubeconfigFile = new File(kubeconfigPath);
      if (!kubeconfigFile.exists()) {
        throw new K8sException("Kubeconfig file not found at: " + kubeconfigPath);
      }
      String kubeconfigContent = Files.readString(kubeconfigFile.toPath());
      Config config = Config.fromKubeconfig(kubeconfigContent);

      try (KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build()) {

        String deploymentId = UUID.randomUUID().toString().substring(0, 8);
        String instanceName = "challenge-" + deploymentId;
        String terminalPassword = UUID.randomUUID().toString().substring(0, 12);

        Map<String, String> labels = Map.of("app", instanceName);

        // 1. Create Deployment
        Deployment deployment =
            new DeploymentBuilder()
                .withNewMetadata()
                .withName(instanceName)
                .withNamespace(defaultNamespace)
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewSelector()
                .withMatchLabels(labels)
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                // App Container
                .addNewContainer()
                .withName(
                    (request.getContainerName() != null && !request.getContainerName().isBlank())
                        ? request.getContainerName()
                        : "app")
                .withImage(request.getImage())
                .addNewPort()
                .withContainerPort(request.getContainerPort())
                .endPort()
                .endContainer()
                // Terminal Sidecar Container
                .addNewContainer()
                .withName("terminal")
                .withImage("tsl0922/ttyd:1.7.7")
                .withArgs("ttyd", "-W", "-c", "student:" + terminalPassword, "sh")
                .addNewPort()
                .withContainerPort(7681)
                .endPort()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        client.apps().deployments().inNamespace(defaultNamespace).resource(deployment).create();

        // 2. Create Service
        io.fabric8.kubernetes.api.model.Service service =
            new ServiceBuilder()
                .withNewMetadata()
                .withName(instanceName + "-svc")
                .withNamespace(defaultNamespace)
                .endMetadata()
                .withNewSpec()
                .withSelector(labels)
                .addNewPort()
                .withName("app-port")
                .withProtocol("TCP")
                .withPort(80)
                .withTargetPort(new IntOrString(request.getContainerPort()))
                .endPort()
                .addNewPort()
                .withName("term-port")
                .withProtocol("TCP")
                .withPort(8081)
                .withTargetPort(new IntOrString(7681))
                .endPort()
                .withType("ClusterIP")
                .endSpec()
                .build();

        client.services().inNamespace(defaultNamespace).resource(service).create();

        // 3. Create Ingress
        String appHost = "app-" + deploymentId + "." + domain;
        String termHost = "term-" + deploymentId + "." + domain;

        Ingress ingress =
            new IngressBuilder()
                .withNewMetadata()
                .withName(instanceName + "-ingress")
                .withNamespace(defaultNamespace)
                .endMetadata()
                .withNewSpec()
                .addNewRule()
                .withHost(appHost)
                .withNewHttp()
                .addNewPath()
                .withPath("/")
                .withPathType("Prefix")
                .withNewBackend()
                .withNewService()
                .withName(service.getMetadata().getName())
                .withNewPort()
                .withNumber(80)
                .endPort()
                .endService()
                .endBackend()
                .endPath()
                .endHttp()
                .endRule()
                .addNewRule()
                .withHost(termHost)
                .withNewHttp()
                .addNewPath()
                .withPath("/")
                .withPathType("Prefix")
                .withNewBackend()
                .withNewService()
                .withName(service.getMetadata().getName())
                .withNewPort()
                .withNumber(8081)
                .endPort()
                .endService()
                .endBackend()
                .endPath()
                .endHttp()
                .endRule()
                .endSpec()
                .build();

        client.network().v1().ingresses().inNamespace(defaultNamespace).resource(ingress).create();

        log.info("Resources created successfully for deploymentId: {}", deploymentId);

        String appUrl = (tls ? "https" : "http") + "://" + appHost;
        String terminalUrl = (tls ? "https" : "http") + "://" + termHost;

        return new PodCreationResponse(
            "CREATED",
            instanceName,
            defaultNamespace,
            "Deployment, Service, and Ingress created successfully",
            appUrl,
            terminalUrl,
            terminalPassword);
      }
    } catch (K8sException e) {
      log.error("K8s operation failed: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error while creating k8s resources", e);
      throw new K8sException("Failed to create k8s resources: " + e.getMessage(), e);
    }
  }

  public void deletePod(String instanceName) {
    try {
      File kubeconfigFile = new File(kubeconfigPath);
      if (!kubeconfigFile.exists()) {
        throw new K8sException("Kubeconfig file not found at: " + kubeconfigPath);
      }
      String kubeconfigContent = Files.readString(kubeconfigFile.toPath());
      Config config = Config.fromKubeconfig(kubeconfigContent);

      try (KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build()) {
        client.apps().deployments().inNamespace(defaultNamespace).withName(instanceName).delete();
        client.services().inNamespace(defaultNamespace).withName(instanceName + "-svc").delete();
        client
            .network()
            .v1()
            .ingresses()
            .inNamespace(defaultNamespace)
            .withName(instanceName + "-ingress")
            .delete();
        log.info("Resources deleted successfully for: {}", instanceName);
      }
    } catch (K8sException e) {
      log.error("K8s operation failed: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error(
          "Unexpected error while deleting k8s resources for instance {}: {}",
          instanceName,
          e.getMessage(),
          e);
      throw new K8sException("Failed to delete k8s resources: " + e.getMessage(), e);
    }
  }
}
