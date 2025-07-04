package com.raffleease.raffleease.Domains.Images.Services;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.util.List;

public interface FileStorageService {
    /*
     * Move files from pending directory to the final raffle directory.
     * The file is moved to the raffle directory and the file name is updated to include the raffle ID.
     * Used in the context of the creation of a new raffle to move the files from the pending directory to the final raffle directory when the raffle creation process is completed.
     *
     * @param associationId Association ID
     * @param raffleId Raffle ID
     * @param imageId Image ID
     * @param currentPath Path of the current file (can be temp or pending)
     * @return Path of the final file
     */
    Path moveFileToRaffle(String associationId, String raffleId, String imageId, String tempPath);

    /*
     * Load file from the file system by its path.
     *
     * @param filePath Path of the file to load
     * @return Resource of the file
     */
    Resource load(String filePath);

    /*
     * Delete file from the file system by its path.
     *
     * @param filePath Path of the file to delete
     */
    void delete(String filePath);

    /*
     * Save temporary batch of files in the file system.
     * If the batch save fails, the temporary files will be cleaned up.
     * Used in the context of the creation of a new raffle to save the files in a temporary directory when the raffle creation process is completed.
     *
     * @param files List of files to save
     * @param associationId Association ID
     * @param batchId Batch ID
     * @return List of temporary file paths
     */
    List<String> saveTemporaryBatch(List<MultipartFile> files, String associationId, String batchId);

    /*
     * Move temporary batch of files from the temporary directory to the final raffle directory.
     * If the batch move fails, the temporary files will be cleaned up.
     * Used in the context of the creation of a new raffle to move the files from the temporary directory to the final raffle directory when the raffle creation process is completed.
     *
     * @param tempPaths List of temporary file paths to move
     * @param associationId Association ID
     * @param raffleId Raffle ID
     * @param imageIds List of image IDs
     * @return List of final file paths
     */
    List<String> moveTemporaryBatchToFinal(List<String> tempPaths, String associationId, String raffleId, List<String> imageIds);

    /*
     * Cleanup temporary files from the file system.
     *
     * @param tempPaths List of temporary file paths to cleanup
     */
    void cleanupTemporaryFiles(List<String> tempPaths);

    /*
     * Cleanup files from the file system.
     *
     * @param filePaths List of file paths to cleanup
     */
    void cleanupFiles(List<String> filePaths);
}
