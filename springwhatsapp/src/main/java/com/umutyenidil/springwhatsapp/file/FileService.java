package com.umutyenidil.springwhatsapp.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    @Value("${application.file.uploads.media-output-path}")
    private String fileUploadPath;

    public String saveFile(
            @NonNull MultipartFile sourceFile,
            @NonNull String senderId
    ) {
        final String fileUploadSubPath = "users" + File.separator + senderId;

        return uploadFile(sourceFile, senderId);
    }

    private String uploadFile(
            @NonNull MultipartFile sourceFile,
            @NonNull String fileUploadSubPath
    ) {
        final String uploadPath = fileUploadPath + File.separator + fileUploadSubPath;

        File targetFolder = new File(uploadPath);

        if (!targetFolder.exists()) {
            boolean folderCreated = targetFolder.mkdirs();
            if (!folderCreated) {
                log.warn("Could not create folder {}", targetFolder.getAbsolutePath());
                return null;
            }
        }

        final String fileExtension = getFileExtension(sourceFile.getOriginalFilename());
        final String filePath = fileUploadPath + File.separator + System.currentTimeMillis() + "." + fileExtension;

        Path targetPath = Paths.get(filePath);

        try {
            Files.write(targetPath, sourceFile.getBytes());
            log.info("File uploaded to {}", targetPath);
        } catch (IOException e) {
            log.error("File not saved", e);
        }

        return null;
    }

    private String getFileExtension(
            String filename
    ) {
        if (filename == null || filename.isEmpty()) return "";

        int lastDotIndex = filename.lastIndexOf('.');

        if (lastDotIndex == -1) return "";

        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
}
