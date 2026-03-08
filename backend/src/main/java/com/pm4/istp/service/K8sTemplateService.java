package com.pm4.istp.service;

import com.pm4.istp.dto.PodCreationRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class K8sTemplateService {

  private final ResourceLoader resourceLoader;

  public K8sTemplateService(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public String processPodTemplate(PodCreationRequest request) throws IOException {
    Resource resource = resourceLoader.getResource("classpath:k8s-templates/pod-template.yaml");
    String templateContent =
        new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

    Map<String, String> replacements = createReplacementMap(request);
    String processedTemplate = replacePlaceholders(templateContent, replacements);

    return processedTemplate;
  }

  private Map<String, String> createReplacementMap(PodCreationRequest request) {
    Map<String, String> replacements = new HashMap<>();
    replacements.put("${podName}", request.getPodName());
    replacements.put("${containerName}", request.getContainerName());
    replacements.put("${image}", request.getImage());
    replacements.put("${namespace}", "default");
    return replacements;
  }

  private String replacePlaceholders(String template, Map<String, String> replacements) {
    String result = template;
    for (Map.Entry<String, String> entry : replacements.entrySet()) {
      result = result.replace(entry.getKey(), entry.getValue());
    }
    return result;
  }
}
