# CV Parser / CV Import — Architecture & Sprint 13

## 1. Goal

Implement CV import/parsing for the CV application from:

- PDF files
- HTML CVs

The parser must transform an unstructured or semi-structured CV into the application's defined CV structure.

Because CVs can use different layouts, terminology, section names, incomplete dates, implicit skills, and inconsistent project descriptions, the solution should use a **hybrid deterministic + AI approach**.

The preferred AI runtime is **Ollama with a local LLM**, keeping the AI provider replaceable so that OpenAI/Azure OpenAI or another provider can be added later.

---

# 2. Target CV Structure

The parser should ultimately populate the application's CV model, including the following major areas.

## Personal information

Examples:

- First name
- Last name
- Email
- Phone
- Location
- LinkedIn / other URLs

## Education

Examples:

- Institution
- Degree
- Field of study
- Period
- Description

## Employment / Experience

An employment record may contain:

- Company
- Employment period
- Position
- Location
- Employment type
- Industry
- Projects

## Projects

A project should support approximately:

```text
Company:
Industries:
Project Name:
Project Description:
Period:
Position:
Responsibilities:
Technologies and Tools:
```

A project may contain multiple industries, responsibilities and technologies.

## Skills

Skills are organized into groups:

```text
Skill Group
    └── Skills
```

Examples:

```text
Backend
    Java
    Spring Boot
    REST
    Kafka

Frontend
    JavaScript
    TypeScript
    Vue.js

Database
    PostgreSQL
    Redis
```

Important: the parser should prefer matching extracted skills to the application's existing skill catalog instead of creating arbitrary new skills.

## Languages

Language records should support CEFR-style levels where available, for example:

```text
English (C1)
Latvian (B2)
Russian (C2)
```

The parser should not invent a CEFR level when the CV does not provide enough evidence.

## Other possible sections

The parser architecture should allow future support for:

- Certifications
- Military status
- Employment type
- Employment location
- Professional summary
- Awards
- Publications
- Volunteer experience

---

# 3. Core Architecture

The recommended pipeline is:

```text
                PDF / HTML
                    |
                    v
          +--------------------+
          | Document extraction|
          | PDFBox / Jsoup     |
          +---------+----------+
                    |
                    v
          Normalized CV Document
                    |
          +---------+----------+
          |                    |
          v                    v
   Deterministic parser     AI parser
   - email                  - sections
   - phone                  - experience
   - URLs                   - projects
   - dates                  - skills
   - obvious headings       - education
          |                 - languages
          +--------+---------+
                   |
                   v
            ParsedCv DTO
                   |
                   v
          Validation / schema
                   |
                   v
         Normalization layer
          - skills
          - positions
          - companies
                   |
                   v
        Deterministic calculations
          - experience duration
          - last used date
          - overlapping periods
                   |
                   v
              CV Draft
                   |
                   v
             User Review
                   |
                   v
             Final CV Model
```

The important design principle is:

> **AI extracts and interprets. Application code validates, normalizes, calculates and persists.**

Do not allow the LLM to directly write JPA/domain entities.

---

# 4. Why a Hybrid Parser

A pure rule-based parser will fail on vague CVs.

A pure LLM parser will be unreliable for calculations and domain consistency.

Use deterministic parsing for information that is easy to recognize:

- Email
- Phone
- URLs
- Explicit dates
- Common section headings
- Basic HTML structure
- PDF page boundaries
- Obvious lists/tables

Use AI for ambiguous information:

- Section identification
- Company vs project detection
- Position extraction
- Project extraction
- Responsibilities
- Skill extraction
- Skill synonym normalization
- Education interpretation
- Language interpretation
- Mapping differently named sections to application concepts

---

# 5. PDF Parsing

## Text-based PDF

Use Apache PDFBox.

Conceptually:

```java
try (PDDocument document = Loader.loadPDF(file)) {
    PDFTextStripper stripper = new PDFTextStripper();
    String text = stripper.getText(document);
}
```

Prefer preserving page/section information where possible rather than immediately flattening everything into one string.

## Scanned PDF

Some PDFs contain only images.

Future/optional OCR pipeline:

```text
PDF
 |
 v
No useful text
 |
 v
OCR
 |
 v
Normalized text
```

Possible OCR implementations:

- Tesseract
- Cloud OCR/document services

Initial implementation can focus on text-based PDFs and add OCR separately.

---

# 6. HTML Parsing

Use Jsoup.

Do not immediately convert HTML into plain text.

Preserve semantic structure such as:

- headings
- paragraphs
- lists
- tables
- links

Example:

```java
Document document = Jsoup.parse(html);

Elements headings = document.select("h1, h2, h3");
Elements paragraphs = document.select("p");
Elements lists = document.select("ul, ol");
```

The result should be transformed into a normalized document representation.

---

# 7. Normalized CV Document

Introduce an intermediate representation between document extraction and AI.

Example:

```json
{
  "blocks": [
    {
      "type": "heading",
      "text": "Professional Experience"
    },
    {
      "type": "paragraph",
      "text": "Senior Java Developer"
    },
    {
      "type": "paragraph",
      "text": "ABC Bank"
    }
  ]
}
```

For PDF, a text representation can retain page boundaries:

```text
=== PAGE 1 ===

JOHN SMITH
Senior Java Developer

Email: john@example.com

=== PAGE 2 ===

PROFESSIONAL EXPERIENCE
...
```

This normalized representation becomes the common input for HTML and PDF.

---

# 8. Intermediate Parsed CV Model

Do not map AI output directly into the final persistence model.

Create an intermediate DTO such as:

```java
public record ParsedCv(
        PersonalData personalData,
        List<ParsedEmployment> employment,
        List<ParsedEducation> education,
        List<ParsedLanguage> languages,
        List<ParsedSkill> skills,
        List<ParsedProject> projects
) {}
```

For extracted fields that need evidence, use something similar to:

```java
public record ExtractedValue<T>(
        T value,
        Double confidence,
        String sourceText
) {}
```

Example:

```json
{
  "value": "Java",
  "confidence": 0.98,
  "sourceText": "Technologies and Tools: Java, Spring Boot, Kafka"
}
```

The evidence is important for the review UI and debugging.

---

# 9. AI Processing Strategy

Do not use one giant prompt such as:

> Parse this CV into the entire application model.

Instead, process the document in logical stages.

## Stage 1 — Section detection

Input:

```text
Normalized CV
```

Output:

```json
{
  "sections": [
    {
      "type": "PROFILE",
      "start": 0,
      "end": 120
    },
    {
      "type": "EXPERIENCE",
      "start": 121,
      "end": 1450
    },
    {
      "type": "EDUCATION",
      "start": 1451,
      "end": 1650
    },
    {
      "type": "SKILLS",
      "start": 1651,
      "end": 1900
    }
  ]
}
```

The parser should handle alternative headings such as:

```text
Experience
Professional Experience
Work Experience
Employment History
Career History
```

and map them to the same logical section.

## Stage 2 — Personal data

Extract:

- name
- email
- phone
- location
- URLs

Use deterministic extraction where possible and AI for ambiguous formats.

## Stage 3 — Employment extraction

Example:

```json
{
  "company": "ABC Bank",
  "position": "Senior Java Developer",
  "period": {
    "from": "2022-01",
    "to": null
  },
  "location": "Riga",
  "employmentType": null
}
```

## Stage 4 — Project extraction

Map project descriptions to the application's project structure:

```json
{
  "company": "ABC Bank",
  "industries": [
    "Banking",
    "Cyber Security"
  ],
  "projectName": "Jira Plugin",
  "projectDescription": "Jira plugin to integrate with customer service",
  "period": {
    "from": "2022-01",
    "to": "2023-05"
  },
  "position": "Full Stack Developer",
  "responsibilities": [
    "Developed project from scratch",
    "Testing and quality assurance"
  ],
  "technologies": [
    "Java",
    "Spring Boot"
  ]
}
```

## Stage 5 — Skill extraction

Extract raw skills first.

Example:

```json
{
  "rawName": "Postgres",
  "evidence": "Technologies: Java, Spring Boot, Postgres",
  "confidence": 0.95
}
```

Then normalize against the application's skill catalog.

---

# 10. Skill Catalog Matching

The application should own the canonical skill list.

Example:

```json
[
  {
    "id": "skill-java",
    "name": "Java",
    "group": "Backend"
  },
  {
    "id": "skill-spring-boot",
    "name": "Spring Boot",
    "group": "Backend"
  },
  {
    "id": "skill-postgresql",
    "name": "PostgreSQL",
    "group": "Database"
  }
]
```

The AI should be instructed:

> Match extracted technologies to the provided skill catalog. Do not create new skills.

Examples:

```text
Postgres     -> PostgreSQL
Postgres SQL -> PostgreSQL
Spring Boot  -> Spring Boot
```

If no match exists:

```json
{
  "skillId": null,
  "rawValue": "UnknownFramework",
  "confidence": 0.91
}
```

The user/admin can decide whether to add a new skill.

---

# 11. Skill Experience Calculation

Do not ask AI to calculate years of experience.

The AI should extract evidence:

```text
Project A
2019–2021
Java

Project B
2021–2023
Java

Project C
2023–Present
Java
```

Then backend code should:

1. Collect all skill evidence periods.
2. Normalize dates.
3. Merge overlapping periods.
4. Calculate duration.
5. Determine the last used date.

Example:

```text
2019 ----------------------------- Present
```

Then:

```text
yearsOfExperience = calculated value
lastUsedDate       = calculated value
```

This prevents hallucinated experience.

---

# 12. Skill Proficiency

Only use a proficiency value when there is evidence.

Explicit:

```text
Java — Expert
```

can become:

```json
{
  "proficiency": "EXPERT",
  "confidence": 0.99
}
```

But:

```text
Developed distributed systems using Java and Spring Boot.
```

should not automatically become `EXPERT`.

Instead:

```json
{
  "proficiency": null,
  "proficiencyEvidence": "Developed distributed systems using Java",
  "confidence": 0.72
}
```

The user can provide the final value.

---

# 13. Confidence and Evidence

Every ambiguous extracted field should ideally contain:

- value
- confidence
- source/evidence

Possible application-level confidence:

```text
HIGH       0.90–1.00
MEDIUM     0.70–0.89
LOW        <0.70
```

The numeric confidence returned by an LLM should not be treated as authoritative by itself.

Confidence should eventually be combined with deterministic validation/evidence quality.

---

# 14. User Review

The parser should create a **CV Draft**, not silently publish the imported data.

Example UI:

```text
Review imported CV

Name              John Smith              ✓
Email             john@example.com        ✓
Java              7 years                 ✓
Spring Boot       6 years                 ✓
Kafka             3 years                 ?
Java proficiency  Not specified            !

[Accept] [Edit]
```

Low-confidence or inferred values should be highlighted.

This is particularly important for:

- skill proficiency
- ambiguous employment dates
- project/company relationships
- industries
- inferred education fields
- normalized skills

---

# 15. Ollama / Local AI

Ollama is the preferred initial AI runtime.

Architecture:

```text
CvAiExtractor
      |
      v
Spring AI / LLM abstraction
      |
      v
Ollama
      |
      v
Local instruction-following model
```

Example configuration concept:

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: <selected-model>
```

The exact model should be selected through a benchmark against representative CVs rather than assumed in advance.

Candidate models should be evaluated for:

- instruction following
- structured JSON output
- multilingual support
- context window
- inference speed
- available RAM/VRAM

---

# 16. Keep AI Provider Replaceable

Define an application-level interface:

```java
public interface CvAiExtractor {

    ParsedCv extract(NormalizedCvDocument document);
}
```

Possible implementations:

```text
CvAiExtractor
    |
    +-- OllamaCvAiExtractor
    +-- OpenAiCvAiExtractor
    +-- AzureOpenAiCvAiExtractor
```

Configuration:

```yaml
cv:
  ai:
    provider: ollama
```

The CV domain should not depend directly on Ollama or a specific LLM vendor.

---

# 17. Spring Boot Package Structure

Suggested structure:

```text
cv-service
|
+-- controller
|   +-- CvImportController
|
+-- application
|   +-- CvImportService
|   +-- CvParsingService
|   +-- CvReviewService
|
+-- domain
|   +-- model
|   |   +-- Cv
|   |   +-- Experience
|   |   +-- Project
|   |   +-- Education
|   |   +-- Language
|   |   +-- Skill
|   |
|   +-- service
|       +-- SkillExperienceCalculator
|
+-- parsing
|   +-- CvParser
|   +-- PdfCvParser
|   +-- HtmlCvParser
|   +-- TextNormalizer
|
+-- ai
|   +-- CvAiExtractor
|   +-- SectionExtractor
|   +-- ExperienceExtractor
|   +-- SkillExtractor
|   +-- AiResponseValidator
|
+-- normalization
|   +-- SkillNormalizer
|   +-- PositionNormalizer
|   +-- CompanyNormalizer
|
+-- infrastructure
    +-- pdf
    +-- ai
    +-- persistence
```

---

# 18. API

Initial endpoint:

```http
POST /api/cvs/import
Content-Type: multipart/form-data
```

Example:

```text
file = resume.pdf
```

For small files/simple processing, a synchronous response may be acceptable.

Recommended production flow:

```http
POST /api/cvs/import
```

Response:

```json
{
  "importId": "8f7...",
  "status": "PROCESSING"
}
```

Then:

```http
GET /api/cvs/import/{id}
```

Possible statuses:

```text
UPLOADED
EXTRACTING
PARSING
NORMALIZING
VALIDATING
REVIEW_REQUIRED
COMPLETED
FAILED
```

---

# 19. Validation

AI output must never be trusted directly.

Pipeline:

```text
LLM JSON
   |
   v
JSON schema / Jackson
   |
   v
Bean Validation
   |
   v
Domain validation
   |
   v
Normalization
   |
   v
Final CV Draft
```

Examples of validation:

- Invalid email -> reject/fix using deterministic parser
- Invalid date range -> warning
- End date before start date -> invalid
- Unknown skill -> keep as raw skill candidate
- Empty project name -> warning
- Duplicate skills -> merge
- Duplicate employment records -> flag
- Unsupported CEFR level -> warning

---

# 20. Security / Privacy

Because CVs contain personal information:

- Prefer local Ollama processing.
- Do not send CV content to external LLM providers unless explicitly configured.
- Do not expose Ollama directly to the public internet.
- Restrict Ollama access to the internal network.
- Avoid logging full CV contents.
- Avoid logging LLM prompts/responses in production.
- Encrypt uploaded CVs if stored.
- Define retention/deletion rules for source documents.
- Remove temporary files after processing.
- Audit import/review operations without logging sensitive CV contents.

---

# 21. Failure Handling

The parser must support partial results.

Example:

```json
{
  "status": "REVIEW_REQUIRED",
  "warnings": [
    "Could not determine employment end date",
    "Skill 'XYZ Framework' could not be matched",
    "Education degree is ambiguous"
  ]
}
```

Do not fail the complete import because one field is ambiguous.

Instead:

```text
Reliable fields -> imported
Ambiguous fields -> review
Unsupported fields -> warning
Invalid data -> validation error
```

---

# 22. Testing Strategy

Create a CV parsing test corpus.

Recommended initial dataset:

- 20–50 anonymized CVs
- PDF and HTML
- different layouts
- different languages
- different levels of detail
- one-column and two-column CVs
- CVs with tables
- CVs with explicit skills
- CVs where skills only appear inside projects
- CVs with incomplete dates
- CVs with multiple projects under one employer
- CVs with no project section
- CVs with unconventional section names

Measure field-level accuracy:

```text
Personal data
Employment
Dates
Projects
Responsibilities
Technologies
Skills
Education
Languages
```

Also test:

- malformed PDF
- empty HTML
- scanned PDF
- very large CV
- unsupported file type
- AI unavailable
- Ollama unavailable
- invalid LLM JSON
- LLM timeout
- model returns unknown skills

---

# 23. Recommended Processing Rules

## Rule 1

Never invent information.

## Rule 2

If the CV explicitly states something, preserve it.

## Rule 3

If something can be calculated deterministically, calculate it in backend code.

## Rule 4

If something is ambiguous, preserve evidence and require review.

## Rule 5

Never allow AI to create canonical skills without matching/approval.

## Rule 6

Keep source evidence for important imported values.

## Rule 7

AI provider must be replaceable.

---

# 24. Sprint 13

## Sprint Goal

Implement the first production-ready foundation for importing CVs from PDF/HTML and extracting a structured CV draft using a local Ollama model.

---

## Issue 13.1 — Define CV Import Domain and Intermediate DTOs

**Type:** Story

### Scope

Define the intermediate representation used between document parsing and the final CV domain.

### Tasks

- Define `ParsedCv`
- Define `ParsedEmployment`
- Define `ParsedProject`
- Define `ParsedEducation`
- Define `ParsedLanguage`
- Define `ParsedSkill`
- Define `NormalizedCvDocument`
- Define extraction evidence/confidence model
- Define parser/import status model

### Acceptance Criteria

- DTOs represent all required CV sections.
- DTOs are independent of JPA entities.
- Ambiguous values can carry evidence/confidence.
- JSON serialization/deserialization works.

---

## Issue 13.2 — Implement PDF Text Extraction

**Type:** Story

### Scope

Implement text extraction for text-based PDFs.

### Tasks

- Add PDFBox
- Implement `PdfCvParser`
- Extract text by page
- Preserve page boundaries
- Detect empty/non-text PDFs
- Add size/type validation

### Acceptance Criteria

- Text-based PDF can be converted into `NormalizedCvDocument`.
- Page boundaries are retained.
- Invalid PDFs are handled gracefully.
- Scanned PDFs are detected as unsupported/OCR-required.

---

## Issue 13.3 — Implement HTML CV Extraction

**Type:** Story

### Scope

Implement HTML parsing using Jsoup.

### Tasks

- Add Jsoup
- Extract headings
- Extract paragraphs
- Extract lists
- Extract tables
- Extract links
- Build normalized document blocks

### Acceptance Criteria

- HTML CV is converted to the same normalized representation used by PDF.
- Heading/list/table structure is preserved.
- Links can be identified.

---

## Issue 13.4 — Implement Document Normalization

**Type:** Story

### Scope

Create a common representation independent of PDF/HTML.

### Tasks

- Define document block types.
- Normalize whitespace.
- Normalize line endings.
- Preserve section ordering.
- Preserve page/source information where available.
- Remove obvious parser artifacts.

### Acceptance Criteria

- PDF and HTML parsers produce compatible normalized documents.
- AI layer does not need to know the source format.

---

## Issue 13.5 — Implement Ollama Integration

**Type:** Story

### Scope

Connect Spring Boot to a local Ollama instance.

### Tasks

- Add Spring AI Ollama integration.
- Configure Ollama base URL.
- Configure model through application configuration.
- Implement health check.
- Implement timeout handling.
- Implement `CvAiExtractor`.
- Keep AI provider behind an interface.

### Acceptance Criteria

- Application can communicate with local Ollama.
- Model is configurable.
- Ollama failures are handled.
- No domain code directly depends on Ollama APIs.

---

## Issue 13.6 — Implement AI Section Detection

**Type:** Story

### Scope

Use the local model to identify logical CV sections.

### Tasks

- Create section extraction prompt.
- Define structured output schema.
- Support common section-name variations.
- Validate section positions/types.
- Add fallback when AI cannot identify a section.

### Acceptance Criteria

- CV sections are detected reliably on representative test CVs.
- Output is valid structured JSON.
- Unknown sections do not break processing.

---

## Issue 13.7 — Implement Personal Data Extraction

**Type:** Story

### Scope

Extract personal information.

### Tasks

- Deterministic email extraction.
- Deterministic URL extraction.
- Phone extraction.
- AI-assisted name/location extraction.
- Add evidence/confidence.
- Validate extracted values.

### Acceptance Criteria

- Email and URLs are extracted deterministically where possible.
- Ambiguous personal information is marked for review.
- No invented values are generated.

---

## Issue 13.8 — Implement Employment and Experience Extraction

**Type:** Story

### Scope

Extract employment records from arbitrary CV layouts.

### Tasks

- Extract company.
- Extract position.
- Extract employment dates.
- Extract location.
- Extract employment type when explicitly stated.
- Associate projects with employment where possible.
- Preserve ambiguous relationships for review.

### Acceptance Criteria

- Employment records are returned as structured DTOs.
- Incomplete dates are allowed.
- Invalid date ranges are detected.
- Company/project ambiguity is surfaced as a warning.

---

## Issue 13.9 — Implement Project Extraction

**Type:** Story

### Scope

Map project descriptions into the application's project structure.

### Tasks

- Extract project name.
- Extract description.
- Extract industry.
- Extract period.
- Extract position.
- Extract responsibilities.
- Extract technologies/tools.
- Associate project with company/employment.

### Acceptance Criteria

- Projects can be extracted even when no explicit "Projects" section exists.
- Responsibilities and technologies are represented as lists.
- Missing values remain null/empty instead of being invented.

---

## Issue 13.10 — Implement Skill Extraction

**Type:** Story

### Scope

Extract skills/technologies from all relevant CV sections.

### Tasks

- Extract explicit skills.
- Extract technologies from projects.
- Extract technologies from employment descriptions.
- Deduplicate raw skills.
- Preserve evidence.
- Do not calculate experience yet.

### Acceptance Criteria

- Skills can originate from multiple CV sections.
- Duplicate mentions can be merged.
- Evidence is retained.
- Unknown skills are not silently discarded.

---

## Issue 13.11 — Implement Skill Catalog Normalization

**Type:** Story

### Scope

Map raw CV skills to the application's canonical skill catalog.

### Tasks

- Load available skill catalog.
- Implement exact matching.
- Implement alias/synonym matching.
- Use Ollama for ambiguous mappings.
- Return unmatched skills as candidates.
- Preserve original raw name.

### Acceptance Criteria

Examples:

```text
Postgres -> PostgreSQL
PostgreSQL -> PostgreSQL
Spring -> Spring Framework
```

- Canonical skill IDs are used when a match exists.
- AI cannot create canonical skills automatically.
- Unmatched skills require review/approval.

---

## Issue 13.12 — Implement Skill Experience Calculation

**Type:** Story

### Scope

Calculate `yearsOfExperience` and `lastUsedDate` from project/employment evidence.

### Tasks

- Collect skill usage periods.
- Normalize dates.
- Merge overlapping periods.
- Calculate total duration.
- Calculate last used date.
- Handle current/present employment.
- Handle incomplete dates.

### Acceptance Criteria

- AI does not calculate years of experience.
- Overlapping periods are not double-counted.
- Results are deterministic and unit tested.

---

## Issue 13.13 — Implement Education and Language Extraction

**Type:** Story

### Scope

Extract education and language information.

### Tasks

- Institution extraction.
- Degree extraction.
- Field of study extraction.
- Education period extraction.
- Language extraction.
- CEFR extraction when explicitly available.
- Do not infer CEFR without evidence.

### Acceptance Criteria

- Education is mapped to structured DTOs.
- Languages are normalized.
- Explicit CEFR values are preserved.
- Missing CEFR values remain unknown.

---

## Issue 13.14 — Implement AI Output Validation

**Type:** Story

### Scope

Protect the application from invalid or malformed LLM output.

### Tasks

- Define JSON schema/structured response format.
- Jackson deserialization.
- Bean validation.
- Domain validation.
- Validate dates.
- Validate enums.
- Validate required fields.
- Handle malformed model responses.

### Acceptance Criteria

- Invalid AI output cannot be persisted directly.
- Validation errors produce actionable warnings/errors.
- Partial valid data can still be reviewed.

---

## Issue 13.15 — Implement CV Import API

**Type:** Story

### Scope

Expose the import process through REST API.

### Endpoints

```http
POST /api/cvs/import
GET  /api/cvs/import/{importId}
```

### Tasks

- Multipart upload.
- File type validation.
- Size validation.
- Import status.
- Async processing if required.
- Error handling.
- Persist import metadata.

### Acceptance Criteria

- PDF and HTML imports are supported.
- Import status can be queried.
- Processing errors are represented explicitly.
- Source files are not exposed publicly.

---

## Issue 13.16 — Implement CV Draft / Review Flow

**Type:** Story

### Scope

Store parser output as a reviewable CV draft.

### Tasks

- Persist parsed draft.
- Persist confidence/evidence where needed.
- Expose draft through API.
- Mark ambiguous fields.
- Support user corrections.
- Convert approved draft into final CV model.

### Acceptance Criteria

- AI output is not immediately treated as final CV data.
- User can review ambiguous fields.
- Approved data can be persisted as the final CV.

---

## Issue 13.17 — Add CV Parser Test Corpus

**Type:** Story

### Scope

Create representative anonymized CV fixtures.

### Tasks

- Collect 20–50 representative CVs where legally/privately appropriate.
- PDF fixtures.
- HTML fixtures.
- Different layouts.
- Different languages.
- Different section names.
- CVs with missing information.
- CVs with multiple projects.
- CVs with ambiguous dates.
- CVs with skills only inside projects.

### Acceptance Criteria

- Automated regression tests can run against representative CVs.
- Parser accuracy can be measured by field/category.

---

## Issue 13.18 — Add Ollama / Parser Observability

**Type:** Task

### Scope

Make parser failures diagnosable without logging sensitive CV content.

### Tasks

- Processing duration metrics.
- AI request duration.
- AI failure count.
- Parsing failure count.
- Import status metrics.
- Model name/version in technical metadata.
- Correlation/import ID.
- Avoid full CV/prompt/response logging.

### Acceptance Criteria

- Parser processing can be monitored.
- Failures can be correlated to an import.
- Sensitive CV content is not written to normal application logs.

---

## Issue 13.19 — Security and Privacy Hardening

**Type:** Story

### Scope

Secure the CV import pipeline.

### Tasks

- Validate uploaded file type.
- Validate file size.
- Prevent path traversal.
- Store uploads outside public web roots.
- Clean temporary files.
- Restrict Ollama network access.
- Avoid sensitive content in logs.
- Define CV source-document retention/deletion behavior.
- Add authorization to import/review endpoints.

### Acceptance Criteria

- Malicious/invalid uploads are rejected.
- CV content cannot be accessed without authorization.
- Ollama is not publicly exposed.
- Sensitive data is not present in standard logs.

---

# 25. Suggested Sprint 13 Sequence

Recommended dependency order:

```text
13.1 Domain / DTOs
      |
      +----> 13.2 PDF extraction
      |
      +----> 13.3 HTML extraction
                 |
                 v
            13.4 Normalization
                 |
                 v
            13.5 Ollama
                 |
                 v
            13.6 Section detection
                 |
        +--------+---------+
        |        |         |
        v        v         v
      13.7     13.8      13.13
      Person   Experience Education/
                         Languages
                 |
                 v
             13.9 Projects
                 |
                 v
             13.10 Skills
                 |
                 v
             13.11 Skill normalization
                 |
                 v
             13.12 Experience calculation
                 |
                 v
             13.14 Validation
                 |
                 v
             13.15 Import API
                 |
                 v
             13.16 Review flow
```

Cross-cutting:

```text
13.17 Test corpus
13.18 Observability
13.19 Security
```

---

# 26. Sprint 13 Definition of Done

Sprint 13 should be considered complete when:

- PDF CV can be uploaded and parsed.
- HTML CV can be uploaded and parsed.
- Both formats produce the same normalized document model.
- Ollama is running locally and integrated through an abstraction.
- CV sections can be detected.
- Personal data can be extracted.
- Employment can be extracted.
- Projects can be extracted.
- Skills can be extracted.
- Skills can be matched to the application's skill catalog.
- Years of experience and last-used dates are calculated by backend code.
- Education and languages can be extracted.
- LLM responses are structurally validated.
- Import output is stored as a draft.
- Ambiguous values are visible for user review.
- Parser errors are recoverable.
- Sensitive CV data is not unnecessarily logged.
- A representative CV test corpus exists.
- Ollama/model configuration can be changed without changing domain logic.

---

# 27. Final Architecture Principle

The implementation should follow this rule:

```text
                LLM
                 |
                 | extracts
                 v
          Evidence / Facts
                 |
                 | normalized by application
                 v
           Domain Model
                 |
                 | calculated by application
                 v
       Derived CV Information
```

In other words:

> **The LLM is an extraction and interpretation component, not the source of truth.**

The application's database/domain model remains the source of truth.

This makes the parser:

- safer
- testable
- deterministic where it matters
- provider-independent
- suitable for local AI
- easier to improve as more CV examples become available
