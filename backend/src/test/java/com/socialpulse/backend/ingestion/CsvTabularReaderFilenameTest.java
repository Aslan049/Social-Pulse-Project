package com.socialpulse.backend.ingestion;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvTabularReaderFilenameTest {

    private final CsvTabularReader reader = new CsvTabularReader();

    private List<RawTable> read(String filename) {
        return reader.read(new ByteArrayInputStream("a,b\n1,2\n".getBytes(StandardCharsets.UTF_8)), filename);
    }

    @Test
    void tableNameComesFromFilenameWithoutExtension() {
        assertEquals("ziyaretler", read("ziyaretler.csv").get(0).name());
        assertEquals("Instagram Raporu.final", read("Instagram Raporu.final.csv").get(0).name());
    }

    @Test
    void readWithoutFilename_keepsDefaultName() {
        List<RawTable> tables = reader.read(new ByteArrayInputStream("a,b\n1,2\n".getBytes(StandardCharsets.UTF_8)));

        assertEquals("CSV", tables.get(0).name());
    }

    @Test
    void rowsAreUnchangedByRenaming() {
        RawTable table = read("x.csv").get(0);

        assertEquals(2, table.rowCount());
        assertEquals(List.of("a", "b"), table.rows().get(0));
    }
}
