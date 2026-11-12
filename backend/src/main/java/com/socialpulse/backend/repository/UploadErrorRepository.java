package com.socialpulse.backend.repository;

import com.socialpulse.backend.entity.UploadError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadErrorRepository extends JpaRepository<UploadError, Long> {

    List<UploadError> findByBatchIdOrderByIdAsc(Long batchId);
}
