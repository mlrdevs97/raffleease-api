package com.raffleease.raffleease.Domains.Images.Controller;

import com.raffleease.raffleease.Domains.Auth.Validations.ValidateAssociationAccess;
import com.raffleease.raffleease.Domains.Images.DTOs.ImageUpload;
import com.raffleease.raffleease.Domains.Images.Services.ImagesCreateService;
import com.raffleease.raffleease.Common.Responses.ApiResponse;
import com.raffleease.raffleease.Common.Responses.ResponseFactory;
import com.raffleease.raffleease.Domains.Images.Services.ImagesDeleteService;
import com.raffleease.raffleease.Domains.Auth.Validations.RequireRole;
import com.raffleease.raffleease.Domains.Images.Services.ImagesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.raffleease.raffleease.Common.RateLimiting.RateLimit;

import static com.raffleease.raffleease.Common.RateLimiting.RateLimit.AccessLevel.PRIVATE;
import static com.raffleease.raffleease.Domains.Associations.Model.AssociationRole.MEMBER;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@ValidateAssociationAccess
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/associations/{associationId}/images")
public class PendingImagesController {
    private final ImagesDeleteService deleteService;
    private final ImagesCreateService createService;
    private final ImagesService imagesService;

    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    @RequireRole(value = MEMBER, message = "Only administrators and members can upload images")
    @RateLimit(operation = "upload", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> upload(
            @PathVariable Long associationId,
            @Valid @ModelAttribute ImageUpload imageUpload
    ) {
        return ResponseEntity.ok(
                ResponseFactory.success(
                        createService.create(associationId, imageUpload),
                        "New images created successfully"
                )
        );
    }

    @GetMapping("/images")
    @RequireRole(value = MEMBER, message = "Only administrators and members can get pending images")
    @RateLimit(operation = "read", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> getUserImages() {
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        imagesService.getAllUserImages(),
                        "Images retrieved successfully"
                )
        );
    }

    @DeleteMapping("/{imageId}")
    @RequireRole(value = MEMBER, message = "Only administrators and members can delete images")
    @RateLimit(operation = "delete", accessLevel = PRIVATE)
    public ResponseEntity<Void> delete(
            @PathVariable Long imageId
    ) {
        deleteService.softDelete(imageId);
        return ResponseEntity.noContent().build();
    }
}