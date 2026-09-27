package com.yourorg.taskmanager.project.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import com.yourorg.taskmanager.common.util.FileValidator;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesAndExtractsTextFromDocx() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "release-notes.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                createDocx());
        FileValidator validator = new FileValidator(DataSize.ofMegabytes(20),
            DataSize.ofMegabytes(10), DataSize.ofMegabytes(300));
        DocumentStorageService storage = new DocumentStorageService(tempDir.toString(), validator);

        DocumentStorageService.StoredUpload stored = storage.store(file);

        assertTrue(Files.exists(tempDir.resolve(stored.storedFilename())));
        assertTrue(stored.extractedText().contains("Release checklist"));
    }

    private byte[] createDocx() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            writeEntry(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                                            <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                    </Types>
                    """);
                        writeEntry(zip, "_rels/.rels", """
                                        <?xml version="1.0" encoding="UTF-8"?>
                                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                                            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                                        </Relationships>
                                        """);
                        writeEntry(zip, "word/_rels/document.xml.rels", """
                                        <?xml version="1.0" encoding="UTF-8"?>
                                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>
                                        """);
            writeEntry(zip, "word/document.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                                            <w:body><w:p><w:r><w:t>Release checklist</w:t></w:r></w:p><w:sectPr/></w:body>
                    </w:document>
                    """);
        }
        return bytes.toByteArray();
    }

    private void writeEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
