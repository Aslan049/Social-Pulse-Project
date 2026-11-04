package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Platform;

import java.util.List;

/** Bir platformun ham tablolarını ortak metrik modeline çeviren bileşen. */
public interface PlatformAdapter {

    Platform platform();

    /** Hiçbir sayfa tanınmazsa InvalidUploadException fırlatır. */
    ParseResult parse(List<RawTable> tables);
}
