package com.socialpulse.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bir yüklemede atlanan satır (ERROR) veya veri kalitesi uyarısı (WARNING). */
@Entity
@Table(name = "upload_errors")
@Getter
@Setter
@NoArgsConstructor
public class UploadError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(length = 100)
    private String sheet;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(length = 100)
    private String field;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IssueSeverity severity;
}
