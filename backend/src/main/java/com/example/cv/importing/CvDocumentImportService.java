package com.example.cv.importing;

import com.example.cv.cv.CvContent;
import com.example.cv.cv.CvImportRequest;
import com.example.cv.cv.CvImportService;
import com.example.cv.person.PersonRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CvDocumentImportService {

    private static final Logger logger = LoggerFactory.getLogger(CvDocumentImportService.class);

    private final CvDocumentImportRepository imports;
    private final PdfCvParser pdfParser;
    private final HtmlCvParser htmlParser;
    private final CvAiExtractor extractor;
    private final CvImportService cvImportService;
    private final ParsedCvToContentMapper contentMapper;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final PersonRepository people;
    private final long maxFileSizeBytes;

    public CvDocumentImportService(CvDocumentImportRepository imports, PdfCvParser pdfParser,
            HtmlCvParser htmlParser, CvAiExtractor extractor, CvImportService cvImportService,
            ParsedCvToContentMapper contentMapper, ObjectMapper objectMapper,
            MeterRegistry meterRegistry, PersonRepository people,
            @Value("${cv.import.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        this.imports = imports;
        this.pdfParser = pdfParser;
        this.htmlParser = htmlParser;
        this.extractor = extractor;
        this.cvImportService = cvImportService;
        this.contentMapper = contentMapper;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.people = people;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public CvDocumentImportResponse startImport(MultipartFile file, UUID ownerId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An uploaded CV file is required");
        }
        if (ownerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign-in is required to import a CV");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "File exceeds the maximum supported size of " + maxFileSizeBytes + " bytes");
        }

        String mediaType = resolveMediaType(file);
        CvDocumentImport documentImport = new CvDocumentImport(
            safeFileName(file.getOriginalFilename()), mediaType, file.getSize(), ownerId);
        imports.save(documentImport);
        documentImport.markProcessing();

        Timer.Sample processingTimer = Timer.start(meterRegistry);
        String stage = "parser";
        try {
            NormalizedCvDocument document = parse(file.getBytes(), mediaType);
            document = new NormalizedCvDocument(CvDocumentNormalizer.normalize(document.blocks()));
            String sourceText = document.blocks().stream().map(NormalizedCvBlock::text)
                    .reduce((left, right) -> left + "\n" + right).orElse("");
            documentImport.recordAiModel(extractor.modelMetadata());
            stage = "ai";
            ParsedCv parsedCv = extractor.extract(document);
            if (parsedCv == null) {
                throw new IllegalStateException("The extractor returned no CV data");
            }
            stage = "normalization";
            parsedCv = ParsedCvNormalizer.normalize(parsedCv, LocalDate.now(), sourceText);
            documentImport.complete(serialize(parsedCv));
        } catch (IOException | RuntimeException exception) {
            documentImport.fail(failureMessage(exception));
            if ("ai".equals(stage)) {
                Counter.builder("cv.ai.failures").register(meterRegistry).increment();
            }
            Counter.builder("cv.import.failures")
                .tag("stage", stage)
                .register(meterRegistry)
                .increment();
            logger.warn("CV import {} failed during {} ({})", documentImport.getId(), stage,
                exception.getClass().getSimpleName());
        }

        imports.save(documentImport);
        String status = documentImport.getStatus().name();
        processingTimer.stop(Timer.builder("cv.import.processing")
            .tag("status", status)
            .register(meterRegistry));
        Counter.builder("cv.import.status")
            .tag("status", status)
            .register(meterRegistry)
            .increment();
        logger.info("CV import {} finished with status {} (AI model={}, version={})", documentImport.getId(),
            status, documentImport.getAiModelName(), documentImport.getAiModelVersion());
        return response(documentImport, parseResult(documentImport.getResultJson()));
    }

    @Transactional(readOnly = true)
    public CvDocumentImportResponse findById(UUID importId, UUID ownerId) {
        CvDocumentImport documentImport = getImport(importId, ownerId);
        return response(documentImport, parseResult(documentImport.getResultJson()));
    }

    public CvDocumentImportResponse updateDraft(UUID importId, UUID ownerId, ParsedCv draft) {
        CvDocumentImport documentImport = getImport(importId, ownerId);
        documentImport.updateDraft(serialize(draft));
        imports.save(documentImport);
        return response(documentImport, draft);
    }

    public CvDocumentImportResponse approveDraft(UUID importId, UUID ownerId, CvDraftApprovalRequest request) {
        CvDocumentImport documentImport = getImport(importId, ownerId);
        documentImport.assertReviewable();
        if (!people.existsByIdAndOwner_Id(request.personId(), ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Person was not found");
        }
        ParsedCv draft = parseResult(documentImport.getResultJson());
        if (draft == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CV import has no reviewable draft");
        }
        CvContent content = contentMapper.toContent(draft);
        var cv = cvImportService.importCv(request.personId(), new CvImportRequest(
                request.name(), null, request.language(), request.status(), request.tags(), content));
        documentImport.approve(cv.id());
        imports.save(documentImport);
        return response(documentImport, draft);
    }

    private CvDocumentImport getImport(UUID importId, UUID ownerId) {
        return imports.findByIdAndOwnerId(importId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV import was not found"));
    }

    private String safeFileName(String originalFilename) {
        String fileName = Optional.ofNullable(originalFilename).orElse("").replace('\\', '/');
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_").strip();
        if (fileName.isBlank() || ".".equals(fileName) || "..".equals(fileName)) {
            return "upload";
        }
        return fileName.substring(0, Math.min(fileName.length(), 500));
    }

    private String resolveMediaType(MultipartFile file) {
        String mediaType = Optional.ofNullable(file.getContentType()).orElse("").split(";", 2)[0].strip()
                .toLowerCase(Locale.ROOT);
        String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        fileName = fileName.replace('\\', '/');
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "";
        if (mediaType.isBlank() || "application/octet-stream".equals(mediaType)) {
            if ("pdf".equals(extension)) {
                return "application/pdf";
            }
            if ("html".equals(extension) || "htm".equals(extension)) {
                return "text/html";
            }
        }
        if ("application/pdf".equals(mediaType)
                && (extension.isBlank() || "pdf".equals(extension))) {
            return mediaType;
        }
        if (("text/html".equals(mediaType) || "application/xhtml+xml".equals(mediaType))
                && (extension.isBlank() || "html".equals(extension) || "htm".equals(extension))) {
            return mediaType;
        }
        throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Only PDF and HTML CV files are supported");
    }

    private NormalizedCvDocument parse(byte[] content, String mediaType) {
        if ("application/pdf".equals(mediaType)) {
            return pdfParser.parse(content, mediaType);
        }
        return htmlParser.parse(content, mediaType);
    }

    private String serialize(ParsedCv parsedCv) {
        try {
            return objectMapper.writeValueAsString(parsedCv);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("CV extraction result could not be serialized", exception);
        }
    }

    private ParsedCv parseResult(String resultJson) {
        if (resultJson == null) {
            return null;
        }
        try {
            return objectMapper.readValue(resultJson, ParsedCv.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored CV extraction result is invalid", exception);
        }
    }

    private String failureMessage(Exception exception) {
        if (exception instanceof CvAiExtractionException) {
            return "AI extraction failed; the file can be retried";
        }
        return "CV processing failed; the file can be retried";
    }

    private CvDocumentImportResponse response(CvDocumentImport documentImport, ParsedCv result) {
        return new CvDocumentImportResponse(documentImport.getId(), documentImport.getFileName(),
                documentImport.getMediaType(), documentImport.getFileSize(), documentImport.getStatus(),
            documentImport.getErrorMessage(), result, documentImport.getCvId(), documentImport.getCreatedAt(),
            documentImport.getUpdatedAt());
    }
}