package com.example.cv.cv;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocxRendererTest {

    private final DocxRenderer renderer = new DocxRenderer();

    @Test
    void omitsProfilePhotoFromDocumentWhenSnapshotContainsOne() throws Exception {
        CvVersionSnapshot snapshot = new CvVersionSnapshot(null, "Backend CV", null, "en", CvStatus.ACTIVE,
                new CvContent(null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()),
                new CvVersionSnapshot.PersonProfile("Jane", "Doe", "Engineer", "Riga", List.of(), "persons/jane/photo.png"));

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(renderer.render(snapshot)))) {
            assertThat(document.getAllPictures()).isEmpty();
        }
        }

    @Test
    void hidesSkillLevelsWhenDisabledGlobally() throws Exception {
        CvContent content = new CvContent(null, List.of(), List.of(), List.of(new CvContent.SkillGroup("Backend", 0,
                List.of(new CvContent.Skill("Java", "Advanced", 0, true)))), List.of(), List.of(), List.of(), List.of(),
                List.of(new CvContent.Section(CvSectionType.SKILLS, true, 0)), false, false);
        CvVersionSnapshot snapshot = new CvVersionSnapshot(null, "Backend CV", null, "en", CvStatus.ACTIVE, content,
                new CvVersionSnapshot.PersonProfile("Jane", "Doe", "Engineer", "Riga", List.of()));

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(renderer.render(snapshot)))) {
            String output = String.join(" ", document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText()).toList());

            assertThat(output).contains("Java").doesNotContain("Advanced");
        }
    }

    @Test
    void rendersVisibleSectionsInConfiguredOrder() throws Exception {
        CvContent.Experience experience = new CvContent.Experience("Example Corp", "Developer", "Riga", null, null,
                LocalDate.of(2020, 1, 1), null, true, "Built services", 0, List.of());
        CvContent content = new CvContent("Profile summary", List.of(experience), List.of(),
                List.of(new CvContent.SkillGroup("Skills", 0,
                        List.of(new CvContent.Skill("Java", null, 0, true)))), List.of(), List.of(), List.of(), List.of(),
                List.of(new CvContent.Section(CvSectionType.EXPERIENCE, true, 0),
                        new CvContent.Section(CvSectionType.SUMMARY, true, 1),
                        new CvContent.Section(CvSectionType.SKILLS, false, 2)));
        CvVersionSnapshot snapshot = new CvVersionSnapshot(null, "Backend CV", null, "en", CvStatus.ACTIVE, content,
                new CvVersionSnapshot.PersonProfile("Jane", "Doe", "Engineer", "Riga", List.of()));

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(renderer.render(snapshot)))) {
            List<String> paragraphs = document.getParagraphs().stream().map(paragraph -> paragraph.getText()).toList();
            String output = String.join(" ", paragraphs);

            assertThat(output).contains("Jane Doe", "Engineer", "Experience", "Example Corp", "Profile",
                    "Profile summary");
            assertThat(output).doesNotContain("Skills", "Java");
            assertThat(paragraphs.indexOf("Experience")).isLessThan(paragraphs.indexOf("Profile"));
        }
    }
}