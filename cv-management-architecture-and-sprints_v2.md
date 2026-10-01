# CV Management Application — Technical Architecture & Delivery Plan

## 1. Overview

A web application for creating, editing, versioning, previewing, exporting, and storing CVs.

### Core capabilities

- Manage multiple persons.
- Manage multiple CVs per person.
- Create immutable CV versions.
- Edit structured CV sections.
- Reorder sections.
- Select CV templates.
- Live preview.
- Export CV to PDF.
- Download files locally.
- Store generated files in cloud storage.
- Restore previous CV versions.
- Compare versions.
- Support multiple languages.
- Provide authentication and authorization in a later phase.

### Recommended stack

| Layer | Technology |
|---|---|
| Frontend | Vue 3 + TypeScript |
| UI | Vuetify 3 |
| State | Pinia |
| Routing | Vue Router |
| HTTP | Axios |
| Backend | Java 17+ / Spring Boot 3.x |
| API | REST / OpenAPI 3 |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| PDF | Chromium/Playwright or OpenHTMLtoPDF |
| Storage abstraction | Local filesystem + S3-compatible storage |
| Build | Maven |
| Testing | JUnit 5, Mockito, Testcontainers, Vitest |
| API documentation | Springdoc OpenAPI |

---

# 2. High-Level Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                         Vue 3 SPA                           │
│                                                             │
│  Persons │ CVs │ Editor │ Versions │ Templates │ Preview   │
└─────────────────────────────┬───────────────────────────────┘
                              │ HTTPS / REST
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                  │
│                                                             │
│  Person │ CV │ Version │ Template │ Export │ Storage       │
│                                                             │
│  REST Controllers                                           │
│  Application Services                                       │
│  Domain                                                   │
│  Repositories                                               │
└───────────────┬─────────────────────┬───────────────────────┘
                │                     │
                ▼                     ▼
        ┌──────────────┐      ┌─────────────────┐
        │ PostgreSQL   │      │ File Storage    │
        │              │      │                 │
        │ CV metadata  │      │ Local / S3      │
        │ CV content   │      │ PDF / assets    │
        └──────────────┘      └─────────────────┘
```

## 2.1 Architectural style

Use a **modular monolith** initially.

Do not start with microservices.

Recommended backend modules:

```text
person
cv
template
export
storage
common
```

This gives clear module boundaries while keeping deployment and development simple.

---

# 3. Domain Model

The central aggregate structure is:

```text
Person
  │
  └── 1:N CV
          │
          └── 1:N CV Version
                    │
                    ├── Personal Information
                    ├── Summary
                    ├── Experience
                    ├── Education
                    ├── Skills
                    ├── Languages
                    ├── Projects
                    ├── Certifications
                    └── Custom Sections
```

A CV version represents an immutable snapshot.

```text
Person
   │
   ├── CV: Java Backend Developer
   │      ├── v1
   │      ├── v2
   │      └── v3
   │
   └── CV: Full Stack Developer
          ├── v1
          └── v2
```

---

# 4. PostgreSQL Database Schema

## 4.1 Entity relationship overview

```text
person
  │
  ├──< person_contact
  │
  └──< cv
          │
          ├──< cv_version
          │       │
          │       ├──< cv_experience
          │       ├──< cv_education
          │       ├──< cv_skill
          │       ├──< cv_language
          │       ├──< cv_project
          │       ├──< cv_certification
          │       └──< cv_custom_section
          │
          └──> cv_template
```

---

# 5. Database Tables

## 5.1 person

```sql
CREATE TABLE person (
    id              UUID PRIMARY KEY,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    date_of_birth   DATE,
    headline        VARCHAR(255),
    photo_storage_key VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
```

## 5.2 person_contact

```sql
CREATE TABLE person_contact (
    id              UUID PRIMARY KEY,
    person_id       UUID NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    type            VARCHAR(50) NOT NULL,
    value           VARCHAR(500) NOT NULL,
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order      INTEGER NOT NULL DEFAULT 0
);
```

Possible contact types:

```text
EMAIL
PHONE
LINKEDIN
GITHUB
WEBSITE
ADDRESS
OTHER
```

---

# 6. CV

```sql
CREATE TABLE cv (
    id              UUID PRIMARY KEY,
    person_id       UUID NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    language        VARCHAR(10) NOT NULL,
    status          VARCHAR(30) NOT NULL,
    template_id     UUID,
    current_version_id UUID,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
```

Possible statuses:

```text
DRAFT
ACTIVE
ARCHIVED
```

---

# 7. CV Version

```sql
CREATE TABLE cv_version (
    id              UUID PRIMARY KEY,
    cv_id           UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    version_number  INTEGER NOT NULL,
    description     VARCHAR(500),
    snapshot        JSONB NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      UUID,
    UNIQUE(cv_id, version_number)
);
```

The `snapshot` contains the complete CV state.

Example:

```json
{
  "personal": {
    "firstName": "John",
    "lastName": "Smith",
    "headline": "Senior Java Developer"
  },
  "summary": "Senior software engineer...",
  "experience": [],
  "education": [],
  "skills": [],
  "languages": [],
  "projects": [],
  "certifications": []
}
```

### Why JSONB?

It makes a CV version a true immutable snapshot.

The current editing model can remain relational while published versions are persisted as complete snapshots.

---

# 8. CV Experience

```sql
CREATE TABLE cv_experience (
    id              UUID PRIMARY KEY,
    cv_id           UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    company         VARCHAR(255) NOT NULL,
    position        VARCHAR(255) NOT NULL,
    location        VARCHAR(255),
    start_date      DATE,
    end_date        DATE,
    current         BOOLEAN NOT NULL DEFAULT FALSE,
    description     TEXT,
    sort_order      INTEGER NOT NULL DEFAULT 0
);
```

---

# 8.1 Experience Projects

Each work experience can contain one or more projects.

```text
Experience
 ├── Company A
 │    ├── Project A
 │    ├── Project B
 │    └── Project C
```

A project contains detailed information about the work performed within the experience entry.

## Project fields

```text
Company
Industries
Project Name
Project Description
Period
Position
Responsibilities
Technologies and Tools
Team Size
External Link
```

Example:

```text
Company: Customer Company
Industries: Cyber Security
Project Name: Jira plugin
Project Description: Jira plugin to integrate with the customer service
Period: 2024-01 — 2025-03
Position: Full Stack Developer
Responsibilities: Develop project from scratch. Testing and quality assurance.
Technologies and Tools: Java, React
Team Size: 6
External Link: https://example.com/project
```

## 8.1.1 Database table

```sql
CREATE TABLE cv_experience_project (
    id                  UUID PRIMARY KEY,
    experience_id       UUID NOT NULL REFERENCES cv_experience(id) ON DELETE CASCADE,
    company             VARCHAR(255),
    industries          TEXT,
    project_name        VARCHAR(255) NOT NULL,
    project_description TEXT,
    period_from         DATE,
    period_to           DATE,
    position            VARCHAR(255),
    responsibilities    TEXT,
    technologies        TEXT,
    team_size           INTEGER,
    external_link       VARCHAR(1000),
    sort_order          INTEGER NOT NULL DEFAULT 0
);
```

The project belongs to one experience entry, allowing multiple projects under one company/position.

# 9. CV Education

```sql
CREATE TABLE cv_education (
    id              UUID PRIMARY KEY,
    cv_id           UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    institution     VARCHAR(255) NOT NULL,
    degree          VARCHAR(255),
    field_of_study  VARCHAR(255),
    start_date      DATE,
    end_date        DATE,
    description     TEXT,
    sort_order      INTEGER NOT NULL DEFAULT 0
);
```

---

# 10. Skill Groups and Skills

Skills are organized into groups. Each group contains the concrete skills selected for a particular CV.

Example:

```text
Backend
 ├── Java
 ├── Spring Boot
 ├── Spring Security
 └── Hibernate

Frontend
 ├── Vue.js
 ├── React
 └── TypeScript

Databases
 ├── PostgreSQL
 ├── Redis
 └── Elasticsearch
```

## 10.1 Skill Group

```sql
CREATE TABLE cv_skill_group (
    id              UUID PRIMARY KEY,
    cv_id           UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    sort_order      INTEGER NOT NULL DEFAULT 0
);
```

## 10.2 Skill

```sql
CREATE TABLE cv_skill (
    id              UUID PRIMARY KEY,
    skill_group_id  UUID NOT NULL REFERENCES cv_skill_group(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    level           VARCHAR(50),
    sort_order      INTEGER NOT NULL DEFAULT 0
);
```

Example UI:

```text
Skill Group: Backend
    [x] Java
    [x] Spring Boot
    [x] Spring Security
    [+ Add skill]

Skill Group: Frontend
    [x] Vue.js
    [x] TypeScript
    [+ Add skill]

[+ Add skill group]
```

The group controls categorization and ordering; the skill stores the concrete skill and optional proficiency level.

# 11. CV Languages

```sql
CREATE TABLE cv_language (
    id              UUID PRIMARY KEY,
    cv_id           UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    language        VARCHAR(100) NOT NULL,
    level           VARCHAR(50),
    sort_order      INTEGER NOT NULL DEFAULT 0
);
```

---

# 12. CV Projects

```sql
CREATE TABLE cv_project (
    id              UUID PRIMARY KEY,
    cv_id           UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    role            VARCHAR(255),
    description     TEXT,
    technologies    TEXT,
    url             VARCHAR(500),
    sort_order      INTEGER NOT NULL DEFAULT 0
);
```

---

# 13. CV Certifications

```sql
CREATE TABLE cv_certification (
    id              UUID PRIMARY KEY,
    cv_id           UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    issuer          VARCHAR(255),
    issue_date      DATE,
    expiry_date     DATE,
    credential_id   VARCHAR(255),
    credential_url  VARCHAR(500),
    sort_order      INTEGER NOT NULL DEFAULT 0
);
```

---

# 14. Custom Sections

```sql
CREATE TABLE cv_custom_section (
    id              UUID PRIMARY KEY,
    cv_id           UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    content         TEXT,
    sort_order      INTEGER NOT NULL DEFAULT 0
);
```

---

# 15. CV Section Configuration

To support arbitrary ordering:

```sql
CREATE TABLE cv_section (
    id              UUID PRIMARY KEY,
    cv_id           UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    section_type    VARCHAR(50) NOT NULL,
    visible         BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INTEGER NOT NULL
);
```

Example:

```text
SUMMARY       1
EXPERIENCE    2
SKILLS        3
PROJECTS      4
EDUCATION     5
LANGUAGES     6
```

---

# 16. Templates

```sql
CREATE TABLE cv_template (
    id              UUID PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    template_key    VARCHAR(100) NOT NULL UNIQUE,
    version         INTEGER NOT NULL DEFAULT 1,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
```

Example:

```text
modern
classic
minimal
two-column
```

---

# 17. Exports

```sql
CREATE TABLE cv_export (
    id                  UUID PRIMARY KEY,
    cv_version_id       UUID NOT NULL REFERENCES cv_version(id),
    format              VARCHAR(20) NOT NULL,
    storage_type        VARCHAR(30) NOT NULL,
    storage_key         VARCHAR(500),
    file_name            VARCHAR(500),
    file_size            BIGINT,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL
);
```

Possible values:

```text
format:
PDF
DOCX
JSON

storage_type:
LOCAL
S3
```

---

# 18. Backend Package Structure

Recommended package-by-feature structure:

```text
com.example.cv
│
├── CvApplication.java
│
├── common/
│   ├── config/
│   │   ├── JacksonConfig.java
│   │   ├── JpaConfig.java
│   │   └── OpenApiConfig.java
│   │
│   ├── exception/
│   │   ├── ApiExceptionHandler.java
│   │   ├── NotFoundException.java
│   │   └── ValidationException.java
│   │
│   ├── security/
│   └── util/
│
├── person/
│   ├── controller/
│   │   └── PersonController.java
│   ├── service/
│   │   ├── PersonService.java
│   │   └── PersonServiceImpl.java
│   ├── repository/
│   │   └── PersonRepository.java
│   ├── entity/
│   │   ├── Person.java
│   │   └── PersonContact.java
│   └── dto/
│       ├── PersonResponse.java
│       ├── CreatePersonRequest.java
│       └── UpdatePersonRequest.java
│
├── cv/
│   ├── controller/
│   │   ├── CvController.java
│   │   └── CvVersionController.java
│   │
│   ├── service/
│   │   ├── CvService.java
│   │   ├── CvVersionService.java
│   │   └── CvSnapshotService.java
│   │
│   ├── repository/
│   │   ├── CvRepository.java
│   │   └── CvVersionRepository.java
│   │
│   ├── entity/
│   │   ├── Cv.java
│   │   ├── CvVersion.java
│   │   ├── CvExperience.java
│   │   ├── CvEducation.java
│   │   ├── CvSkill.java
│   │   ├── CvLanguage.java
│   │   ├── CvProject.java
│   │   ├── CvCertification.java
│   │   ├── CvSection.java
│   │   └── CvCustomSection.java
│   │
│   └── dto/
│       ├── CvResponse.java
│       ├── CreateCvRequest.java
│       ├── UpdateCvRequest.java
│       ├── CvVersionResponse.java
│       └── CreateVersionRequest.java
│
├── template/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── export/
│   ├── controller/
│   ├── service/
│   │   ├── PdfExportService.java
│   │   └── ExportService.java
│   ├── renderer/
│   │   ├── HtmlRenderer.java
│   │   └── PdfRenderer.java
│   ├── entity/
│   │   └── CvExport.java
│   └── repository/
│       └── CvExportRepository.java
│
└── storage/
    ├── FileStorage.java
    ├── LocalFileStorage.java
    ├── S3FileStorage.java
    └── StoredFile.java
```

---

# 19. Backend Layer Responsibilities

## Controller

Responsible for:

- HTTP;
- authentication;
- request validation;
- mapping DTOs;
- HTTP response codes.

Controllers should not contain business logic.

## Service

Responsible for:

- business rules;
- transactions;
- version creation;
- restore;
- validation;
- orchestration.

## Repository

Responsible for:

- persistence;
- database queries.

## Entity

Responsible for:

- persistence model;
- relationships;
- entity-level invariants.

## DTO

Never expose JPA entities directly through REST.

Use:

```text
Request DTO
Response DTO
```

---

# 20. REST API

## Person API

### List persons

```http
GET /api/v1/persons
```

Response:

```json
{
  "items": [
    {
      "id": "uuid",
      "firstName": "John",
      "lastName": "Smith",
      "headline": "Senior Java Developer"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

### Create person

```http
POST /api/v1/persons
Content-Type: application/json
```

```json
{
  "firstName": "John",
  "lastName": "Smith",
  "headline": "Senior Java Developer"
}
```

### Get person

```http
GET /api/v1/persons/{personId}
```

### Update person

```http
PUT /api/v1/persons/{personId}
```

### Delete person

```http
DELETE /api/v1/persons/{personId}
```

---

# 21. CV API

### List CVs

```http
GET /api/v1/persons/{personId}/cvs
```

### Create CV

```http
POST /api/v1/persons/{personId}/cvs
```

```json
{
  "name": "Java Backend Developer",
  "language": "en",
  "templateId": "uuid"
}
```

### Get CV

```http
GET /api/v1/cvs/{cvId}
```

### Update CV

```http
PUT /api/v1/cvs/{cvId}
```

### Delete CV

```http
DELETE /api/v1/cvs/{cvId}
```

---

# 22. CV Editor API

The editor can update the complete current CV:

```http
PUT /api/v1/cvs/{cvId}/content
```

Example:

```json
{
  "summary": "Senior Java developer...",
  "experience": [
    {
      "company": "ABC",
      "position": "Senior Java Developer",
      "startDate": "2022-01-01",
      "current": true,
      "description": "..."
    }
  ],
  "skillGroups": [
    {
      "name": "Backend",
      "skills": [
        { "name": "Java", "level": "Expert" },
        { "name": "Spring Boot", "level": "Expert" }
      ]
    }
  ],
  "sections": [
    {
      "type": "SUMMARY",
      "visible": true,
      "sortOrder": 1
    },
    {
      "type": "EXPERIENCE",
      "visible": true,
      "sortOrder": 2
    }
  ]
}
```

---

# 22.1 Experience Projects API

### List projects

```http
GET /api/v1/experiences/{experienceId}/projects
```

### Create project

```http
POST /api/v1/experiences/{experienceId}/projects
```

```json
{
  "company": "Customer Company",
  "industries": "Cyber Security",
  "projectName": "Jira plugin",
  "projectDescription": "Jira plugin to integrate with the customer service",
  "periodFrom": "2024-01-01",
  "periodTo": "2025-03-31",
  "position": "Full Stack Developer",
  "responsibilities": "Develop project from scratch. Testing and quality assurance.",
  "technologies": ["Java", "React"],
  "teamSize": 6,
  "externalLink": "https://example.com/project"
}
```

### Update / delete project

```http
PUT    /api/v1/experience-projects/{projectId}
DELETE /api/v1/experience-projects/{projectId}
```

### Reorder projects

```http
PUT /api/v1/experiences/{experienceId}/projects/order
```

# 22.2 Skill Groups API

### List skill groups

```http
GET /api/v1/cvs/{cvId}/skill-groups
```

### Create / update / delete skill group

```http
POST   /api/v1/cvs/{cvId}/skill-groups
PUT    /api/v1/skill-groups/{skillGroupId}
DELETE /api/v1/skill-groups/{skillGroupId}
```

Create request:

```json
{
  "name": "Backend"
}
```

### Skills within a group

```http
POST   /api/v1/skill-groups/{skillGroupId}/skills
PUT    /api/v1/skills/{skillId}
DELETE /api/v1/skills/{skillId}
```

Create skill request:

```json
{
  "name": "Java",
  "level": "Expert"
}
```

### Reorder groups and skills

```http
PUT /api/v1/cvs/{cvId}/skill-groups/order
PUT /api/v1/skill-groups/{skillGroupId}/skills/order
```

# 23. Version API

### List versions

```http
GET /api/v1/cvs/{cvId}/versions
```

### Create version

```http
POST /api/v1/cvs/{cvId}/versions
```

```json
{
  "description": "Banking version"
}
```

### Get version

```http
GET /api/v1/cv-versions/{versionId}
```

### Restore version

```http
POST /api/v1/cv-versions/{versionId}/restore
```

Recommended behavior:

Do not mutate the historical version.

Instead:

```text
v1
v2
v3

restore v1
   ↓
create v4 based on v1
```

This preserves history.

---

# 24. Version Diff API

```http
GET /api/v1/cv-versions/{fromVersionId}/diff/{toVersionId}
```

Example response:

```json
{
  "changes": [
    {
      "type": "ADDED",
      "section": "SKILLS",
      "path": "skills[3]",
      "value": "Kafka"
    },
    {
      "type": "MODIFIED",
      "section": "SUMMARY",
      "oldValue": "Java developer",
      "newValue": "Senior Java developer"
    }
  ]
}
```

---

# 25. Template API

```http
GET /api/v1/templates
GET /api/v1/templates/{templateId}
```

Admin-only:

```http
POST   /api/v1/templates
PUT    /api/v1/templates/{templateId}
DELETE /api/v1/templates/{templateId}
```

---

# 26. Preview API

For server-side preview:

```http
GET /api/v1/cv-versions/{versionId}/preview
```

Possible response:

```text
text/html
```

Alternatively, Vue can render the preview locally from the same CV DTO.

Recommended:

```text
Vue editor
    ↓
Vue template
    ↓
Live preview

PDF export
    ↓
Backend template renderer
    ↓
PDF
```

Keep preview and PDF templates visually consistent.

---

# 27. Export API

### Generate PDF

```http
POST /api/v1/cv-versions/{versionId}/exports/pdf
```

Response:

```json
{
  "id": "uuid",
  "format": "PDF",
  "fileName": "John_Smith_Java_Backend_v3.pdf",
  "storageType": "LOCAL",
  "downloadUrl": "/api/v1/exports/uuid/download"
}
```

### Download export

```http
GET /api/v1/exports/{exportId}/download
```

### List exports

```http
GET /api/v1/cv-versions/{versionId}/exports
```

---

# 28. Storage API

Internal abstraction:

```java
public interface FileStorage {

    StoredFile upload(
        String path,
        InputStream content,
        String contentType
    );

    InputStream download(String path);

    void delete(String path);

    boolean exists(String path);
}
```

Implementations:

```text
LocalFileStorage
S3FileStorage
```

Configuration:

```yaml
storage:
  type: local
  local:
    base-path: ./data/files
```

or:

```yaml
storage:
  type: s3
  s3:
    bucket: cv-files
```

---

# 29. Vue 3 Package Structure

Recommended feature-based structure:

```text
src/
├── app/
│   ├── App.vue
│   ├── router.ts
│   └── providers/
│
├── layouts/
│   ├── MainLayout.vue
│   └── EditorLayout.vue
│
├── modules/
│   │
│   ├── persons/
│   │   ├── api/
│   │   │   └── personApi.ts
│   │   ├── components/
│   │   │   ├── PersonForm.vue
│   │   │   ├── PersonCard.vue
│   │   │   └── PersonList.vue
│   │   ├── pages/
│   │   │   ├── PersonListPage.vue
│   │   │   ├── PersonCreatePage.vue
│   │   │   └── PersonEditPage.vue
│   │   ├── stores/
│   │   │   └── personStore.ts
│   │   └── types/
│   │       └── person.ts
│   │
│   ├── cvs/
│   │   ├── api/
│   │   │   ├── cvApi.ts
│   │   │   └── cvVersionApi.ts
│   │   ├── components/
│   │   │   ├── CvEditor.vue
│   │   │   ├── CvHeader.vue
│   │   │   ├── CvSectionList.vue
│   │   │   ├── CvSectionItem.vue
│   │   │   ├── ExperienceEditor.vue
│   │   │   ├── EducationEditor.vue
│   │   │   ├── SkillsEditor.vue
│   │   │   ├── LanguagesEditor.vue
│   │   │   ├── ProjectsEditor.vue
│   │   │   └── CertificationsEditor.vue
│   │   ├── pages/
│   │   │   ├── CvListPage.vue
│   │   │   ├── CvCreatePage.vue
│   │   │   ├── CvEditPage.vue
│   │   │   └── CvVersionsPage.vue
│   │   ├── stores/
│   │   │   └── cvEditorStore.ts
│   │   └── types/
│   │       └── cv.ts
│   │
│   ├── templates/
│   │   ├── components/
│   │   │   ├── TemplateSelector.vue
│   │   │   └── TemplatePreview.vue
│   │   ├── templates/
│   │   │   ├── ModernTemplate.vue
│   │   │   ├── ClassicTemplate.vue
│   │   │   └── MinimalTemplate.vue
│   │   └── api/
│   │       └── templateApi.ts
│   │
│   ├── preview/
│   │   └── components/
│   │       └── CvPreview.vue
│   │
│   └── exports/
│       ├── api/
│       │   └── exportApi.ts
│       ├── components/
│       │   ├── ExportDialog.vue
│       │   └── ExportHistory.vue
│       └── pages/
│           └── ExportPage.vue
│
├── shared/
│   ├── api/
│   │   ├── axios.ts
│   │   └── apiError.ts
│   ├── components/
│   │   ├── AppDialog.vue
│   │   ├── AppLoading.vue
│   │   └── AppConfirmDialog.vue
│   ├── composables/
│   │   ├── useDirtyState.ts
│   │   └── useAsync.ts
│   ├── types/
│   └── utils/
│
├── stores/
│   └── appStore.ts
│
└── styles/
```

---

# 30. Vue Router

Example routes:

```text
/persons
/persons/new
/persons/:personId
/persons/:personId/cvs
/persons/:personId/cvs/new

/cvs/:cvId
/cvs/:cvId/edit
/cvs/:cvId/versions
/cvs/:cvId/preview

/cv-versions/:versionId
/cv-versions/:versionId/preview
/cv-versions/:versionId/export
```

---

# 31. Vue Editor Architecture

The main editor:

```text
CvEditPage
      │
      ▼
CvEditor
      │
      ├── CvHeader
      │
      ├── CvSectionList
      │      │
      │      ├── SummaryEditor
      │      ├── ExperienceEditor
      │      ├── EducationEditor
      │      ├── SkillsEditor
      │      ├── LanguagesEditor
      │      ├── ProjectsEditor
      │      └── CertificationsEditor
      │
      └── CvPreview
```

Two-column layout:

```text
┌───────────────────────────┬───────────────────────────┐
│                           │                           │
│       CV EDITOR           │       LIVE PREVIEW        │
│                           │                           │
│ Summary                   │ John Smith                │
│ ┌───────────────────────┐ │ Senior Java Developer    │
│ │ ...                   │ │                           │
│ └───────────────────────┘ │ SUMMARY                   │
│                           │ ...                       │
│ Experience               │                           │
│ ┌───────────────────────┐ │ EXPERIENCE                │
│ │ Company A             │ │ Company A                 │
│ │ Senior Developer      │ │ Senior Developer         │
│ └───────────────────────┘ │                           │
│                           │ SKILLS                    │
│ Skills                    │ Java | Spring | Kafka     │
│ [Java] [Spring] [+ Add]  │                           │
│                           │                           │
└───────────────────────────┴───────────────────────────┘
```

---

# 32. Pinia Store

Example:

```ts
export const useCvEditorStore = defineStore('cvEditor', () => {
  const cv = ref<Cv | null>(null)
  const loading = ref(false)
  const saving = ref(false)
  const dirty = ref(false)

  async function load(id: string) {
    loading.value = true

    try {
      cv.value = await cvApi.get(id)
      dirty.value = false
    } finally {
      loading.value = false
    }
  }

  async function save() {
    if (!cv.value) return

    saving.value = true

    try {
      await cvApi.updateContent(cv.value.id, cv.value)
      dirty.value = false
    } finally {
      saving.value = false
    }
  }

  return {
    cv,
    loading,
    saving,
    dirty,
    load,
    save
  }
})
```

---

# 33. Dirty State

The editor should prevent accidental navigation when there are unsaved changes.

```text
User edits CV
      ↓
dirty = true
      ↓
User navigates away
      ↓
Confirm dialog
      ↓
Save / Discard / Cancel
```

Use a reusable composable:

```ts
useDirtyState()
```

---

# 34. Validation

Frontend validation:

```text
Vue form
    ↓
client-side validation
```

Backend validation remains authoritative:

```java
@NotBlank
private String firstName;
```

Use:

```text
Jakarta Bean Validation
```

The backend should never trust frontend validation.

---

# 35. CV Versioning Workflow

Recommended workflow:

```text
                    ┌──────────────┐
                    │ Current CV   │
                    └──────┬───────┘
                           │
                        Edit
                           │
                           ▼
                    ┌──────────────┐
                    │ Draft state  │
                    └──────┬───────┘
                           │
                    Create Version
                           │
                           ▼
                    ┌──────────────┐
                    │ Immutable v3 │
                    └──────────────┘
```

Historical versions must never be modified.

Restore means:

```text
v1 → restore → v4
```

not:

```text
v1 modified
```

---

# 36. PDF Export Pipeline

```text
CV Version
     │
     ▼
Load snapshot
     │
     ▼
Select template
     │
     ▼
Render HTML
     │
     ▼
Chromium / PDF Renderer
     │
     ▼
PDF bytes
     │
     ├──────────────► LocalFileStorage
     │
     └──────────────► S3FileStorage
     │
     ▼
Create cv_export record
```

Generated filename:

```text
{FirstName}_{LastName}_{CvName}_v{Version}.pdf
```

Example:

```text
John_Smith_Java_Backend_v3.pdf
```

---

# 37. Security

Initial security can be simple, but the architecture should allow authentication.

Later:

```text
Vue
  ↓
OIDC / OAuth2
  ↓
Spring Security
  ↓
User
  ↓
Person ownership
```

Important authorization rule:

```text
User A cannot access User B's persons/CVs.
```

Every query should be scoped to the authenticated user.

Recommended future tables:

```text
app_user
user_person
```

---

# 38. Testing Strategy

## Backend

### Unit tests

- service business rules;
- snapshot creation;
- version restore;
- validation;
- export naming.

### Integration tests

- PostgreSQL via Testcontainers;
- JPA repositories;
- REST controllers;
- Flyway migrations.

### API tests

```text
POST person
POST CV
PUT CV
POST version
GET versions
POST restore
POST export
GET download
```

## Frontend

### Unit tests

Vitest:

- stores;
- composables;
- form validation;
- components.

### E2E

Playwright:

```text
Create person
    ↓
Create CV
    ↓
Edit CV
    ↓
Create version
    ↓
Preview
    ↓
Export PDF
    ↓
Download
```

---

# 39. Sprint Plan

Recommended sprint duration: **2 weeks**.

The plan below assumes one full-stack developer or a small team. Ticket estimates are relative and should be adjusted after the first sprint.

---

# Sprint 1 — Project Foundation

### Goal

Create runnable Spring Boot + Vue 3 + PostgreSQL application.

### Tickets

#### CV-001 — Create Git repository structure

- Create frontend/backend directories.
- Add README.
- Add `.gitignore`.
- Add environment configuration.

#### CV-002 — Initialize Spring Boot project

- Java 17+.
- Spring Web.
- Spring Data JPA.
- Validation.
- Actuator.
- PostgreSQL driver.
- Flyway.
- OpenAPI.

#### CV-003 — Initialize Vue 3 project

- Vue 3.
- TypeScript.
- Vite.
- Vue Router.
- Pinia.
- Vuetify.

#### CV-004 — Docker Compose development environment

Add:

```text
PostgreSQL
```

Optional:

```text
MinIO
```

#### CV-005 — Configure database connection

- application-local.yml
- environment variables
- connection pool.

#### CV-006 — Configure Flyway

Create:

```text
V1__initial_schema.sql
```

#### CV-007 — Configure Axios client

Create:

```text
src/shared/api/axios.ts
```

#### CV-008 — Configure global error handling

Backend:

```text
@RestControllerAdvice
```

Frontend:

```text
API error interceptor
```

### Sprint result

```text
Vue → Spring Boot → PostgreSQL
```

is working.

---

# Sprint 2 — Person Management

### Goal

Implement multiple persons.

### Tickets

#### CV-010 — Create Person entity

#### CV-011 — Create PersonContact entity

#### CV-012 — Create Person repository

#### CV-013 — Implement Person service

#### CV-014 — Implement Person REST controller

Endpoints:

```text
GET
POST
GET/{id}
PUT/{id}
DELETE/{id}
```

#### CV-015 — Create Person DTOs

#### CV-016 — Add backend validation

#### CV-017 — Create Person API client

```text
personApi.ts
```

#### CV-018 — Create Person store

```text
personStore.ts
```

#### CV-019 — Create Person list page

#### CV-020 — Create Person form

#### CV-021 — Create Person edit page

#### CV-022 — Add delete confirmation

### Sprint result

User can:

```text
Create person
Edit person
Delete person
List persons
```

---

# Sprint 3 — CV Management

### Goal

Create multiple CVs per person.

### Tickets

#### CV-030 — Create CV entity

#### CV-031 — Create CV repository

#### CV-032 — Implement CV service

#### CV-033 — Implement CV REST controller

#### CV-034 — Implement CV DTOs

#### CV-035 — Implement person → CV relationship

#### CV-036 — Create CV list endpoint

#### CV-037 — Create CV API client

#### CV-038 — Create CV Pinia store

#### CV-039 — Create CV list page

#### CV-040 — Create CV creation page

#### CV-041 — Create CV edit page

#### CV-042 — Add CV status

```text
DRAFT
ACTIVE
ARCHIVED
```

### Sprint result

```text
Person
 ├── CV 1
 ├── CV 2
 └── CV 3
```

---

# Sprint 4 — CV Content Editor

### Goal

Implement structured CV editing.

### Tickets

#### CV-050 — Create CV experience entity

#### CV-051 — Create CV education entity

#### CV-052 — Create CV skill entity

#### CV-053 — Create CV language entity

#### CV-054 — Create CV project entity

#### CV-055 — Create CV certification entity

#### CV-056 — Create CV custom section entity

#### CV-057 — Create CV section configuration

#### CV-058 — Implement complete CV content endpoint

```text
PUT /api/v1/cvs/{cvId}/content
```

#### CV-059 — Implement ExperienceEditor.vue

#### CV-060 — Implement EducationEditor.vue

#### CV-061 — Implement SkillsEditor.vue

#### CV-062 — Implement LanguagesEditor.vue

#### CV-063 — Implement ProjectsEditor.vue

#### CV-064 — Implement CertificationsEditor.vue

#### CV-065 — Implement section ordering

Use drag & drop.

#### CV-066 — Implement section visibility

#### CV-067 — Add frontend validation

### Sprint result

A user can create a complete structured CV.

---

### Additional Sprint 4 tickets

#### CV-072 — Implement experience project persistence

Support multiple projects per experience with company, industry, project name, description, period, position, responsibilities, technologies/tools, team size, external link, and ordering.

#### CV-073 — Implement SkillGroup entity and repository

#### CV-074 — Implement Skill entity and repository

#### CV-075 — Implement skill group CRUD API

#### CV-076 — Implement skill CRUD API inside a skill group

#### CV-077 — Implement ExperienceProjectEditor.vue

#### CV-078 — Implement SkillGroupsEditor.vue

#### CV-079 — Implement SkillGroupEditor.vue

#### CV-080 — Add frontend validation for project and skill group forms

#### CV-081 — Add ordering for projects, skill groups, and skills

# Sprint 5 — CV Versions

### Goal

Implement immutable CV snapshots.

### Tickets

#### CV-070 — Create CvVersion entity

#### CV-071 — Add JSONB snapshot support

#### CV-072 — Implement snapshot serializer

#### CV-073 — Implement snapshot creation

#### CV-074 — Implement version numbering

#### CV-075 — Create version endpoint

```text
POST /api/v1/cvs/{cvId}/versions
```

#### CV-076 — Create version history endpoint

#### CV-077 — Create CvVersionsPage.vue

#### CV-078 — Create version details page

#### CV-079 — Implement restore as new version

#### CV-080 — Add version description

#### CV-081 — Prevent historical version modification

### Sprint result

```text
CV
 ├── v1
 ├── v2
 └── v3
```

with immutable history.

---

# Sprint 6 — Templates and Live Preview

### Goal

Introduce reusable CV templates.

### Tickets

#### CV-090 — Create CV template entity

#### CV-091 — Create template repository

#### CV-092 — Implement template API

#### CV-093 — Create TemplateSelector.vue

#### CV-094 — Implement ModernTemplate.vue

#### CV-095 — Implement ClassicTemplate.vue

#### CV-096 — Implement MinimalTemplate.vue

#### CV-097 — Create CvPreview.vue

#### CV-098 — Implement live preview

#### CV-099 — Connect editor and preview

#### CV-100 — Add responsive preview

### Sprint result

User can edit CV and see a live formatted preview.

---

# Sprint 7 — PDF Export

### Goal

Generate production-quality PDF files.

### Tickets

#### CV-110 — Design PDF rendering abstraction

```text
PdfRenderer
```

#### CV-111 — Implement HTML renderer

#### CV-112 — Integrate Chromium/OpenHTMLtoPDF

#### CV-113 — Implement PDF generation

#### CV-114 — Create CvExport entity

#### CV-115 — Implement export service

#### CV-116 — Implement export endpoint

```text
POST /api/v1/cv-versions/{versionId}/exports/pdf
```

#### CV-117 — Implement download endpoint

#### CV-118 — Implement export history

#### CV-119 — Generate deterministic file names

#### CV-120 — Test PDF layout

### Sprint result

```text
CV Version
    ↓
PDF
    ↓
Download
```

---

# Sprint 8 — File Storage

### Goal

Support local filesystem and cloud storage.

### Tickets

#### CV-130 — Create FileStorage interface

#### CV-131 — Implement LocalFileStorage

#### CV-132 — Add storage configuration

#### CV-133 — Store generated PDFs locally

#### CV-134 — Implement S3FileStorage

#### CV-135 — Add S3/MinIO Docker environment

#### CV-136 — Make storage provider configurable

#### CV-137 — Add storage integration tests

### Sprint result

The same application can use:

```text
Local filesystem
```

or:

```text
S3-compatible storage
```

without changing business logic.

---

# Sprint 9 — Version Diff and Advanced Editing

### Goal

Improve version management.

### Tickets

#### CV-140 — Implement JSON snapshot diff

#### CV-141 — Create version diff API

#### CV-142 — Create VersionDiffPage.vue

#### CV-143 — Highlight added fields

#### CV-144 — Highlight removed fields

#### CV-145 — Highlight modified fields

#### CV-146 — Add CV duplication

#### CV-147 — Add version branching metadata

#### CV-148 — Improve autosave/draft handling

### Sprint result

Users can understand what changed between versions.

---

# Sprint 10 — Authentication and Authorization

### Goal

Protect user data.

### Tickets

#### CV-150 — Add Spring Security

#### CV-151 — Add OIDC/OAuth2 integration

#### CV-152 — Add authenticated user model

#### CV-153 — Associate Person with user

#### CV-154 — Implement ownership checks

#### CV-155 — Protect REST endpoints

#### CV-156 — Add frontend authentication flow

#### CV-157 — Add route guards

#### CV-158 — Add unauthorized/forbidden handling

### Sprint result

Users can only access their own CV data.

---

# Sprint 11 — Quality and Production Readiness

### Goal

Prepare the application for deployment.

### Tickets

#### CV-170 — Add Testcontainers PostgreSQL tests

#### CV-171 — Add REST integration tests

#### CV-172 — Add Vue component tests

#### CV-173 — Add Playwright E2E tests

#### CV-174 — Add Testcontainers/MinIO tests

#### CV-175 — Add database indexes

#### CV-176 — Review N+1 queries

#### CV-177 — Add structured logging

#### CV-178 — Add Actuator health checks

#### CV-179 — Add metrics

#### CV-180 — Add OpenAPI documentation

#### CV-181 — Add Docker image

#### CV-182 — Create production configuration

#### CV-183 — Add CI pipeline

---

# Sprint 12 — Optional Advanced Features

### Goal

Additional functionality after MVP.

### Tickets

#### CV-190 — DOCX export

#### CV-191 — JSON import

#### CV-192 — JSON export

#### CV-193 — CV duplication between persons

#### CV-194 — CV tags

#### CV-195 — Search CVs

#### CV-196 — Multiple UI languages

#### CV-197 — Multiple CV languages

#### CV-198 — Public/shareable CV link

#### CV-199 — CV analytics

#### CV-200 — Cloud synchronization

---

# 40. MVP Definition

The MVP should be considered complete after Sprint 8.

Required functionality:

```text
✓ Multiple persons
✓ Multiple CVs per person
✓ Structured CV editor
✓ Experience
✓ Experience projects
✓ Education
✓ Skill groups
✓ Skills within groups
✓ Languages
✓ Projects
✓ Certifications
✓ Custom sections
✓ Section ordering
✓ CV versions
✓ Immutable snapshots
✓ Version restore
✓ Templates
✓ Live preview
✓ PDF export
✓ Local storage
✓ S3-compatible storage
```

Authentication can be included in the MVP if the application is intended for multi-user cloud deployment.

---

# 41. Recommended Development Order

```text
Sprint 1
   ↓
Foundation
   ↓
Sprint 2
   ↓
Persons
   ↓
Sprint 3
   ↓
CV
   ↓
Sprint 4
   ↓
CV Editor
   ↓
Sprint 5
   ↓
Versions
   ↓
Sprint 6
   ↓
Templates + Preview
   ↓
Sprint 7
   ↓
PDF
   ↓
Sprint 8
   ↓
Storage
   ↓
Sprint 9+
   ↓
Advanced features
```

---

# 42. Key Architectural Decisions

## Decision 1 — Modular monolith

Start with one Spring Boot application.

Reason:

- simpler deployment;
- simpler transactions;
- easier development;
- no unnecessary network calls;
- modules can later be extracted.

## Decision 2 — Version snapshots

Store historical CV versions as immutable JSONB snapshots.

Reason:

- easy restore;
- historical data cannot accidentally change;
- simple version export;
- straightforward diff;
- template-independent data.

## Decision 3 — Storage abstraction

Use:

```java
FileStorage
```

instead of coupling the application to S3 or the local filesystem.

## Decision 4 — Template separation

CV data and visual representation must be separate.

```text
CV data
   +
Template
   =
Rendered CV
```

## Decision 5 — REST DTOs

Do not expose JPA entities directly.

```text
HTTP
 ↓
DTO
 ↓
Service
 ↓
Entity
 ↓
Repository
```

## Decision 6 — Historical versions are immutable

Restore always creates a new version.

```text
v1
v2
v3

restore v1

v1
v2
v3
v4 ← copy of v1
```

---

# 43. Final Architecture

```text
                         ┌─────────────────────┐
                         │       Vue 3         │
                         │                     │
                         │ Persons             │
                         │ CVs                 │
                         │ Editor              │
                         │ Templates           │
                         │ Preview             │
                         │ Versions            │
                         │ Export              │
                         └──────────┬──────────┘
                                    │
                                  REST
                                    │
                                    ▼
                    ┌─────────────────────────────┐
                    │       Spring Boot           │
                    │                             │
                    │ ┌─────────┐ ┌────────────┐ │
                    │ │ Person  │ │    CV      │ │
                    │ └─────────┘ └────────────┘ │
                    │                             │
                    │ ┌─────────┐ ┌────────────┐ │
                    │ │Template │ │   Export   │ │
                    │ └─────────┘ └────────────┘ │
                    │                             │
                    │ ┌─────────────────────────┐ │
                    │ │      FileStorage        │ │
                    │ └─────────────────────────┘ │
                    └───────────┬─────────────────┘
                                │
                    ┌───────────┴───────────┐
                    │                       │
                    ▼                       ▼
             ┌─────────────┐        ┌─────────────┐
             │ PostgreSQL  │        │ Local / S3  │
             │             │        │             │
             │ CV data     │        │ PDF         │
             │ Snapshots   │        │ Photos      │
             │ Metadata    │        │ Assets      │
             └─────────────┘        └─────────────┘
```

This architecture keeps the first implementation simple enough to develop as a modular monolith, while leaving clear boundaries for later extraction of export, storage, or other modules into separate services.
