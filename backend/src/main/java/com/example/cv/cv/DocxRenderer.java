package com.example.cv.cv;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Component
public class DocxRenderer {

    public byte[] render(CvVersionSnapshot snapshot) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CvVersionSnapshot.PersonProfile person = snapshot.person();
            String fullName = person == null ? "" : join(person.firstName(), person.lastName());
            addText(document, fullName.isBlank() ? snapshot.name() : fullName, 24, true, "183B56");
            if (person != null && !Boolean.FALSE.equals(person.showContacts())) {
                addText(document, person.position(), 12, false, "56636E");
                List<String> contacts = safe(person.contacts()).stream()
                        .sorted(Comparator.comparingInt(CvVersionSnapshot.Contact::sortOrder))
                    .filter(contact -> !Boolean.FALSE.equals(contact.showContact()))
                        .map(CvVersionSnapshot.Contact::value).filter(DocxRenderer::notBlank).toList();
                if (notBlank(person.location())) {
                    contacts = new java.util.ArrayList<>(contacts);
                    contacts.add(0, person.location());
                }
                addText(document, String.join(" | ", contacts), 10, false, "56636E");
            }

            CvContent content = snapshot.content();
            if (content != null) {
                List<CvContent.Section> configured = safe(content.sections());
                List<CvContent.Section> sections = configured.stream().filter(CvContent.Section::visible)
                        .sorted(Comparator.comparingInt(CvContent.Section::sortOrder)).toList();
                if (configured.isEmpty()) {
                    for (CvSectionType type : CvSectionType.values()) {
                        appendSection(document, type, content);
                    }
                } else {
                    sections.forEach(section -> appendSection(document, section.type(), content));
                }
            }
            document.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not render CV DOCX", exception);
        }
    }

    private void appendSection(XWPFDocument document, CvSectionType type, CvContent content) {
        switch (type) {
            case SUMMARY -> appendTextSection(document, "Profile", content.summary());
            case EXPERIENCE -> {
                if (!safe(content.experiences()).isEmpty()) {
                    addHeading(document, "Experience");
                    sorted(content.experiences(), CvContent.Experience::sortOrder).forEach(item -> {
                        addText(document, join(item.position(), item.company()), 12, true, "202A35");
                        addText(document, dateRange(item.startDate(), item.endDate(), item.current()), 10, false, "56636E");
                        addText(document, item.location(), 10, false, "56636E");
                        addText(document, item.description(), 10, false, "202A35");
                        sorted(item.projects(), CvContent.ExperienceProject::sortOrder).forEach(project -> {
                            String name = Boolean.FALSE.equals(project.showProjectName()) ? "" : project.projectName();
                            String company = Boolean.FALSE.equals(project.showCustomerCompany()) ? "" : project.company();
                            addText(document, join(name, company), 11, true, "202A35");
                            addText(document, project.responsibilities(), 10, false, "202A35");
                            addText(document, project.technologies(), 10, false, "202A35");
                        });
                    });
                }
            }
            case EDUCATION -> {
                if (!safe(content.education()).isEmpty()) {
                    addHeading(document, "Education");
                    sorted(content.education(), CvContent.Education::sortOrder).forEach(item -> {
                        addText(document, join(item.degree(), item.institution()), 12, true, "202A35");
                        addText(document, dateRange(item.startDate(), item.endDate(), item.current()), 10, false, "56636E");
                        addText(document, item.fieldOfStudy(), 10, false, "202A35");
                        addText(document, item.diplomaDegreeWork(), 10, false, "202A35");
                        addText(document, item.description(), 10, false, "202A35");
                    });
                }
            }
            case SKILLS -> {
                List<CvContent.SkillGroup> groups = sorted(content.skillGroups(), CvContent.SkillGroup::sortOrder).stream()
                        .map(group -> new CvContent.SkillGroup(group.name(), group.sortOrder(), safe(group.skills()).stream()
                                .filter(skill -> !Boolean.FALSE.equals(skill.visible()) && notBlank(skill.name()))
                                .sorted(Comparator.comparingInt(CvContent.Skill::sortOrder)).toList()))
                        .filter(group -> !group.skills().isEmpty()).toList();
                if (!groups.isEmpty()) {
                    addHeading(document, "Skills");
                    groups.forEach(group -> addText(document, group.name() + ": " + String.join(", ", group.skills().stream()
                                .map(skill -> content.includeSkillLevelsInOutput() && notBlank(skill.level())
                                    ? skill.name() + " (" + skill.level() + ")" : skill.name())
                            .toList()), 10, false, "202A35"));
                }
            }
            case LANGUAGES -> {
                if (!safe(content.languages()).isEmpty()) {
                    addHeading(document, "Languages");
                    sorted(content.languages(), CvContent.Language::sortOrder).forEach(item -> addText(document,
                            join(item.language(), item.level()), 10, false, "202A35"));
                }
            }
            case PROJECTS -> {
                if (!safe(content.projects()).isEmpty()) {
                    addHeading(document, "Projects");
                    sorted(content.projects(), CvContent.Project::sortOrder).forEach(item -> {
                        addText(document, item.name(), 12, true, "202A35");
                        addText(document, item.role(), 10, false, "56636E");
                        addText(document, item.description(), 10, false, "202A35");
                        addText(document, item.technologies(), 10, false, "202A35");
                        addText(document, item.url(), 10, false, "202A35");
                    });
                }
            }
            case CERTIFICATIONS -> {
                if (!safe(content.certifications()).isEmpty()) {
                    addHeading(document, "Certifications");
                    sorted(content.certifications(), CvContent.Certification::sortOrder).forEach(item -> {
                        addText(document, item.name(), 12, true, "202A35");
                        addText(document, item.issuer(), 10, false, "56636E");
                        addText(document, item.description(), 10, false, "202A35");
                    });
                }
            }
            case CUSTOM -> {
                if (!safe(content.customSections()).isEmpty()) {
                    addHeading(document, "Additional Information");
                    sorted(content.customSections(), CvContent.CustomSection::sortOrder).forEach(item -> {
                        addText(document, item.title(), 12, true, "202A35");
                        addText(document, item.content(), 10, false, "202A35");
                    });
                }
            }
        }
    }

    private void appendTextSection(XWPFDocument document, String title, String text) {
        if (notBlank(text)) {
            addHeading(document, title);
            addText(document, text, 10, false, "202A35");
        }
    }

    private void addHeading(XWPFDocument document, String text) {
        addText(document, text, 14, true, "176B68");
    }

    private void addText(XWPFDocument document, String text, int size, boolean bold, String color) {
        if (!notBlank(text)) {
            return;
        }
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(100);
        if (size == 24) {
            paragraph.setAlignment(ParagraphAlignment.CENTER);
        }
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("Aptos");
        run.setFontSize(size);
        run.setBold(bold);
        run.setColor(color);
        run.setText(text);
    }

    private String dateRange(LocalDate start, LocalDate end, boolean current) {
        String from = start == null ? "" : start.toString();
        String to = current ? "Present" : end == null ? "" : end.toString();
        return join(from, to.isBlank() ? "" : " - " + to);
    }

    private <T> List<T> sorted(List<T> values, java.util.function.ToIntFunction<T> order) {
        return safe(values).stream().sorted(Comparator.comparingInt(order)).toList();
    }

    private String join(String first, String second) {
        return ((first == null ? "" : first) + " " + (second == null ? "" : second)).trim();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}