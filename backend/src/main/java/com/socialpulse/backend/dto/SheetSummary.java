package com.socialpulse.backend.dto;

import java.util.List;

/** Yüklenen dosyadaki bir sayfanın (CSV için tek tablo) özeti. */
public record SheetSummary(String name, int rowCount, int columnCount, List<String> header) {}
