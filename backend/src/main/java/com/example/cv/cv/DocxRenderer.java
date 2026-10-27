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

    private final CvHtmlRenderer outputFormatter = new CvHtmlRenderer();

    public byte[] render(CvVersionSnapshot snapshot) {
        return render(snapshot, "modern");
    }

    public byte[] render(CvVersionSnapshot snapshot, String templateKey) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            String variant = "classic".equals(templateKey) || "minimal".equals(templateKey) ? templateKey : "modern";
            var style = org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle.Factory.newInstance();
            style.setStyleId(variant);
            document.createStyles().addStyle(new org.apache.poi.xwpf.usermodel.XWPFStyle(style));
            CvVersionSnapshot.PersonProfile person = snapshot.person();
            String fullName = person == null ? "" : join(person.firstName(), person.lastName());
            addText(document, fullName, 24, true, "183B56");
            org.apache.poi.xwpf.usermodel.IBody sectionBody = document;
            org.apache.poi.xwpf.usermodel.IBody contactBody = document;
            if ("minimal".equals(variant)) {
                var table = document.createTable(1, 2);
                table.setWidth("100%");
                table.setTopBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.NONE, 0, 0, "FFFFFF");
                table.setBottomBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.NONE, 0, 0, "FFFFFF");
                table.setLeftBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.NONE, 0, 0, "FFFFFF");
                table.setRightBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.NONE, 0, 0, "FFFFFF");
                table.setInsideHBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.NONE, 0, 0, "FFFFFF");
                table.setInsideVBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.SINGLE, 4, 0, "DCE4DE");
                var rail = table.getRow(0).getCell(0);
                rail.setWidth("24%");
                rail.removeParagraph(0);
                var sections = table.getRow(0).getCell(1);
                sections.setWidth("76%");
                sections.removeParagraph(0);
                contactBody = rail;
                sectionBody = sections;
            }
            if (person != null) addText(contactBody, person.position(), 12, false, "56636E");
            if (person != null && !Boolean.FALSE.equals(person.showContacts())) {
                List<String> contacts = safe(person.contacts()).stream()
                        .sorted(Comparator.comparingInt(CvVersionSnapshot.Contact::sortOrder))
                    .filter(contact -> !Boolean.FALSE.equals(contact.showContact()))
                        .map(CvVersionSnapshot.Contact::value).filter(DocxRenderer::notBlank).toList();
                if (notBlank(person.location())) {
                    contacts = new java.util.ArrayList<>(contacts);
                    contacts.add(0, person.location());
                }
                if ("minimal".equals(variant) && !contacts.isEmpty()) addHeading(contactBody, "Contact");
                addText(contactBody, String.join(" | ", contacts), 10, false, "56636E");
            }

            CvContent content = snapshot.content();
            if (content != null) {
                List<CvContent.Section> configured = safe(content.sections());
                List<CvContent.Section> sections = configured.stream().filter(CvContent.Section::visible)
                        .sorted(Comparator.comparingInt(CvContent.Section::sortOrder)).toList();
                if (configured.isEmpty()) {
                    for (CvSectionType type : CvSectionType.values()) {
                        appendSection(sectionBody, type, content);
                    }
                } else {
                    for (CvContent.Section section : sections) appendSection(sectionBody, section.type(), content);
                }
            }
                addText(document, snapshot.language() == null ? null : snapshot.language().toUpperCase(java.util.Locale.ROOT),
                    8, false, "88948C");
            document.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not render CV DOCX", exception);
        }
    }

    private void appendSection(org.apache.poi.xwpf.usermodel.IBody document, CvSectionType type, CvContent content) {
        switch (type) {
            case SUMMARY -> appendTextSection(document, "Profile", content.summary());
            case EXPERIENCE -> {
                if (!safe(content.experiences()).isEmpty()) {
                    addHeading(document, "Experience");
                    sorted(content.experiences(), CvContent.Experience::sortOrder).forEach(item -> {
                        XWPFParagraph heading = addText(document, notBlank(item.position()) ? item.position() : item.company(), 12, true, "202A35");
                        String period = dateRange(item.startDate(), item.endDate(), item.current());
                        if (heading != null && notBlank(period)) {
                            XWPFRun dates = heading.createRun();
                            dates.setFontFamily(heading.getRuns().get(0).getFontFamily());
                            dates.setFontSize(9);
                            dates.setColor("66756B");
                            dates.setText("   |   " + period);
                        } else if (heading == null) {
                            addText(document, period, 9, false, "56636E");
                        }
                        addText(document, compact(item.company(), item.location(), item.employmentType(), item.employmentLocation()), 9, false, "56636E");
                        addText(document, item.description(), 10, false, "202A35");
                        sorted(item.projects(), CvContent.ExperienceProject::sortOrder).forEach(project -> {
                            String name = Boolean.FALSE.equals(project.showProjectName()) ? "" : project.projectName();
                            String company = Boolean.FALSE.equals(project.showCustomerCompany()) ? "" : project.company();
                            addProjectText(document, name, true, false);
                            addProjectText(document, compact(project.position(), company), false, true);
                            addProjectText(document, project.projectDescription(), false, false);
                            addProjectText(document, notBlank(project.responsibilities()) ? "Responsibilities: " + project.responsibilities() : null, false, false);
                            addProjectText(document, outputFormatter.linkedSkills(project, content), false, true);
                            addProjectText(document, project.technologies(), false, true);
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
                    groups.forEach(group -> {
                        addText(document, group.name() + ": " + String.join(", ", group.skills().stream()
                                .map(skill -> content.includeSkillLevelsInOutput() && notBlank(skill.level())
                                    ? skill.name() + " (" + skill.level() + ")" : skill.name())
                                .toList()), 10, false, "202A35");
                            group.skills().forEach(skill -> outputFormatter.skillDetailLines(skill, content)
                                .forEach(line -> addText(document, line, 10, false, "56636E")));
                            });
                }
            }
            case LANGUAGES -> {
                if (!safe(content.languages()).isEmpty()) {
                    addHeading(document, "Languages");
                    sorted(content.languages(), CvContent.Language::sortOrder).forEach(item -> {
                        addText(document, join(item.language(), item.level()), 10, false, "202A35");
                        addText(document, notBlank(item.reading()) ? "Reading: " + item.reading() : null, 10, false, "56636E");
                        addText(document, notBlank(item.writing()) ? "Writing: " + item.writing() : null, 10, false, "56636E");
                        addText(document, notBlank(item.speaking()) ? "Speaking: " + item.speaking() : null, 10, false, "56636E");
                    });
                }
            }
            case PROJECTS -> {
                if (!safe(content.projects()).isEmpty()) {
                    addHeading(document, "Projects");
                    sorted(content.projects(), CvContent.Project::sortOrder).forEach(item -> {
                        addProjectText(document, item.name(), true, false);
                        addProjectText(document, item.role(), false, true);
                        addProjectText(document, item.description(), false, false);
                        addProjectText(document, item.technologies(), false, true);
                        addProjectText(document, safeUrl(item.url()), false, true);
                    });
                }
            }
            case CERTIFICATIONS -> {
                if (!safe(content.certifications()).isEmpty()) {
                    addHeading(document, "Certifications");
                    sorted(content.certifications(), CvContent.Certification::sortOrder).forEach(item -> {
                        addText(document, item.name(), 12, true, "202A35");
                        addText(document, item.issuer(), 10, false, "56636E");
                        addText(document, dateRange(item.issueDate(), item.expiryDate(), false), 10, false, "56636E");
                        addText(document, item.credentialId(), 10, false, "56636E");
                        addText(document, item.description(), 10, false, "202A35");
                        addText(document, safeUrl(item.credentialUrl()), 10, false, "202A35");
                    });
                }
            }
            case CUSTOM -> {
                if (!safe(content.customSections()).isEmpty()) {
                    sorted(content.customSections(), CvContent.CustomSection::sortOrder).forEach(item -> {
                        addHeading(document, item.title());
                        addText(document, item.content(), 10, false, "202A35");
                    });
                }
            }
        }
    }

    private void appendTextSection(org.apache.poi.xwpf.usermodel.IBody document, String title, String text) {
        if (notBlank(text)) {
            addHeading(document, title);
            addText(document, text, 10, false, "202A35");
        }
    }

    private void addHeading(org.apache.poi.xwpf.usermodel.IBody document, String text) {
        addText(document, text, 14, true, "176B68");
    }

    private void addProjectText(org.apache.poi.xwpf.usermodel.IBody body, String text, boolean title, boolean metadata) {
        XWPFParagraph paragraph = addText(body, text, title ? 11 : metadata ? 9 : 10, title, metadata ? "56636E" : "202A35");
        if (paragraph == null) return;
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setIndentationLeft(240);
        paragraph.setSpacingBefore(title ? 100 : 0);
        paragraph.setSpacingAfter(40);
        paragraph.setBorderLeft(org.apache.poi.xwpf.usermodel.Borders.SINGLE);
        var border = paragraph.getCTP().getPPr().getPBdr().getLeft();
        border.setColor("DCE6DF");
        border.setSz(java.math.BigInteger.valueOf(8));
        border.setSpace(java.math.BigInteger.valueOf(6));
        paragraph.getRuns().forEach(run -> run.setItalic(metadata));
    }

    private String compact(String... values) {
        return java.util.Arrays.stream(values).filter(DocxRenderer::notBlank)
                .collect(java.util.stream.Collectors.joining(" | "));
    }

    private XWPFParagraph addText(org.apache.poi.xwpf.usermodel.IBody body, String text, int size, boolean bold, String color) {
        if (!notBlank(text)) {
            return null;
        }
        XWPFDocument document = body.getXWPFDocument();
        XWPFParagraph paragraph = body instanceof org.apache.poi.xwpf.usermodel.XWPFTableCell cell
            ? cell.addParagraph() : document.createParagraph();
        paragraph.setSpacingAfter(100);
        boolean classic = document.getStyles().styleExist("classic");
        boolean minimal = document.getStyles().styleExist("minimal");
        if (classic && (size == 24 || size == 9 || size == 12 && !bold || "56636E".equals(color) && document.getParagraphs().size() <= 4)) {
            paragraph.setAlignment(ParagraphAlignment.CENTER);
        }
        XWPFRun run = paragraph.createRun();
        run.setFontFamily(classic ? "Times New Roman" : "Arial");
        run.setFontSize(size);
        run.setBold(bold);
        run.setColor(classic ? (bold ? "282828" : "555555")
                : minimal ? (bold ? "465D50" : "35443B")
                : "183B56".equals(color) ? "20382E" : "176B68".equals(color) ? "315D4B" : color);
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) run.addBreak();
            run.setText(lines[index]);
        }
        return paragraph;
    }

    private String safeUrl(String value) {
        if (!notBlank(value)) return null;
        try {
            var uri = java.net.URI.create(value);
            return uri.getHost() != null && ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme())) ? value : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String dateRange(LocalDate start, LocalDate end, boolean current) {
        var format = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale.ENGLISH);
        String from = start == null ? "" : start.format(format);
        String to = current ? "Present" : end == null ? "" : end.format(format);
        return from.isBlank() ? to : to.isBlank() ? from : from + " - " + to;
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