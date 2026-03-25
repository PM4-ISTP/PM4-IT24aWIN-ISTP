package com.pm4.istp.util;

import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

public interface StorageHandler {
  void store(MultipartFile file, Path storeInFile);
}
