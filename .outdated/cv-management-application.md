# CV Management Application

Приложение для создания и редактирования CV с поддержкой нескольких персон, нескольких версий CV, шаблонов, экспорта в PDF и сохранения файлов локально или в облачное хранилище.

## 1. Основная модель

```text
Person
 ├── Personal information
 ├── Contacts
 ├── Photo
 └── CVs
      ├── CV #1
      │    ├── Version 1
      │    ├── Version 2
      │    └── Version 3
      │
      └── CV #2
           ├── Version 1
           └── Version 2
```

Пример:

```text
Person: John Smith

  CV: Java Backend Developer
      ├── v1 — General
      ├── v2 — Senior Java
      └── v3 — Banking

  CV: Full Stack Developer
      ├── v1
      └── v2 — Vue + Java
```

Версия CV должна быть immutable snapshot либо иметь полноценную историю изменений.

## 2. Основные возможности

### Persons

- создание нескольких персон;
- редактирование;
- удаление;
- копирование персоны;
- photo/avatar;
- personal information;
- контакты;
- ссылки:
  - LinkedIn
  - GitHub
  - website;
- разные настройки CV для каждой персоны.

### CV

Для каждой персоны:

- несколько CV;
- название CV;
- язык;
- template;
- статус:
  - Draft
  - Published
  - Archived;
- дата создания/изменения.

### CV sections

Например:

```text
Personal Information
Summary
Skills
Experience
Education
Certifications
Languages
Projects
Achievements
Publications
References
Custom Sections
```

Порядок секций должен быть изменяемым.

## 3. Версии

Для каждой версии:

```text
Edit
Duplicate
Create version
Restore version
Compare versions
Export version
```

Рекомендуется snapshot approach:

```text
CV Version
    |
    +-- complete CV data snapshot
```

Например:

```text
Java Backend Developer

v1
Created: 2026-01-10
Description: Original CV

v2
Created: 2026-03-15
Description: Added Kafka experience

v3
Created: 2026-06-20
Description: Banking version
```

## 4. Хранение данных

Основная БД:

```text
PostgreSQL
```

Возможные таблицы:

```text
person
person_contact
cv
cv_version
cv_section
cv_experience
cv_education
cv_skill
cv_language
cv_project
cv_certification
```

Структурированные данные лучше хранить в нормализованной модели.

Например:

```text
cv_experience

id
cv_version_id
company
position
start_date
end_date
description
sort_order
```

И:

```text
cv_skill

id
cv_version_id
name
category
level
sort_order
```

### Snapshot

Возможны два варианта.

#### Вариант A — копирование entities

При создании v2 копируются связанные данные для нового `cv_version_id`.

Плюсы:

- легко работать;
- SQL остаётся понятным;
- легко делать queries.

Минус — больше данных.

#### Вариант B — JSON snapshot

```text
cv_version

id
cv_id
version
created_at
snapshot JSONB
```

Пример:

```json
{
  "personal": {
    "firstName": "John",
    "lastName": "Smith"
  },
  "summary": "...",
  "skills": [
    {
      "name": "Java",
      "level": "Expert"
    }
  ],
  "experience": [
    {
      "company": "ABC",
      "position": "Senior Java Developer"
    }
  ]
}
```

### Рекомендуемый вариант

Использовать гибрид:

```text
Current CV → normalized relational model

CV Version → immutable JSONB snapshot
```

Пользователь редактирует нормальную модель, а при `Create Version` создаётся snapshot.

## 5. PDF

Архитектура:

```text
CV data
   ↓
Template
   ↓
HTML
   ↓
PDF
```

Возможные технологии:

- Playwright/Chromium;
- WeasyPrint;
- OpenHTMLtoPDF;
- Flying Saucer.

Для современного CV рекомендуется HTML/CSS → Chromium PDF.

## 6. Templates

Дизайн CV не должен быть жёстко зашит в backend.

Например:

```text
CV Template
 ├── Modern
 ├── Classic
 ├── Minimal
 └── Two-column
```

Данные:

```json
{
  "name": "John Smith",
  "skills": [],
  "experience": []
}
```

Templates:

```text
ModernTemplate
ClassicTemplate
MinimalTemplate
```

Один CV можно вывести в разных шаблонах без изменения данных.

## 7. Export

Можно сделать отдельную сущность:

```text
Export
```

Например:

```text
CV Version
     |
     +── PDF export
     +── JSON export
     +── DOCX export
```

Модель:

```text
export
------------------
id
cv_version_id
type
storage_type
storage_key
file_name
created_at
```

## 8. Local / Cloud storage

Нужно использовать abstraction:

```java
public interface FileStorage {

    StoredFile upload(
        String path,
        InputStream content
    );

    InputStream download(String path);

    void delete(String path);
}
```

Реализации:

```text
LocalFileStorage
S3FileStorage
AzureBlobStorage
GoogleCloudStorage
```

Local:

```text
C:\CV\exports\john-v3.pdf
```

S3:

```text
s3://my-cv/john/v3/cv.pdf
```

Приложение не должно зависеть от конкретного storage.

## 9. Vue application

Рекомендуемая структура:

```text
src/
├── app/
│
├── layouts/
│   ├── MainLayout.vue
│   └── EditorLayout.vue
│
├── modules/
│   ├── persons/
│   │   ├── pages/
│   │   │   ├── PersonList.vue
│   │   │   ├── PersonCreate.vue
│   │   │   └── PersonEdit.vue
│   │   ├── components/
│   │   └── api/
│   │
│   ├── cvs/
│   │   ├── pages/
│   │   │   ├── CvList.vue
│   │   │   ├── CvEditor.vue
│   │   │   └── CvVersions.vue
│   │   ├── components/
│   │   │   ├── CvHeader.vue
│   │   │   ├── ExperienceEditor.vue
│   │   │   ├── SkillsEditor.vue
│   │   │   └── SectionEditor.vue
│   │   └── api/
│   │
│   ├── templates/
│   │   ├── ModernTemplate.vue
│   │   ├── ClassicTemplate.vue
│   │   └── MinimalTemplate.vue
│   │
│   └── exports/
│
├── router/
├── stores/
└── shared/
```

## 10. UI

Основной интерфейс:

```text
┌──────────────────────────────────────────────────────────┐
│ CV Manager                                  John Smith ▼ │
├──────────────┬───────────────────────────────────────────┤
│              │                                           │
│ Persons      │  Java Backend Developer                   │
│              │                                           │
│ ● John       │  Version: v3                              │
│   Jane       │                                           │
│   Peter      │  [Edit] [Versions] [Preview] [Export PDF]│
│              │                                           │
│              ├───────────────────────────────────────────┤
│ CVs          │                                           │
│              │  Summary                                  │
│ + Create CV  │  ─────────────────────────────             │
│              │  Senior Java developer...                 │
│ ● Java       │                                           │
│   Backend    │  Experience                               │
│              │  ─────────────────────────────             │
│   Full Stack │  Company A                                │
│              │  Senior Java Developer                    │
│              │                                           │
│              │  Skills                                   │
│              │  Java  Spring  Kafka  PostgreSQL          │
└──────────────┴───────────────────────────────────────────┘
```

## 11. Live preview

Редактор может работать в двух колонках:

```text
┌─────────────────┬─────────────────────┐
│ Editor          │ Preview             │
│                 │                     │
│ Summary         │ John Smith          │
│ [............]  │                     │
│                 │ Summary             │
│ Skills          │ Senior developer... │
│ [Java]          │                     │
│ [Spring]        │ Experience          │
│ [+ Add]         │ ABC                 │
│                 │ Senior Developer    │
└─────────────────┴─────────────────────┘
```

Поток:

```text
state
   ↓
Vue reactive model
   ↓
Template
   ↓
Live preview
```

## 12. Backend

Для Java/Spring Boot:

```text
Vue 3
   │
   │ REST
   ▼
Spring Boot
   │
   ├── CV service
   ├── Template service
   ├── Export service
   └── Storage service
   │
   ├── PostgreSQL
   └── S3 / Local filesystem
```

Для первой версии не стоит делать микросервисы.

Рекомендуется modular monolith:

```text
cv-manager
```

Структура:

```text
backend/
└── src/main/java/
    └── com.example.cv/
        ├── person/
        ├── cv/
        ├── template/
        ├── export/
        ├── storage/
        └── common/
```

Позже Export можно вынести в отдельный service, если это действительно понадобится.

## 13. REST API

### Persons

```http
GET    /api/persons
POST   /api/persons
GET    /api/persons/{id}
PUT    /api/persons/{id}
DELETE /api/persons/{id}
```

### CV

```http
GET    /api/persons/{personId}/cvs
POST   /api/persons/{personId}/cvs

GET    /api/cvs/{cvId}
PUT    /api/cvs/{cvId}
DELETE /api/cvs/{cvId}
```

### Versions

```http
GET  /api/cvs/{cvId}/versions
POST /api/cvs/{cvId}/versions

GET  /api/cv-versions/{versionId}
POST /api/cv-versions/{versionId}/restore
```

### Export

```http
POST /api/cv-versions/{id}/exports/pdf
GET  /api/exports/{id}
```

### Preview

```http
GET /api/cv-versions/{id}/preview
```

## 14. Дополнительные возможности

### Duplicate CV

```text
Java Developer
       ↓
Duplicate
       ↓
Java Developer — Banking
```

### Version branching

```text
v1
├── v2
│   └── v3
│
└── v2-banking
    └── v3-banking
```

Полноценный Git-подобный механизм не обязателен, но concept полезен.

### Tags

```text
Java
Backend
Banking
Senior
Remote
EU
```

Позволяют быстро находить нужные версии.

### Diff

```text
v2 → v3

+ Added Kafka
+ Added Elasticsearch
- Removed Angular
~ Changed summary
```

### Export filename

Например:

```text
John_Smith_Java_Backend_v3.pdf
```

## 15. MVP

### Phase 1 — Core

```text
Person
CV
CV sections
CRUD
PostgreSQL
Vue editor
```

### Phase 2 — Versions

```text
CV Version
Snapshots
Duplicate
Restore
Version history
```

### Phase 3 — Templates

```text
Template system
Live preview
2–3 templates
```

### Phase 4 — Export

```text
PDF
Local download
Cloud storage
```

### Phase 5 — Advanced

```text
DOCX
CV diff
Tags
Search
Import/export JSON
Cloud synchronization
Authentication
```

## 16. Итоговая архитектура

```text
                 ┌──────────────┐
                 │    Person    │
                 └──────┬───────┘
                        │
                  1..N  │
                        ▼
                 ┌──────────────┐
                 │      CV      │
                 └──────┬───────┘
                        │
                  1..N  │
                        ▼
                 ┌──────────────┐
                 │ CV Version   │
                 │  immutable   │
                 └──────┬───────┘
                        │
             ┌──────────┼──────────┐
             ▼          ▼          ▼
         Template     Preview     Export
                                    │
                              ┌─────┴─────┐
                              ▼           ▼
                           Local         S3
```

Ключевая модель:

```text
Person
  └── CV
       └── CV Version
            ├── Template
            ├── Preview
            └── Export
                 ├── Local storage
                 └── Cloud storage
```
