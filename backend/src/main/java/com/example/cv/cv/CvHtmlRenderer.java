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
                body { font-family: "Noto Sans", sans-serif; color: #27362e; font-size: 10pt; line-height: 1.4; }
                h1 { font-size: 25pt; margin: 0; color: #20382e; }
                h2 { font-size: 12pt; color: #315d4b; border-bottom: 1px solid #dce4dd; padding-bottom: 3px; margin: 18px 0 8px; }
                h3 { font-size: 11pt; margin: 8px 0 2px; }
                p { margin: 2px 0 6px; }
                header { display: table; width: 100%; border-bottom: 3px solid #347463; padding-bottom: 14px; }
                .title, .contact { display: table-cell; vertical-align: bottom; }
                .title { width: 60%; }
                .contact { width: 40%; text-align: right; font-size: 9pt; }
                .subtitle, .contact, .muted { color: #53645a; }
                footer { margin-top: 20px; color: #88948c; font-size: 8pt; text-align: right; }
                a { color: #276d5a; }
                .entry { page-break-inside: avoid; margin-bottom: 8px; }
                .experience-heading { display: table; width: 100%; margin: 8px 0 2px; }
                .experience-heading h3, .experience-period { display: table-cell; vertical-align: baseline; }
                .experience-heading h3 { margin: 0; }
                .experience-period { text-align: right; color: #66756b; font-size: 9pt; padding-left: 12px; }
                .experience-meta { color: #66756b; font-size: 9pt; margin: 2px 0 6px; }
                .project-entry { margin: 8px 0 10px 10px; padding-left: 9px; border-left: 2px solid #dce6df; }
                .project-entry h3 { font-size: 10pt; margin: 0 0 3px; }
                .project-entry p { font-size: 9.5pt; margin: 3px 0; }
                .project-entry .project-meta { font-size: 9pt; color: #66756b; font-style: italic; }
                .classic { font-family: "Noto Serif", serif; color: #282828; }
                .classic header { display: block; text-align: center; border-bottom: 1px solid #696969; }
                .classic .title, .classic .contact { display: block; width: 100%; text-align: center; }
                .classic h1 { color: #222; }
                .classic .subtitle { color: #555; }
                .classic h2 { color: #333; border-bottom-color: #888; }
                .minimal { border-top: 5px solid #596f62; }
                .minimal header { border: 0; }
                .minimal h1 { color: #28352d; }
                .minimal h2 { color: #465d50; border: 0; }
                .minimal-body { width: 100%; border-collapse: collapse; table-layout: fixed; }
                .rail { width: 24%; vertical-align: top; padding-right: 12px; border-right: 1px solid #dce4de; color: #56675c; font-size: 9pt; }
                .sections { width: 76%; vertical-align: top; padding-left: 18px; }
                </style></head><body class=\""""
                ).append(templateClass(templateKey)).append("\">");

        CvVersionSnapshot.PersonProfile person = snapshot.person();
        boolean minimal = "minimal".equals(templateClass(templateKey));
        String fullName = person == null ? "" : join(person.firstName(), person.lastName());
        html.append("<header><div class=\"title\">");
        if (notBlank(fullName)) html.append("<h1>").append(escape(fullName)).append("</h1>");
        if (!minimal && person != null && notBlank(person.position())) {
            html.append("<p class=\"subtitle\">").append(escape(person.position())).append("</p>");
        }
        html.append("</div>");
        StringBuilder contacts = new StringBuilder();
        if (person != null && !Boolean.FALSE.equals(person.showContacts())) {
            List<String> contactDetails = person.contacts() == null ? List.of() : person.contacts().stream()
                    .sorted(Comparator.comparingInt(CvVersionSnapshot.Contact::sortOrder))
                    .filter(contact -> !Boolean.FALSE.equals(contact.showContact()))
                    .map(CvVersionSnapshot.Contact::value).filter(CvHtmlRenderer::notBlank).toList();
            if (notBlank(person.location())) {
                contactDetails = new java.util.ArrayList<>(contactDetails);
                contactDetails.add(0, person.location());
            }
            if (!contactDetails.isEmpty()) {
                contactDetails.forEach(value -> contacts.append(optionalLine(value)));
            }
        }
        if (!minimal && !contacts.isEmpty()) html.append("<div class=\"contact\">").append(contacts).append("</div>");
        html.append("</header>");
        if (minimal) {
            html.append("<table class=\"minimal-body\"><tbody><tr><td class=\"rail\">");
            if (person != null) html.append(optionalLine(person.position()));
            if (!contacts.isEmpty()) html.append("<h3>Contact</h3>").append(contacts);
            html.append("</td><td class=\"sections\">");
        }

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
        if (minimal) html.append("</td></tr></tbody></table>");
        html.append("<footer>").append(escape(snapshot.language() == null ? ""
            : snapshot.language().toUpperCase(java.util.Locale.ROOT))).append("</footer>");
        return html.append("</body></html>").toString();
    }

    private void appendSection(StringBuilder html, CvSectionType type, CvContent content) {
        switch (type) {
            case SUMMARY -> appendTextSection(html, "Profile", content.summary());
                case EXPERIENCE -> appendEntries(html, "Experience",
                    sorted(content.experiences(), CvContent.Experience::sortOrder),
                    experience -> experienceHtml(experience, content));
                case EDUCATION -> appendEntries(html, "Education",
                    sorted(content.education(), CvContent.Education::sortOrder), education ->
                    "<h3>" + escape(education.degree()) + " - " + escape(education.institution()) + "</h3>"
                        + optionalLine(dateRange(education.startDate(), education.endDate(), education.current()))
                        + optionalLine(education.fieldOfStudy()) + optionalLine(education.diplomaDegreeWork())
                        + optionalLine(education.description()));
                case SKILLS -> appendSkillGroups(html, content);
                case LANGUAGES -> appendEntries(html, "Languages",
                    sorted(content.languages(), CvContent.Language::sortOrder), language ->
                    "<p><strong>" + escape(language.language()) + "</strong>"
                        + (notBlank(language.level()) ? " - " + escape(language.level()) : "")
                        + optionalInline("Reading", language.reading())
                        + optionalInline("Writing", language.writing())
                        + optionalInline("Speaking", language.speaking()) + "</p>");
                case PROJECTS -> appendEntries(html, "Projects",
                    sorted(content.projects(), CvContent.Project::sortOrder), project ->
                    "<div class=\"project-entry\"><h3>" + escape(project.name()) + "</h3>" + projectMeta(project.role())
                            + optionalLine(project.description()) + optionalLine(project.technologies())
                        + optionalLink(project.url(), project.url()) + "</div>");
                case CERTIFICATIONS -> appendEntries(html, "Certifications",
                    sorted(content.certifications(), CvContent.Certification::sortOrder), item ->
                    "<h3>" + escape(item.name()) + "</h3>" + optionalLine(item.issuer())
                            + optionalLine(dateRange(item.issueDate(), item.expiryDate(), false))
                            + optionalLine(item.credentialId()) + optionalLine(item.description())
                            + optionalLink(item.credentialUrl(), "Credential"));
                case CUSTOM -> sorted(content.customSections(), CvContent.CustomSection::sortOrder).forEach(item -> {
                    if (notBlank(item.title()) || notBlank(item.content())) {
                        html.append("<section><h2>").append(escape(item.title())).append("</h2>")
                                .append(optionalLine(item.content())).append("</section>");
                    }
                });
        }
    }

    private void appendSkillGroups(StringBuilder html, CvContent content) {
        List<CvContent.SkillGroup> printableGroups = sorted(content.skillGroups(), CvContent.SkillGroup::sortOrder).stream()
                .map(group -> new CvContent.SkillGroup(group.name(), group.sortOrder(), safe(group.skills()).stream()
                        .filter(skill -> !Boolean.FALSE.equals(skill.visible()) && notBlank(skill.name()))
                        .sorted(Comparator.comparingInt(CvContent.Skill::sortOrder)).toList()))
                .filter(group -> !group.skills().isEmpty())
                .toList();
        appendEntries(html, "Skills", printableGroups, group ->
                "<h3>" + escape(group.name()) + "</h3><p>" + escape(String.join(", ", group.skills().stream()
                        .map(skill -> content.includeSkillLevelsInOutput() && notBlank(skill.level())
                                ? skill.name() + " (" + skill.level() + ")" : skill.name()).toList())) + "</p>"
                        + group.skills().stream().map(skill -> skillDetailsHtml(skill, content))
                                .collect(java.util.stream.Collectors.joining()));
    }

    private String skillDetailsHtml(CvContent.Skill skill, CvContent content) {
        return skillDetailLines(skill, content).stream().map(this::optionalLine)
            .collect(java.util.stream.Collectors.joining());
        }

        List<String> skillDetailLines(CvContent.Skill skill, CvContent content) {
        CvSkillDetails details = skill.details();
        if (details == null || !content.includeSkillDetailsInOutput()) {
            return List.of();
        }
        var calculated = details.calculate(content.skillProjectPeriods(), java.time.LocalDate.now());
        List<String> values = new java.util.ArrayList<>();
        if (calculated.yearsOfExperience() != null) values.add("Years of experience: " + calculated.yearsOfExperience());
        if (calculated.totalExperience() != null) values.add("Total experience: " + calculated.totalExperience() + " years");
        if (details.yearsActivelyUsed() != null) values.add("Actively used: " + details.yearsActivelyUsed() + " years");
        if (notBlank(details.startedFrom())) values.add("Started from: " + details.startedFrom());
        if (calculated.lastUsed() != null) values.add("Last used: " + calculated.lastUsed());
        if (notBlank(details.frequency())) values.add(details.frequency());
        if (notBlank(details.status())) values.add(details.status());
        if (calculated.stale()) values.add("Not used in over 5 years");
        List<String> lines = new java.util.ArrayList<>();
        if (!values.isEmpty()) lines.add(skill.name() + ": " + String.join(" | ", values));
        safe(details.linkedProjects()).forEach(link -> {
            String name = safe(content.projects()).stream().filter(project -> link.projectKey().equals(project.projectKey()))
                    .map(CvContent.Project::name).findFirst().orElse(null);
            if (name == null) {
                name = safe(content.experiences()).stream().flatMap(experience -> safe(experience.projects()).stream())
                        .filter(project -> link.projectKey().equals(project.projectKey()))
                        .map(project -> Boolean.FALSE.equals(project.showProjectName()) ? "Project" : project.projectName())
                        .findFirst().orElse(null);
            }
            if (name != null) lines.add(name + (notBlank(link.outcome()) ? ": " + link.outcome() : ""));
        });
        return lines;
    }

    private String experienceHtml(CvContent.Experience experience, CvContent content) {
            StringBuilder html = new StringBuilder("<div class=\"experience-heading\"><h3>")
                .append(escape(notBlank(experience.position()) ? experience.position() : experience.company()))
                .append("</h3><span class=\"experience-period\">")
                .append(escape(dateRange(experience.startDate(), experience.endDate(), experience.current())))
                .append("</span></div>");
            String metadata = compact(experience.company(), experience.location(), experience.employmentType(), experience.employmentLocation());
            if (notBlank(metadata)) html.append("<p class=\"experience-meta\">").append(escape(metadata)).append("</p>");
            html
                .append(optionalLine(experience.description()));
            sorted(experience.projects(), CvContent.ExperienceProject::sortOrder).forEach(project -> {
                String projectName = Boolean.FALSE.equals(project.showProjectName()) ? "" : project.projectName();
                String customer = Boolean.FALSE.equals(project.showCustomerCompany()) ? "" : project.company();
                html.append("<div class=\"project-entry\">");
                if (notBlank(projectName)) html.append("<h3>").append(escape(projectName)).append("</h3>");
                html.append(projectMeta(compact(project.position(), customer)))
                    .append(optionalLine(project.projectDescription()))
                    .append(optionalLine(notBlank(project.responsibilities()) ? "Responsibilities: " + project.responsibilities() : null))
                    .append(projectMeta(linkedSkills(project, content)))
                    .append(projectMeta(project.technologies())).append("</div>");
            });
            return html.toString();
            }

    private String projectMeta(String value) {
        return notBlank(value) ? "<p class=\"project-meta\">" + escape(value) + "</p>" : "";
    }

    private String compact(String... values) {
        return java.util.Arrays.stream(values).filter(CvHtmlRenderer::notBlank)
                .collect(java.util.stream.Collectors.joining(" | "));
    }

    String linkedSkills(CvContent.ExperienceProject project, CvContent content) {
        if (project.projectKey() == null) {
            return "";
        }
        List<String> names = safe(content.skillGroups()).stream()
                .flatMap(group -> safe(group.skills()).stream())
                .filter(skill -> !Boolean.FALSE.equals(skill.visible()) && notBlank(skill.name()))
                .filter(skill -> skill.details() != null && safe(skill.details().linkedProjects()).stream()
                        .anyMatch(link -> project.projectKey().equals(link.projectKey())))
                .map(CvContent.Skill::name).distinct().toList();
        return names.isEmpty() ? "" : "Used: " + String.join(", ", names);
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

    private String optionalLink(String url, String label) {
        if (!notBlank(url)) return "";
        try {
            java.net.URI uri = java.net.URI.create(url);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) return "";
            if (uri.getHost() == null) return "";
            return "<p><a href=\"" + escape(url) + "\">" + escape(label) + "</a></p>";
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String optionalLine(String value) {
        return notBlank(value) ? "<p>" + escape(value) + "</p>" : "";
    }

    private String optionalInline(String label, String value) {
        return notBlank(value) ? "<br/>" + label + ": " + escape(value) : "";
    }

    private String dateRange(java.time.LocalDate start, java.time.LocalDate end, boolean current) {
        var format = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale.ENGLISH);
        String from = start == null ? "" : start.format(format);
        String to = current ? "Present" : end == null ? "" : end.format(format);
        return from.isBlank() ? to : to.isBlank() ? from : from + " - " + to;
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