package com.example.cv.importing;

import com.example.cv.cv.CvContent;
import com.example.cv.cv.CvImportRequest;
import com.example.cv.cv.CvImportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final CvDocumentImportRepository imports;
    private final PdfCvParser pdfParser;
    private final HtmlCvParser htmlParser;
    private final CvAiExtractor extractor;
    private final CvImportService cvImportService;
    private final ParsedCvToContentMapper contentMapper;
    private final ObjectMapper objectMapper;
    private final long maxFileSizeBytes;

    public CvDocumentImportService(CvDocumentImportRepository imports, PdfCvParser pdfParser,
            HtmlCvParser htmlParser, CvAiExtractor extractor, CvImportService cvImportService,
            ParsedCvToContentMapper contentMapper, ObjectMapper objectMapper,
            @Value("${cv.import.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        this.imports = imports;
        this.pdfParser = pdfParser;
        this.htmlParser = htmlParser;
        this.extractor = extractor;
        this.cvImportService = cvImportService;
        this.contentMapper = contentMapper;
        this.objectMapper = objectMapper;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public CvDocumentImportResponse startImport(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An uploaded CV file is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "File exceeds the maximum supported size of " + maxFileSizeBytes + " bytes");
        }

        String mediaType = resolveMediaType(file);
        CvDocumentImport documentImport = new CvDocumentImport(
                Optional.ofNullable(file.getOriginalFilename()).filter(name -> !name.isBlank()).orElse("upload"),
                mediaType,
                file.getSize());
        imports.save(documentImport);
        documentImport.markProcessing();

        try {
            NormalizedCvDocument document = parse(file.getBytes(), mediaType);
            document = new NormalizedCvDocument(CvDocumentNormalizer.normalize(document.blocks()));
            String sourceText = document.blocks().stream().map(NormalizedCvBlock::text)
                    .reduce((left, right) -> left + "\n" + right).orElse("");
            ParsedCv parsedCv = extractor.extract(document);
            if (parsedCv == null) {
                throw new IllegalStateException("The extractor returned no CV data");
            }
            parsedCv = ParsedCvNormalizer.normalize(parsedCv, LocalDate.now(), sourceText);
            documentImport.complete(serialize(parsedCv));
        } catch (IOException | RuntimeException exception) {
            documentImport.fail(failureMessage(exception));
        }

        imports.save(documentImport);
        return response(documentImport, parseResult(documentImport.getResultJson()));
    }

    @Transactional(readOnly = true)
    public CvDocumentImportResponse findById(UUID importId) {
        CvDocumentImport documentImport = getImport(importId);
        return response(documentImport, parseResult(documentImport.getResultJson()));
    }

    public CvDocumentImportResponse updateDraft(UUID importId, ParsedCv draft) {
        CvDocumentImport documentImport = getImport(importId);
        documentImport.updateDraft(serialize(draft));
        imports.save(documentImport);
        return response(documentImport, draft);
    }

    public CvDocumentImportResponse approveDraft(UUID importId, CvDraftApprovalRequest request) {
        CvDocumentImport documentImport = getImport(importId);
        documentImport.assertReviewable();
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

    private CvDocumentImport getImport(UUID importId) {
        return imports.findById(importId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV import was not found"));
    }

    private String resolveMediaType(MultipartFile file) {
        String mediaType = Optional.ofNullable(file.getContentType()).orElse("").split(";", 2)[0].strip()
                .toLowerCase(Locale.ROOT);
        String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (mediaType.isBlank() || "application/octet-stream".equals(mediaType)) {
            if (fileName.endsWith(".pdf")) {
                return "application/pdf";
            }
            if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                return "text/html";
            }
        }
        if ("application/pdf".equals(mediaType)) {
            return mediaType;
        }
        if ("text/html".equals(mediaType) || "application/xhtml+xml".equals(mediaType)) {
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
        if (exception instanceof PdfCvParserException || exception instanceof HtmlCvParserException) {
            return exception.getMessage();
        }
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