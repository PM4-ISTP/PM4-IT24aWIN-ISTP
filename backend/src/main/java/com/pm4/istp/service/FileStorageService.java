package com.pm4.istp.service;

import com.pm4.istp.exception.StorageException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

  public void store(final MultipartFile file, final Path targetPath) {
    Objects.requireNonNull(file, "file must not be null");
    Objects.requireNonNull(targetPath, "targetPath must not be null");

    try (InputStream content = file.getInputStream()) {
      Path parent = targetPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      Files.copy(content, targetPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new StorageException(
          "Could not store file at path \"" + targetPath + "\": " + e.getMessage(), e);
    }
  }
}
