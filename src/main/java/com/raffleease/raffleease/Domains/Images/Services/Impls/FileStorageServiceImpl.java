package com.raffleease.raffleease.Domains.Images.Services.Impls;

import com.raffleease.raffleease.Domains.Images.Services.FileStorageService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {
    @Value("${spring.storage.images.base_path}")
    private String basePath;

    @Override
    public Path moveFileToRaffle(String associationId, String raffleId, String imageId, String currentPath) {
        try {
            Path currentFile = Paths.get(currentPath);
            if (!Files.exists(currentFile)) {
                throw new FileStorageException("Source file not found: " + currentPath);
            }
            String originalFileName = extractOriginalFileNameForSingleMove(currentFile.getFileName().toString());
            Path finalDir = Paths.get(basePath, "associations", associationId, "raffles", raffleId, "images");
            Files.createDirectories(finalDir);
            String finalFileName = imageId + "_" + originalFileName;
            Path finalFilePath = finalDir.resolve(finalFileName);
            Files.move(currentFile, finalFilePath);
            return finalFilePath;
        } catch (IOException e) {
            throw new FileStorageException("Failed to move file to raffle: " + e.getMessage());
        }
    }

    @Override
    public Resource load(String filePath) {
        try {
            Path path = Paths.get(filePath).normalize();

            if (!Files.exists(path)) {
                throw new FileStorageException("File not found: " + filePath);
            }

            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException("Could not read file: " + filePath);
            }

            return resource;
        } catch (MalformedURLException e) {
            throw new FileStorageException("Malformed URL for file: " + filePath);
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException ex) {
            throw new FileStorageException("Failed to delete file: " + ex.getMessage());
        }
    }

    @Override
    public List<String> saveTemporaryBatch(List<MultipartFile> files, String associationId, String batchId) {
        List<String> tempPaths = new ArrayList<>();
        Path tempDirectory;
        
        try {
            tempDirectory = Paths.get(basePath, "associations", associationId, "images", "temp", batchId);
            Files.createDirectories(tempDirectory);
            
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String tempFileName = "temp_" + i + "_" + file.getOriginalFilename();
                Path tempFilePath = tempDirectory.resolve(tempFileName);
                
                try {
                    Files.write(tempFilePath, file.getBytes());
                    tempPaths.add(tempFilePath.toString());
                } catch (IOException e) {
                    cleanupTemporaryFiles(tempPaths);
                    throw new FileStorageException("Failed to save file " + tempFileName + ": " + e.getMessage());
                }
            }
            return tempPaths;
        } catch (IOException e) {
            cleanupTemporaryFiles(tempPaths);
            throw new FileStorageException("Failed to create temporary directory: " + e.getMessage());
        }
    }

    @Override
    public List<String> moveTemporaryBatchToFinal(List<String> tempPaths, String associationId, String raffleId, List<String> imageIds) {
        if (tempPaths.size() != imageIds.size()) {
            throw new IllegalArgumentException("Number of temp paths must match number of image IDs");
        }
        
        List<String> finalPaths = new ArrayList<>();
        Path finalDirectory;
        
        try {
            finalDirectory = raffleId != null ? 
                Paths.get(basePath, "associations", associationId, "raffles", raffleId, "images") :
                Paths.get(basePath, "associations", associationId, "images", "pending");
            Files.createDirectories(finalDirectory);
            
            for (int i = 0; i < tempPaths.size(); i++) {
                Path tempFile = Paths.get(tempPaths.get(i));
                String originalFileName = extractOriginalFileName(tempFile.getFileName().toString());
                String finalFileName = imageIds.get(i) + "_" + originalFileName;
                Path finalFilePath = finalDirectory.resolve(finalFileName);
                
                try {
                    Files.move(tempFile, finalFilePath);
                    finalPaths.add(finalFilePath.toString());
                } catch (IOException e) {
                    log.error("Failed to move file from {} to {}, initiating complete rollback", tempFile, finalFilePath, e);
                    rollbackCompletelyOnFailure(finalPaths, tempPaths);
                    throw new FileStorageException("Failed to move file " + tempFile.getFileName() + ": " + e.getMessage());
                }
            }
            
            cleanupEmptyDirectory(tempPaths.get(0));
            return finalPaths;
        } catch (IOException e) {
            cleanupTemporaryFiles(tempPaths);
            throw new FileStorageException("Failed to create final directory: " + e.getMessage());
        }
    }

    @Override
    public void cleanupTemporaryFiles(List<String> tempPaths) {
        for (String tempPath : tempPaths) {
            try {
                Path path = Paths.get(tempPath);
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Failed to cleanup temporary file: {}", tempPath, e);
            }
        }
    }

    @Override
    public void cleanupFiles(List<String> filePaths) {
        for (String filePath : filePaths) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
                log.debug("Cleaned up file: {}", filePath);
            } catch (IOException e) {
                log.warn("Failed to cleanup file: {}", filePath, e);
            }
        }
    }

    /*
     * Extract original file name from temporary file name
     * 
     * @param tempFileName Temporary file name
     * @return Original file name
     */
    private String extractOriginalFileName(String tempFileName) {
        int lastUnderscoreIndex = tempFileName.indexOf('_', tempFileName.indexOf('_') + 1);
        return lastUnderscoreIndex != -1 ? tempFileName.substring(lastUnderscoreIndex + 1) : tempFileName;
    }

    /*
     * Rollback completely on failure
     * 
     * @param successfullyMovedPaths List of paths that were successfully moved
     * @param allTempPaths List of all temporary paths
     */
    private void rollbackCompletelyOnFailure(List<String> successfullyMovedPaths, List<String> allTempPaths) {
        // Clean up successfully moved files
        cleanupFiles(successfullyMovedPaths);
        log.debug("Cleaned up {} successfully moved files", successfullyMovedPaths.size());
        
        // Clean up ALL temp files (including ones that weren't processed yet)
        cleanupTemporaryFiles(allTempPaths);
        log.debug("Cleaned up all {} temporary files", allTempPaths.size());
    }

    /*
     * Cleanup empty directory
     * 
     * @param filePath Path of the directory to cleanup
     */
    private void cleanupEmptyDirectory(String filePath) {
        try {
            Path directory = Paths.get(filePath).getParent();
            if (directory != null && Files.exists(directory) && isDirectoryEmpty(directory)) {
                Files.delete(directory);
            }
        } catch (IOException e) {
            log.debug("Could not cleanup directory (may not be empty): {}", Paths.get(filePath).getParent());
        }
    }

    /*
     * Check if directory is empty
     * 
     * @param directory Path of the directory to check
     * @return True if directory is empty, false otherwise
     */
    private boolean isDirectoryEmpty(Path directory) {
        try {
            return Files.list(directory).findAny().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    /*
     * Extract original file name for single move
     * Handles both temp file format (temp_0_filename.jpg) and pending file format (123_filename.jpg)
     * 
     * @param fileName Current file name
     * @return Original file name
     */
    private String extractOriginalFileNameForSingleMove(String fileName) {
        int underscoreIndex = fileName.indexOf('_');
        if (underscoreIndex != -1) {
            // Check if it's temp format (temp_0_filename.jpg) or pending format (123_filename.jpg)
            if (fileName.startsWith("temp_")) {
                // For temp format: temp_0_filename.jpg -> temp_0_ then extract filename
                int secondUnderscoreIndex = fileName.indexOf('_', underscoreIndex + 1);
                return secondUnderscoreIndex != -1 ? fileName.substring(secondUnderscoreIndex + 1) : fileName;
            } else {
                // For pending format: 123_filename.jpg -> extract filename
                return fileName.substring(underscoreIndex + 1);
            }
        }
        return fileName;
    }
}