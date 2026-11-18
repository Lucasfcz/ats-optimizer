package com.atsoptimizer.modules.jobmatch.service;

import com.atsoptimizer.modules.jobmatch.domain.StructuredResumeTemplateData;
import com.atsoptimizer.modules.jobmatch.domain.TemplateExperienceEntry;
import com.atsoptimizer.modules.jobmatch.exception.PdfGenerationException;
import com.atsoptimizer.modules.resume.domain.EducationItem;
import com.atsoptimizer.modules.resume.domain.LanguageItem;
import com.atsoptimizer.modules.resume.domain.SkillCategory;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Deterministic 1-page ATS-friendly PDF generator replicating the official candidate template.
 * Uses progressive typographic density levels (0..3) and deterministic tail-pruning fallback
 * to guarantee a strict 1-page A4 output.
 */
@Service
public class ResumePdfGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(ResumePdfGeneratorService.class);

    private record LayoutProfile(
            int level,
            double bodyFontPt,
            double nameFontPt,
            double sectionHeadingPt,
            double marginMm,
            double lineHeight,
            double sectionMarginTopPt,
            double itemMarginBottomPt
    ) {
    }

    private static final List<LayoutProfile> LAYOUT_PROFILES = List.of(
            new LayoutProfile(0, 9.5, 18.0, 10.0, 12.0, 1.32, 8.0, 5.5),
            new LayoutProfile(1, 9.0, 17.0, 9.5, 10.0, 1.25, 6.0, 4.5),
            new LayoutProfile(2, 8.5, 16.0, 9.0, 9.0, 1.20, 5.0, 3.5),
            new LayoutProfile(3, 8.0, 15.0, 8.5, 8.0, 1.15, 4.0, 3.0)
    );

    /**
     * Checks whether the given structured resume fits within 1 A4 page across any supported
     * typographic layout profile (levels 0 to 3).
     */
    public boolean fitsInSinglePage(StructuredResumeTemplateData data) {
        if (data == null) {
            throw new PdfGenerationException("Cannot validate page count for null resume data");
        }
        return findFittingBytes(data) != null;
    }

    /**
     * Applies deterministic tail-pruning fallback only if even the tightest layout profile (Level 3)
     * exceeds 1 page. Assumes experiencesAndProjects are ordered by descending relevance.
     */
    public StructuredResumeTemplateData pruneToSinglePageIfNeeded(StructuredResumeTemplateData data) {
        if (fitsInSinglePage(data)) {
            return data;
        }

        log.warn("Resume content for [{}] still exceeds 1 page at tightest layout level; applying deterministic tail pruning",
                data.candidateName());

        List<TemplateExperienceEntry> mutableExperiences = new ArrayList<>(data.experiencesAndProjects());
        StructuredResumeTemplateData candidate = data;

        while (!fitsInSinglePage(candidate) && !mutableExperiences.isEmpty()) {
            int lastIdx = mutableExperiences.size() - 1;
            TemplateExperienceEntry lastEntry = mutableExperiences.get(lastIdx);
            if (lastEntry.bullets().size() > 1) {
                List<String> trimmedBullets = lastEntry.bullets().subList(0, lastEntry.bullets().size() - 1);
                mutableExperiences.set(lastIdx, lastEntry.withBullets(trimmedBullets));
            } else {
                mutableExperiences.remove(lastIdx);
            }
            candidate = candidate.withExperiencesAndProjects(mutableExperiences);
        }

        return candidate;
    }

    /**
     * Generates a 1-page A4 PDF binary matching the official template.
     */
    public byte[] generatePdf(StructuredResumeTemplateData data) {
        if (data == null) {
            throw new PdfGenerationException("Cannot generate PDF from null resume data");
        }

        byte[] fittingBytes = findFittingBytes(data);
        if (fittingBytes != null) {
            return fittingBytes;
        }

        StructuredResumeTemplateData pruned = pruneToSinglePageIfNeeded(data);
        byte[] prunedBytes = findFittingBytes(pruned);
        if (prunedBytes != null) {
            return prunedBytes;
        }

        return renderWithProfile(pruned, LAYOUT_PROFILES.getLast());
    }

    private byte[] findFittingBytes(StructuredResumeTemplateData data) {
        for (LayoutProfile profile : LAYOUT_PROFILES) {
            byte[] pdfBytes = renderWithProfile(data, profile);
            int pages = countPages(pdfBytes);
            if (pages == 1) {
                if (profile.level() > 0) {
                    log.info("Fitted resume [{}] into 1 page using compact layout level {}",
                            data.candidateName(), profile.level());
                }
                return pdfBytes;
            }
        }
        return null;
    }

    byte[] renderWithProfile(StructuredResumeTemplateData data, LayoutProfile profile) {
        String xhtml = buildXhtml(data, profile);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(xhtml, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to render ATS resume PDF", e);
        }
    }

    public int countPages(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new PdfGenerationException("Failed to inspect generated PDF page count", e);
        }
    }

    String buildXhtml(StructuredResumeTemplateData data, LayoutProfile profile) {
        StringBuilder html = new StringBuilder();
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n<meta charset=\"UTF-8\"/>\n<style>\n");
        html.append(buildCss(profile));
        html.append("</style>\n</head>\n<body>\n");

        appendHeader(html, data);
        appendTargetSummary(html, data);
        appendTechnicalSkills(html, data);
        appendExperiencesAndProjects(html, data);
        appendEducation(html, data);
        appendAchievements(html, data);
        appendLanguages(html, data);

        html.append("</body>\n</html>");
        return html.toString();
    }

    private String buildCss(LayoutProfile p) {
        return String.format(Locale.US, """
                @page {
                    size: A4;
                    margin: %.1fmm;
                }
                body {
                    font-family: Helvetica, Arial, sans-serif;
                    font-size: %.1fpt;
                    line-height: %.2f;
                    color: #111111;
                    margin: 0;
                    padding: 0;
                }
                .candidate-name {
                    font-size: %.1fpt;
                    font-weight: bold;
                    margin: 0 0 3pt 0;
                    padding-bottom: 4pt;
                    border-bottom: 1.5pt solid #111111;
                }
                .contact-block {
                    margin-bottom: %.1fpt;
                    color: #222222;
                }
                .contact-line {
                    margin: 0 0 1pt 0;
                }
                .section-title {
                    font-size: %.1fpt;
                    font-weight: bold;
                    text-transform: uppercase;
                    margin: %.1fpt 0 3pt 0;
                    padding-bottom: 1.5pt;
                    border-bottom: 0.75pt solid #222222;
                }
                .target-role {
                    font-size: %.1fpt;
                    font-weight: bold;
                    text-transform: uppercase;
                    margin: %.1fpt 0 2.5pt 0;
                }
                .summary-text {
                    margin: 0 0 2pt 0;
                }
                .skill-row {
                    margin: 0 0 1.5pt 0;
                }
                .entry-block {
                    margin-bottom: %.1fpt;
                }
                .entry-header {
                    width: 100%%;
                    border-collapse: collapse;
                }
                .entry-header td {
                    padding: 0;
                    vertical-align: baseline;
                }
                .entry-title {
                    font-weight: bold;
                }
                .entry-period {
                    text-align: right;
                    white-space: nowrap;
                    color: #222222;
                }
                .entry-subtitle-role {
                    font-weight: bold;
                    margin: 0.5pt 0 1.5pt 0;
                }
                .entry-subtitle-stack {
                    font-style: italic;
                    margin: 0.5pt 0 1.5pt 0;
                    color: #222222;
                }
                ul.bullets {
                    margin: 1pt 0 0 0;
                    padding-left: 13pt;
                }
                ul.bullets li {
                    margin: 0 0 1pt 0;
                }
                .edu-course {
                    margin: 0.5pt 0 2pt 0;
                }
                .languages-line {
                    margin: 0;
                }
                """,
                p.marginMm(),
                p.bodyFontPt(),
                p.lineHeight(),
                p.nameFontPt(),
                p.sectionMarginTopPt() * 0.7,
                p.sectionHeadingPt(),
                p.sectionMarginTopPt(),
                p.sectionHeadingPt(),
                p.sectionMarginTopPt() * 0.6,
                p.itemMarginBottomPt()
        );
    }

    private void appendHeader(StringBuilder html, StructuredResumeTemplateData data) {
        String name = nonBlank(data.candidateName(), "Candidato");
        html.append("<h1 class=\"candidate-name\">").append(escapeXml(name)).append("</h1>\n");

        List<String> line1 = joinNonBlank(data.location(), data.phone(), data.email());
        List<String> line2 = new ArrayList<>(joinNonBlank(data.linkedin()));
        if (data.otherLinks() != null) {
            data.otherLinks().stream()
                    .filter(s -> s != null && !s.isBlank())
                    .forEach(line2::add);
        }

        if (!line1.isEmpty() || !line2.isEmpty()) {
            html.append("<div class=\"contact-block\">\n");
            if (!line1.isEmpty()) {
                html.append("  <p class=\"contact-line\">")
                        .append(line1.stream().map(this::escapeXml).collect(Collectors.joining(" | ")))
                        .append("</p>\n");
            }
            if (!line2.isEmpty()) {
                html.append("  <p class=\"contact-line\">")
                        .append(line2.stream().map(this::escapeXml).collect(Collectors.joining(" | ")))
                        .append("</p>\n");
            }
            html.append("</div>\n");
        }
    }

    private void appendTargetSummary(StringBuilder html, StructuredResumeTemplateData data) {
        if (isNotBlank(data.targetRoleTitle())) {
            html.append("<div class=\"target-role\">")
                    .append(escapeXml(data.targetRoleTitle()))
                    .append("</div>\n");
        }
        if (isNotBlank(data.summary())) {
            html.append("<p class=\"summary-text\">")
                    .append(escapeXml(data.summary()))
                    .append("</p>\n");
        }
    }

    private void appendTechnicalSkills(StringBuilder html, StructuredResumeTemplateData data) {
        if (data.technicalSkills() == null || data.technicalSkills().isEmpty()) {
            return;
        }
        html.append("<div class=\"section-title\">HABILIDADES TÉCNICAS</div>\n");
        for (SkillCategory category : data.technicalSkills()) {
            if (category == null || category.skills().isEmpty()) {
                continue;
            }
            String skillsJoined = category.skills().stream()
                    .filter(Objects::nonNull)
                    .filter(s -> !s.isBlank())
                    .map(this::escapeXml)
                    .collect(Collectors.joining(", "));
            if (skillsJoined.isEmpty()) {
                continue;
            }
            html.append("<p class=\"skill-row\">");
            if (isNotBlank(category.categoryName())) {
                html.append("<strong>").append(escapeXml(category.categoryName())).append(":</strong> ");
            }
            html.append(skillsJoined).append("</p>\n");
        }
    }

    private void appendExperiencesAndProjects(StringBuilder html, StructuredResumeTemplateData data) {
        if (data.experiencesAndProjects() == null || data.experiencesAndProjects().isEmpty()) {
            return;
        }
        html.append("<div class=\"section-title\">EXPERIÊNCIA E PROJETOS</div>\n");
        for (TemplateExperienceEntry entry : data.experiencesAndProjects()) {
            if (entry == null) {
                continue;
            }
            html.append("<div class=\"entry-block\">\n");
            appendTwoColumnHeader(html, entry.title(), entry.period());

            if (isNotBlank(entry.subtitleLabel())) {
                if (entry.isStack()) {
                    String stackText = entry.subtitleLabel().trim();
                    if (!stackText.toLowerCase().startsWith("stack:")) {
                        stackText = "Stack: " + stackText;
                    }
                    html.append("  <div class=\"entry-subtitle-stack\">")
                            .append(escapeXml(stackText))
                            .append("</div>\n");
                } else {
                    html.append("  <div class=\"entry-subtitle-role\">")
                            .append(escapeXml(entry.subtitleLabel()))
                            .append("</div>\n");
                }
            }

            appendBullets(html, entry.bullets());
            html.append("</div>\n");
        }
    }

    private void appendEducation(StringBuilder html, StructuredResumeTemplateData data) {
        if (data.education() == null || data.education().isEmpty()) {
            return;
        }
        html.append("<div class=\"section-title\">FORMAÇÃO ACADÊMICA</div>\n");
        for (EducationItem edu : data.education()) {
            if (edu == null) {
                continue;
            }
            html.append("<div class=\"entry-block\">\n");
            appendTwoColumnHeader(html, edu.institution(), edu.period());
            List<String> courseParts = new ArrayList<>();
            if (isNotBlank(edu.degree()) && isNotBlank(edu.fieldOfStudy())) {
                courseParts.add(edu.degree() + " em " + edu.fieldOfStudy());
            } else {
                if (isNotBlank(edu.degree())) {
                    courseParts.add(edu.degree());
                }
                if (isNotBlank(edu.fieldOfStudy())) {
                    courseParts.add(edu.fieldOfStudy());
                }
            }
            if (isNotBlank(edu.status())) {
                courseParts.add("(" + edu.status() + ")");
            }
            if (!courseParts.isEmpty()) {
                html.append("  <p class=\"edu-course\">")
                        .append(escapeXml(String.join(" ", courseParts)))
                        .append("</p>\n");
            }
            html.append("</div>\n");
        }
    }

    private void appendAchievements(StringBuilder html, StructuredResumeTemplateData data) {
        if (data.achievements() == null || data.achievements().isEmpty()) {
            return;
        }
        List<String> validAchievements = data.achievements().stream()
                .filter(this::isNotBlank)
                .toList();
        if (validAchievements.isEmpty()) {
            return;
        }
        html.append("<div class=\"section-title\">CONQUISTAS</div>\n");
        appendBullets(html, validAchievements);
    }

    private void appendLanguages(StringBuilder html, StructuredResumeTemplateData data) {
        if (data.languages() == null || data.languages().isEmpty()) {
            return;
        }
        String joined = data.languages().stream()
                .filter(Objects::nonNull)
                .filter(l -> isNotBlank(l.language()))
                .map(l -> isNotBlank(l.proficiency())
                        ? escapeXml(l.language()) + ": " + escapeXml(l.proficiency())
                        : escapeXml(l.language()))
                .collect(Collectors.joining(" | "));
        if (joined.isEmpty()) {
            return;
        }
        html.append("<div class=\"section-title\">IDIOMAS</div>\n");
        html.append("<p class=\"languages-line\">").append(joined).append("</p>\n");
    }

    private void appendTwoColumnHeader(StringBuilder html, String leftText, String rightText) {
        html.append("  <table class=\"entry-header\"><tr>\n");
        html.append("    <td class=\"entry-title\">").append(escapeXml(nonBlank(leftText, ""))).append("</td>\n");
        html.append("    <td class=\"entry-period\">").append(escapeXml(nonBlank(rightText, ""))).append("</td>\n");
        html.append("  </tr></table>\n");
    }

    private void appendBullets(StringBuilder html, List<String> bullets) {
        if (bullets == null || bullets.isEmpty()) {
            return;
        }
        List<String> valid = bullets.stream().filter(this::isNotBlank).toList();
        if (valid.isEmpty()) {
            return;
        }
        html.append("  <ul class=\"bullets\">\n");
        for (String bullet : valid) {
            html.append("    <li>").append(escapeXml(bullet)).append("</li>\n");
        }
        html.append("  </ul>\n");
    }

    private List<String> joinNonBlank(String... values) {
        List<String> result = new ArrayList<>();
        for (String val : values) {
            if (isNotBlank(val)) {
                result.add(val.trim());
            }
        }
        return result;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String nonBlank(String value, String fallback) {
        return isNotBlank(value) ? value.trim() : fallback;
    }

    String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> {
                    if (c == '\t' || c == '\n' || c == '\r' || c >= 0x20) {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
