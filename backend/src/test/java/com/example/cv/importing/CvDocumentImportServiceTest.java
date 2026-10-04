package com.example.cv.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvDocumentImportServiceTest {

    @Mock
    private CvDocumentImportRepository imports;
    @Mock
    private PdfCvParser pdfParser;
    @Mock
    private HtmlCvParser htmlParser;
    @Mock
    private CvAiExtractor extractor;
    @Mock
    private ObjectMapper objectMapper;

    private CvDocumentImportService service;
    private final AtomicReference<CvDocumentImport> savedImport = new AtomicReference<>();
    private final ParsedCv parsedCv = new ParsedCv(null, null, List.of(), List.of(), List.of(), List.of(), List.of());
    private final NormalizedCvDocument parsedDocument = new NormalizedCvDocument(List.of());

    @BeforeEach
    void setUp() throws Exception {
        service = new CvDocumentImportService(imports, pdfParser, htmlParser, extractor, objectMapper, 1024);
        Mockito.lenient().when(imports.save(any(CvDocumentImport.class))).thenAnswer(invocation -> {
            CvDocumentImport saved = invocation.getArgument(0);
            savedImport.set(saved);
            return saved;
        });
        Mockito.lenient().when(extractor.extract(any(NormalizedCvDocument.class))).thenReturn(parsedCv);
        Mockito.lenient().when(objectMapper.writeValueAsString(parsedCv)).thenReturn("{}");
        Mockito.lenient().when(objectMapper.readValue("{}", ParsedCv.class)).thenReturn(parsedCv);
        Mockito.lenient().when(pdfParser.parse(any(byte[].class), any(String.class))).thenReturn(parsedDocument);
        Mockito.lenient().when(htmlParser.parse(any(byte[].class), any(String.class))).thenReturn(parsedDocument);
    }

    @Test
    void importsPdfAndReturnsPersistedResultAndStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[] {1, 2});

        CvDocumentImportResponse response = service.startImport(file);

        assertThat(response.status()).isEqualTo(CvImportStatus.COMPLETED);
        assertThat(response.result()).isEqualTo(parsedCv);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        verify(pdfParser).parse(new byte[] {1, 2}, "application/pdf");
        verify(imports, org.mockito.Mockito.times(2)).save(any(CvDocumentImport.class));
    }

    @Test
    void importsHtmlAndCanRetrieveItsStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.html", "text/html", "<h1>CV</h1>".getBytes());
        CvDocumentImportResponse created = service.startImport(file);
        when(imports.findById(created.importId())).thenReturn(Optional.of(savedImport.get()));

        CvDocumentImportResponse retrieved = service.findById(created.importId());

        assertThat(retrieved.status()).isEqualTo(CvImportStatus.COMPLETED);
        assertThat(retrieved.result()).isEqualTo(parsedCv);
        verify(htmlParser).parse(file.getBytes(), "text/html");
    }

    @Test
    void recordsProcessingFailuresAsFailedImports() throws Exception {
        when(extractor.extract(any(NormalizedCvDocument.class)))
                .thenThrow(new CvAiExtractionException("provider failed", new IllegalStateException()));
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[] {1});

        CvDocumentImportResponse response = service.startImport(file);

        assertThat(response.status()).isEqualTo(CvImportStatus.FAILED);
        assertThat(response.errorMessage()).contains("AI extraction failed");
        assertThat(response.result()).isNull();
    }

    @Test
    void rejectsUnsupportedAndOversizedFilesBeforeCreatingImports() {
        MockMultipartFile unsupported = new MockMultipartFile("file", "resume.docx", "application/zip", new byte[] {1});
        MockMultipartFile oversized = new MockMultipartFile("file", "resume.pdf", "application/pdf",
            new byte[1025]);

        assertThatThrownBy(() -> service.startImport(unsupported))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only PDF and HTML");
        assertThatThrownBy(() -> service.startImport(oversized))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("maximum supported size");
    }

    @Test
    void returnsNotFoundForUnknownImportId() {
        UUID missingId = UUID.randomUUID();
        when(imports.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(missingId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CV import was not found");
    }
}