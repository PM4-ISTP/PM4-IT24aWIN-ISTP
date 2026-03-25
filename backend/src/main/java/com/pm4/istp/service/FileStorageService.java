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
  public void store(final MultipartFile file, final Path storeInFile) {
    Objects.requireNonNull(file, "file must not be null");
    Objects.requireNonNull(storeInFile, "storeAt must not be null");

    try (InputStream content = file.getInputStream()) {
      Files.createDirectories(storeInFile.getParent());
      Files.copy(content, storeInFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      final StringBuilder exceptionMessage = new StringBuilder("Could not store file\"");
      exceptionMessage.append(storeInFile.getFileName()).append("\": ").append(e.getMessage());
      throw new StorageException(exceptionMessage.toString(), e);
    }
  }
}
