package com.pm4.istp.service;

import com.pm4.istp.exception.StorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import lombok.NonNull;
import org.springframework.stereotype.Service;

@Service
public class FileStorageService {
  public void store(@NonNull final byte[] content, @NonNull final Path targetPath) {
    Objects.requireNonNull(content, "content must not be null");
    Objects.requireNonNull(targetPath, "targetPath must not be null");

    try {
      Path parent = targetPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      Files.write(targetPath, content);
    } catch (IOException e) {
      throw new StorageException(
          "Could not store file at path \"" + targetPath + "\": " + e.getMessage(), e);
    }
  }
}
