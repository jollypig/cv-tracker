package com.example.cv.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.cv.cv.CvImportRequest;
import com.example.cv.cv.CvResponse;
import com.example.cv.cv.CvStatus;
import com.example.cv.cv.CvImportService;
import jakarta.validation.Validation;
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
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private CvImportService cvImportService;
    @Mock
    private ObjectMapper objectMapper;

    private CvDocumentImportService service;
    private final AtomicReference<CvDocumentImport> savedImport = new AtomicReference<>();
    private final ParsedCv parsedCv = new ParsedCv(null, null, List.of(), List.of(), List.of(), List.of(), List.of());
    private final NormalizedCvDocument parsedDocument = new NormalizedCvDocument(List.of());

    @BeforeEach
    void setUp() throws Exception {
        service = new CvDocumentImportService(imports, pdfParser, htmlParser, extractor, cvImportService,
                new ParsedCvToContentMapper(Validation.buildDefaultValidatorFactory().getValidator()), objectMapper, 1024);
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

        assertThat(response.status()).isEqualTo(CvImportStatus.NEEDS_REVIEW);
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

        assertThat(retrieved.status()).isEqualTo(CvImportStatus.NEEDS_REVIEW);
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

    @Test
    void savesCorrectedDraftForReview() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[] {1});
        CvDocumentImportResponse created = service.startImport(file);
        ParsedCv corrected = new ParsedCv(null, null, List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of("reviewed"));
        when(imports.findById(created.importId())).thenReturn(Optional.of(savedImport.get()));
        when(objectMapper.writeValueAsString(corrected)).thenReturn("corrected");

        CvDocumentImportResponse updated = service.updateDraft(created.importId(), corrected);

        assertThat(updated.status()).isEqualTo(CvImportStatus.NEEDS_REVIEW);
        assertThat(updated.result()).isEqualTo(corrected);
        assertThat(savedImport.get().getResultJson()).isEqualTo("corrected");
        verify(imports, org.mockito.Mockito.times(3)).save(any(CvDocumentImport.class));
    }

    @Test
    void approvalCreatesCvAndCanOnlyBeAppliedOnce() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[] {1});
        CvDocumentImportResponse created = service.startImport(file);
        UUID personId = UUID.randomUUID();
        UUID cvId = UUID.randomUUID();
        when(imports.findById(created.importId())).thenReturn(Optional.of(savedImport.get()));
        when(cvImportService.importCv(eq(personId), any())).thenReturn(new CvResponse(cvId, personId,
                "Test Person", "Resume", null, "en", CvStatus.DRAFT, null, null, List.of(),
                Instant.now(), Instant.now()));
        CvDraftApprovalRequest request = new CvDraftApprovalRequest(personId, "Resume", "en", CvStatus.DRAFT,
                List.of());

        CvDocumentImportResponse approved = service.approveDraft(created.importId(), request);

        assertThat(approved.status()).isEqualTo(CvImportStatus.APPROVED);
        assertThat(approved.cvId()).isEqualTo(cvId);
        org.mockito.ArgumentCaptor<CvImportRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(CvImportRequest.class);
        verify(cvImportService).importCv(eq(personId), requestCaptor.capture());
        assertThat(requestCaptor.getValue().name()).isEqualTo("Resume");
        assertThat(requestCaptor.getValue().content().experiences()).isEmpty();
        assertThatThrownBy(() -> service.approveDraft(created.importId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not awaiting review");
        verify(cvImportService, org.mockito.Mockito.times(1)).importCv(eq(personId), any());
    }
}