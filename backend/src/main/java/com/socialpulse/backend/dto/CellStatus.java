package com.socialpulse.backend.dto;

/** Bir ayın veri durumu: OK tam, PARTIAL bazı günler eksik, MISSING hiç veri yok. */
public enum CellStatus {
    OK,
    PARTIAL,
    MISSING
}
