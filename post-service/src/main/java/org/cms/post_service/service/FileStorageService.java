package org.cms.post_service.service;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;

@Service
public class FileStorageService {

    // Define the upload location (Relative to project root, or absolute path)
    private final Path fileStorageLocation;

    public FileStorageService() {
        // Defines the folder "uploads" in your project directory
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

        try {
            // Create directory if it doesn't exist
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    /**
     * Saves a file to the disk.
     * @param file The multipart file from the controller
     * @return The unique filename/path string to store in the database
     */
    public String saveFile(MultipartFile file) {
        // 1. Basic validation
        if (file == null || file.isEmpty()) {
            return null; // or throw exception depending on your logic
        }

        try {
            // 2. Sanitize and Unique Name Generation
            // Get extension (e.g. "jpg", "pdf") using Commons IO
            String originalFileName = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFileName);

            // Create UUID: "123e4567-e89b... .jpg"
            String uniqueFileName = UUID.randomUUID().toString() + (extension != null && !extension.isEmpty() ? "." + extension : "");

            // 3. Define the Target Path
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);

            // 4. Save the file (Overwrite if uuid collision exists, essentially impossible)
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // 5. Return the String path to be saved in DB
            // We return "uploads/uuid.jpg" usually to make it accessible
            return uniqueFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename() + ". Please try again!", ex);
        }
    }
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }
}
