package com.example.cv.cv;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

@Component
public class CvHtmlRenderer {

    public String render(CvVersionSnapshot snapshot, String templateKey) {
        StringBuilder html = new StringBuilder("""
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"/><style>
                @page { size: A4; margin: 18mm 16mm; }
                body { font-family: sans-serif; color: #202a35; font-size: 10pt; line-height: 1.4; }
                h1 { font-size: 25pt; margin: 0; color: #183b56; }
                h2 { font-size: 13pt; color: #176b68; border-bottom: 1px solid #b9d2ce; padding-bottom: 3px; margin: 18px 0 8px; }
                h3 { font-size: 11pt; margin: 8px 0 2px; }
                p { margin: 2px 0 6px; }
                .subtitle, .contact, .muted { color: #56636e; }
                .entry { page-break-inside: avoid; margin-bottom: 8px; }
                .modern h2 { color: #176b68; }
                .classic h2 { color: #333; border-bottom-color: #888; }
                .minimal h1, .minimal h2 { color: #222; }
                </style></head><body class=\""""
                ).append(templateClass(templateKey)).append("\">");

        CvVersionSnapshot.PersonProfile person = snapshot.person();
        String fullName = person == null ? "" : join(person.firstName(), person.lastName());
        html.append("<header><h1>").append(escape(fullName.isBlank() ? snapshot.name() : fullName))
                .append("</h1>");
        if (person != null && notBlank(person.position())) {
            html.append("<p class=\"subtitle\">").append(escape(person.position())).append("</p>");
        }
        if (person != null) {
            List<String> contactDetails = person.contacts() == null ? List.of() : person.contacts().stream()
                    .sorted(Comparator.comparingInt(CvVersionSnapshot.Contact::sortOrder))
                    .map(CvVersionSnapshot.Contact::value).filter(CvHtmlRenderer::notBlank).toList();
            if (notBlank(person.location())) {
                contactDetails = new java.util.ArrayList<>(contactDetails);
                contactDetails.add(0, person.location());
            }
            if (!contactDetails.isEmpty()) {
                html.append("<p class=\"contact\">").append(escape(String.join(" | ", contactDetails)))
                        .append("</p>");
            }
        }
        html.append("</header>");

        CvContent content = snapshot.content();
        if (content != null) {
                List<CvContent.Section> configuredSections = safe(content.sections());
                List<CvContent.Section> sections = configuredSections.stream()
                    .filter(CvContent.Section::visible)
                    .sorted(Comparator.comparingInt(CvContent.Section::sortOrder)).toList();
                if (configuredSections.isEmpty()) {
                appendSection(html, CvSectionType.SUMMARY, content);
                appendSection(html, CvSectionType.EXPERIENCE, content);
                appendSection(html, CvSectionType.EDUCATION, content);
                appendSection(html, CvSectionType.SKILLS, content);
                appendSection(html, CvSectionType.LANGUAGES, content);
                appendSection(html, CvSectionType.PROJECTS, content);
                appendSection(html, CvSectionType.CERTIFICATIONS, content);
                appendSection(html, CvSectionType.CUSTOM, content);
            } else {
                sections.forEach(section -> appendSection(html, section.type(), content));
            }
        }
        return html.append("</body></html>").toString();
    }

    private void appendSection(StringBuilder html, CvSectionType type, CvContent content) {
        switch (type) {
            case SUMMARY -> appendTextSection(html, "Profile", content.summary());
                case EXPERIENCE -> appendEntries(html, "Experience",
                    sorted(content.experiences(), CvContent.Experience::sortOrder), this::experienceHtml);
                case EDUCATION -> appendEntries(html, "Education",
                    sorted(content.education(), CvContent.Education::sortOrder), education ->
                    "<h3>" + escape(education.degree()) + " - " + escape(education.institution()) + "</h3>"
                        + optionalLine(dateRange(education.startDate(), education.endDate(), education.current()))
                        + optionalLine(education.fieldOfStudy()) + optionalLine(education.description()));
                case SKILLS -> appendEntries(html, "Skills",
                    sorted(content.skillGroups(), CvContent.SkillGroup::sortOrder), group ->
                    "<h3>" + escape(group.name()) + "</h3><p>" + escape(String.join(", ", safe(group.skills())
                            .stream().sorted(Comparator.comparingInt(CvContent.Skill::sortOrder))
                        .map(skill -> notBlank(skill.level())
                            ? skill.name() + " (" + skill.level() + ")" : skill.name()).toList())) + "</p>");
                case LANGUAGES -> appendEntries(html, "Languages",
                    sorted(content.languages(), CvContent.Language::sortOrder), language ->
                    "<p><strong>" + escape(language.language()) + "</strong>"
                        + (notBlank(language.level()) ? " - " + escape(language.level()) : "")
                        + optionalInline("Reading", language.reading())
                        + optionalInline("Writing", language.writing())
                        + optionalInline("Speaking", language.speaking()) + "</p>");
                case PROJECTS -> appendEntries(html, "Projects",
                    sorted(content.projects(), CvContent.Project::sortOrder), project ->
                    "<h3>" + escape(project.name()) + "</h3>" + optionalLine(project.role())
                            + optionalLine(project.description()) + optionalLine(project.technologies()));
                case CERTIFICATIONS -> appendEntries(html, "Certifications",
                    sorted(content.certifications(), CvContent.Certification::sortOrder), item ->
                    "<h3>" + escape(item.name()) + "</h3>" + optionalLine(item.issuer())
                            + optionalLine(item.description()));
                case CUSTOM -> appendEntries(html, "Additional Information",
                    sorted(content.customSections(), CvContent.CustomSection::sortOrder), item ->
                    "<h3>" + escape(item.title()) + "</h3>" + optionalLine(item.content()));
        }
    }

            private String experienceHtml(CvContent.Experience experience) {
            StringBuilder html = new StringBuilder("<h3>").append(escape(experience.position())).append(" - ")
                .append(escape(experience.company())).append("</h3>")
                .append(optionalLine(dateRange(experience.startDate(), experience.endDate(), experience.current())))
                .append(optionalLine(experience.location())).append(optionalLine(experience.description()));
            sorted(experience.projects(), CvContent.ExperienceProject::sortOrder).forEach(project -> {
                String projectName = Boolean.FALSE.equals(project.showProjectName()) ? "" : project.projectName();
                String customer = Boolean.FALSE.equals(project.showCustomerCompany()) ? "" : project.company();
                String heading = join(projectName, customer);
                html.append("<p><strong>").append(escape(heading)).append("</strong></p>")
                    .append(optionalLine(project.responsibilities())).append(optionalLine(project.technologies()));
            });
            return html.toString();
            }

    private void appendTextSection(StringBuilder html, String title, String text) {
        if (notBlank(text)) {
            html.append("<section><h2>").append(title).append("</h2><p>").append(escape(text))
                    .append("</p></section>");
        }
    }

    private <T> void appendEntries(StringBuilder html, String title, List<T> values,
            java.util.function.Function<T, String> formatter) {
        if (!values.isEmpty()) {
            html.append("<section><h2>").append(title).append("</h2>");
            values.forEach(value -> html.append("<div class=\"entry\">").append(formatter.apply(value))
                    .append("</div>"));
            html.append("</section>");
        }
    }

    private String optionalLine(String value) {
        return notBlank(value) ? "<p>" + escape(value) + "</p>" : "";
    }

    private String optionalInline(String label, String value) {
        return notBlank(value) ? "<br/>" + label + ": " + escape(value) : "";
    }

    private String dateRange(java.time.LocalDate start, java.time.LocalDate end, boolean current) {
        String from = start == null ? "" : start.toString();
        String to = current ? "Present" : end == null ? "" : end.toString();
        return join(from, to.isBlank() ? "" : " - " + to);
    }

    private <T> List<T> sorted(List<T> values, ToIntFunction<T> order) {
        return safe(values).stream().sorted(Comparator.comparingInt(order)).toList();
    }

    private String templateClass(String templateKey) {
        return switch (templateKey == null ? "modern" : templateKey) {
            case "classic", "minimal" -> templateKey;
            default -> "modern";
        };
    }

    private String join(String first, String last) {
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;").replace("\r\n", "\n")
                .replace("\r", "\n").replace("\n", "<br/>");
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}