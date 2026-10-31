package com.socialpulse.backend.repository;

import com.socialpulse.backend.entity.UploadBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UploadBatchRepository extends JpaRepository<UploadBatch, Long> {

    List<UploadBatch> findByUserIdOrderByUploadedAtDesc(Long userId);

    /** Sahiplik kontrolü: başka kullanıcının yüklemesi bulunamaz. */
    Optional<UploadBatch> findByIdAndUserId(Long id, Long userId);
}
