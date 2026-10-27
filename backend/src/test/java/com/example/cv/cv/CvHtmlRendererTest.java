package com.example.cv.cv;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CvHtmlRendererTest {

    private final CvHtmlRenderer renderer = new CvHtmlRenderer();

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"classic", "minimal", "modern"})
    void exportsTheSamePrintableFieldsInPdfAndDocx(String templateKey) throws Exception {
        var project = new CvContent.ExperienceProject("Hidden customer", null, "Hidden project", "Nested description",
                false, false, null, null, "Project lead", "Built services", "Spring", null, null, 0, "nested");
        var experience = new CvContent.Experience("Employer", "Engineer", "Riga office", "Contract", "Remote",
                java.time.LocalDate.of(2020, 1, 1), null, true, "Employment description", 0, List.of(project));
        var education = new CvContent.Education("University", "Masters", "Diploma work", "Computer science",
                null, null, false, "Education description", 0);
        var details = new CvSkillDetails(new java.math.BigDecimal("2"), null, "2016", "2024-01-01", "daily", "using",
                List.of(new CvSkillDetails.ProjectLink("nested", "Delivered outcome")), false);
        var content = new CvContent("Profile summary", List.of(experience), List.of(education),
                List.of(new CvContent.SkillGroup("Backend", 0, List.of(new CvContent.Skill("Java", "Advanced", 0, true, details)))),
                List.of(new CvContent.Language("English", "Fluent", "Excellent", "Good", "Conversational", 0)),
                List.of(new CvContent.Project("Portfolio", "Developer", "Standalone description", "Vue", "https://example.com/project", 0)),
                List.of(new CvContent.Certification("Certificate", "Certification description", "Issuer",
                        java.time.LocalDate.of(2023, 1, 1), java.time.LocalDate.of(2026, 1, 1), "Credential ID", "https://example.com/credential", 0)),
                List.of(new CvContent.CustomSection("Awards", "Award description", 0)), List.of(), true, true);
        var snapshot = new CvVersionSnapshot(null, "Backend CV", null, "en", CvStatus.DRAFT, content,
                new CvVersionSnapshot.PersonProfile("Jane", "Doe", "Engineer", "Riga",
                        List.of(new CvVersionSnapshot.Contact("EMAIL", "hidden@example.com", 0, false))));
        String html = renderer.render(snapshot, templateKey);
        assertThat(html).contains("<body class=\"" + templateKey + "\"");
        assertThat(html).contains("<div class=\"experience-heading\"><h3>Engineer</h3>",
                "<p class=\"experience-meta\">Employer | Riga office | Contract | Remote</p>",
                "<div class=\"project-entry\">", "<p class=\"project-meta\">Project lead</p>");
        byte[] pdf = new OpenHtmlToPdfRenderer().render(html);
        try (var pdfDocument = org.apache.pdfbox.pdmodel.PDDocument.load(pdf);
             var docx = new org.apache.poi.xwpf.usermodel.XWPFDocument(new java.io.ByteArrayInputStream(new DocxRenderer().render(snapshot, templateKey)));
             var extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(docx)) {
            String pdfText = new org.apache.pdfbox.text.PDFTextStripper().getText(pdfDocument);
            String docxText = extractor.getText();
            String[] expected = {"Jane Doe", "Riga", "Profile summary", "Contract", "Remote",
                    "Nested description", "Project lead", "Responsibilities: Built services", "Used: Java", "Diploma work",
                    "Years of experience: 2", "Total experience:", "Delivered outcome", "Reading: Excellent", "Writing: Good", "Speaking: Conversational",
                    "Standalone description", "https://example.com/project", "Credential ID", "Jan 2023 - Jan 2026", "Awards", "Award description"};
            assertThat(pdfText).contains(expected).doesNotContain("Backend CV", "BACKEND CV", "Hidden customer", "Hidden project", "hidden@example.com");
            assertThat(docxText).contains(expected).doesNotContain("Backend CV", "BACKEND CV", "Hidden customer", "Hidden project", "hidden@example.com");
            assertThat(pdfText).doesNotContain("Skills: Java");
            assertThat(docxText).doesNotContain("Skills: Java");
            assertThat(pdfText.indexOf("Used: Java")).isLessThan(pdfText.indexOf("Spring"));
            assertThat(docxText.indexOf("Used: Java")).isLessThan(docxText.indexOf("Spring"));
            assertThat(docx.getStyles().styleExist(templateKey)).isTrue();
            assertThat(docx.getTables()).hasSize("minimal".equals(templateKey) ? 1 : 0);
            List<org.apache.poi.xwpf.usermodel.XWPFParagraph> paragraphs = "minimal".equals(templateKey)
                    ? docx.getTables().get(0).getRow(0).getCell(1).getParagraphs() : docx.getParagraphs();
            assertThat(paragraphs).extracting(org.apache.poi.xwpf.usermodel.XWPFParagraph::getText)
                    .contains("Engineer   |   Jan 2020 - Present", "Employer | Riga office | Contract | Remote")
                    .doesNotContain("Contract", "Remote", "Riga office");
            var projectDescription = paragraphs.stream().filter(paragraph -> "Nested description".equals(paragraph.getText())).findFirst().orElseThrow();
            assertThat(projectDescription.getIndentationLeft()).isEqualTo(240);
            assertThat(projectDescription.getBorderLeft()).isEqualTo(org.apache.poi.xwpf.usermodel.Borders.SINGLE);
            var projectRole = paragraphs.stream().filter(paragraph -> "Project lead".equals(paragraph.getText())).findFirst().orElseThrow();
            assertThat(projectRole.getRuns().get(0).isItalic()).isTrue();
            assertThat(projectRole.getRuns().get(0).getFontSizeAsDouble()).isEqualTo(9.0);
        }
    }

    @Test
    void rendersOptInCalculatedMetadataAndEscapesProjectOutcomes() {
        var details = new CvSkillDetails(new java.math.BigDecimal("2"), new java.math.BigDecimal("1"),
                "2016", "2010", "occasionally", "maintaining",
                List.of(new CvSkillDetails.ProjectLink("project", "Shipped <fast> & safely")), false);
        var content = new CvContent(null, List.of(), List.of(), List.of(new CvContent.SkillGroup("Backend", 0,
                List.of(new CvContent.Skill("Java", "Advanced", 0, true, details)))), List.of(),
                List.of(new CvContent.Project("Platform", null, null, null, null, 0, "project",
                        java.time.LocalDate.of(2020, 1, 1), java.time.LocalDate.of(2024, 1, 1), false)),
                List.of(), List.of(), List.of(new CvContent.Section(CvSectionType.SKILLS, true, 0)), true, true);
        assertThat(renderer.render(snapshot(content), "modern"))
                .contains("Total experience: 4 years", "Last used: 2024-01-01", "Actively used: 1 years",
                        "occasionally", "maintaining", "Platform: Shipped &lt;fast&gt; &amp; safely")
                .doesNotContain("<fast>");
    }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(strings = {"classic", "minimal", "modern"})
        void doesNotUseCvNameWhenPersonIsMissing(String templateKey) throws Exception {
                var snapshot = snapshot(content(null, List.of()));
                assertThat(renderer.render(snapshot, templateKey)).doesNotContain("Resume", "RESUME");
                try (var document = new org.apache.poi.xwpf.usermodel.XWPFDocument(
                                new java.io.ByteArrayInputStream(new DocxRenderer().render(snapshot, templateKey)));
                         var extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(document)) {
                        assertThat(extractor.getText()).doesNotContain("Resume", "RESUME");
                }
        }

    @Test
    void hidesSkillLevelsWhenDisabledGlobally() {
        CvContent content = new CvContent(null, List.of(), List.of(), List.of(new CvContent.SkillGroup("Backend", 0,
                List.of(new CvContent.Skill("Java", "Advanced", 0, true)))), List.of(), List.of(), List.of(), List.of(),
                List.of(new CvContent.Section(CvSectionType.SKILLS, true, 0)), false, false);

        assertThat(renderer.render(snapshot(content), "modern"))
                .contains("Java")
                .doesNotContain("Advanced");
    }

    @Test
    void escapesUserContentAndHonorsHiddenSections() {
        CvContent content = content("<script>private & unsafe</script>", List.of(
                new CvContent.Section(CvSectionType.SUMMARY, true, 0),
                new CvContent.Section(CvSectionType.EXPERIENCE, false, 1)));

        String html = renderer.render(snapshot(content), "modern");

        assertThat(html).contains("&lt;script&gt;private &amp; unsafe&lt;/script&gt;")
                .doesNotContain("<script>")
                .doesNotContain("Hidden employer");
    }

    @Test
    void doesNotRenderSectionsWhenEveryConfiguredSectionIsHidden() {
        CvContent content = content("Should not appear", List.of(
                new CvContent.Section(CvSectionType.SUMMARY, false, 0)));

        String html = renderer.render(snapshot(content), "minimal");

        assertThat(html).doesNotContain("Should not appear").doesNotContain("<h2>");
    }

    @Test
    void emitsDocumentTextAndOrdersVisibleSections() {
        CvContent content = content("A careful engineer", List.of(
                new CvContent.Section(CvSectionType.EXPERIENCE, true, 0),
                new CvContent.Section(CvSectionType.SUMMARY, true, 1)));
        CvVersionSnapshot withProfile = new CvVersionSnapshot(null, "Resume", null, "en", CvStatus.DRAFT,
                content, new CvVersionSnapshot.PersonProfile("Jane", "Doe", "Engineer", "Riga", List.of()));

        String html = renderer.render(withProfile, "classic");

        assertThat(html.indexOf("Experience")).isLessThan(html.indexOf("Profile"));
        assertThat(html).contains("Jane Doe", "Engineer", "Riga", "A careful engineer");
    }

    @Test
    void rendersVisibleSkillsLinkedToExperienceProjects() {
        var project = new CvContent.ExperienceProject(null, null, "Platform", "Project <description>", true, true,
                null, null, "Technical lead", "Built API", "Spring", null, null, 0, "platform-key");
        var experience = new CvContent.Experience("Company", "Engineer", null, null, null,
                null, null, false, null, 0, List.of(project));
        var visibleSkill = new CvContent.Skill("Java", null, 0, true,
                new CvSkillDetails(null, null, null, null, null, null,
                        List.of(new CvSkillDetails.ProjectLink("platform-key", null)), false));
        var hiddenSkill = new CvContent.Skill("Secret tool", null, 1, false,
                new CvSkillDetails(null, null, null, null, null, null,
                        List.of(new CvSkillDetails.ProjectLink("platform-key", null)), false));
        CvContent content = new CvContent(null, List.of(experience), List.of(),
                List.of(new CvContent.SkillGroup("Backend", 0, List.of(visibleSkill, hiddenSkill))),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new CvContent.Section(CvSectionType.EXPERIENCE, true, 0)));

        String html = renderer.render(snapshot(content), "modern");

        assertThat(html).contains("Spring", "Used: Java", "Responsibilities: Built API", "Project &lt;description&gt;", "Technical lead")
                .doesNotContain("Used: Java, Secret tool", "Skills: Java");
    }

    @Test
    void omitsProfilePhotoWhenSnapshotContainsOne() {
        CvVersionSnapshot.PersonProfile person = new CvVersionSnapshot.PersonProfile(
                "Jane", "Doe", "Engineer", "Riga", List.of(), "persons/jane/photo.png");
        CvVersionSnapshot snapshot = new CvVersionSnapshot(null, "Resume", null, "en", CvStatus.DRAFT,
                content(null, List.of()), person);

        assertThat(renderer.render(snapshot, "modern"))
                .doesNotContain("<img", "photo.png");
    }

    @Test
    void hidesContactsAndLocationWhenDisabledInSnapshot() {
        CvVersionSnapshot.PersonProfile person = new CvVersionSnapshot.PersonProfile(
                "Jane", "Doe", "Engineer", "Riga",
                List.of(new CvVersionSnapshot.Contact("EMAIL", "jane@example.com", 0)), null, false);
        CvVersionSnapshot snapshot = new CvVersionSnapshot(null, "Resume", null, "en", CvStatus.DRAFT,
                content(null, List.of()), person);

        assertThat(renderer.render(snapshot, "modern"))
                .contains("Jane Doe", "Engineer")
                .doesNotContain("Riga", "jane@example.com");
    }

    @Test
    void hidesOnlyContactsMarkedHidden() {
        CvVersionSnapshot.PersonProfile person = new CvVersionSnapshot.PersonProfile(
                "Jane", "Doe", "Engineer", "Riga", List.of(
                        new CvVersionSnapshot.Contact("EMAIL", "hidden@example.com", 0, false),
                        new CvVersionSnapshot.Contact("PHONE", "+371 20000000", 1, true)), null, true);
        CvVersionSnapshot snapshot = new CvVersionSnapshot(null, "Resume", null, "en", CvStatus.DRAFT,
                content(null, List.of()), person);

        assertThat(renderer.render(snapshot, "modern"))
                .contains("Riga", "+371 20000000")
                .doesNotContain("hidden@example.com");
    }

    @Test
    void omitsHiddenSkillsAndGroupsWithoutPrintableSkills() {
        CvContent content = new CvContent(null, List.of(), List.of(), List.of(
                new CvContent.SkillGroup("Backend", 0, List.of(
                        new CvContent.Skill("Java", "Advanced", 0, false),
                        new CvContent.Skill("Kotlin", "Intermediate", 1, null))),
                new CvContent.SkillGroup("Hidden group", 1, List.of(
                        new CvContent.Skill("Rust", null, 0, false)))),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new CvContent.Section(CvSectionType.SKILLS, true, 0)));

        String html = renderer.render(snapshot(content), "modern");

        assertThat(html).contains("Skills", "Backend", "Kotlin (Intermediate)")
                .doesNotContain("Java", "Hidden group", "Rust");
    }

    @Test
    void omitsSkillsSectionWhenEverySkillIsHidden() {
        CvContent content = new CvContent(null, List.of(), List.of(), List.of(
                new CvContent.SkillGroup("Backend", 0, List.of(
                        new CvContent.Skill("Java", null, 0, false)))),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new CvContent.Section(CvSectionType.SKILLS, true, 0)));

        String html = renderer.render(snapshot(content), "modern");

        assertThat(html).doesNotContain("Skills", "Backend", "Java");
    }

    private CvVersionSnapshot snapshot(CvContent content) {
        return new CvVersionSnapshot(null, "Resume", null, "en", CvStatus.DRAFT, content, null);
    }

    private CvContent content(String summary, List<CvContent.Section> sections) {
        CvContent.Experience experience = new CvContent.Experience("Hidden employer", "Role", null, null, null,
                null, null, false, null, 0, List.of());
        return new CvContent(summary, List.of(experience), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), sections);
    }
}