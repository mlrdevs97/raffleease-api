package com.raffleease.raffleease.Domains.Images.Controller;

import com.raffleease.raffleease.Domains.Images.Services.ImagesService;
import com.raffleease.raffleease.Common.RateLimiting.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.raffleease.raffleease.Common.RateLimiting.RateLimit.AccessLevel.PUBLIC;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/public/associations/{associationId}")
public class PublicImagesController {
    private final ImagesService imagesService;

    @GetMapping("/images/{id}")
    @RateLimit(operation = "read", accessLevel = PUBLIC)
    public ResponseEntity<Resource> getTemp(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok().body(imagesService.getFile(id));
    }

    @GetMapping("/raffles/{raffleId}/images/{id}")
    @RateLimit(operation = "read", accessLevel = PUBLIC)
    public ResponseEntity<Resource> get(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok().body(imagesService.getFile(id));
    }
}
