package com.socialpulse.backend.controller;

import com.socialpulse.backend.auth.AuthenticatedUser;
import com.socialpulse.backend.dto.UploadResponse;
import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @RequestParam("platform") Platform platform,
                                                 @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(uploadService.upload(user.id(), platform, file));
    }

    @GetMapping
    public List<UploadResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return uploadService.list(user.id());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable Long id) {
        uploadService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
