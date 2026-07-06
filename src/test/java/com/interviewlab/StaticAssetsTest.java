package com.interviewlab;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No JS test framework exists in this project (plain JSX served via Babel standalone,
 * no build/test pipeline) — these are structural, text-level checks on the static
 * resources themselves, run as plain Java tests against the classpath copies.
 */
class StaticAssetsTest {

    // -------------------------------------------------------------------------
    // G3 — Monaco loader must NOT be present in the initial HTML (lazy-loaded by
    // CodeEditor.jsx instead, so users who never open Code Challenge never download it)
    // -------------------------------------------------------------------------

    @Test
    void indexHtml_doesNotEagerlyLoadMonaco() throws IOException {
        String html = readClasspathResource("static/index.html");

        assertThat(html).doesNotContain("monaco-editor@0.44.0/min/vs/loader.js");
    }

    @Test
    void codeEditorJsx_loadsMonacoLazilyOnFirstMount() throws IOException {
        String jsx = readClasspathResource("static/CodeEditor.jsx");

        assertThat(jsx).contains("loadMonacoLoader");
        assertThat(jsx).contains("window.__monacoLoaderPromise");
    }

    // -------------------------------------------------------------------------
    // G5 — every <label> in IntakeForm.jsx must have a matching htmlFor/id pair;
    // no orphan labels, no unlabeled input/select/textarea.
    // -------------------------------------------------------------------------

    @Test
    void intakeFormJsx_everyLabelHasMatchingHtmlForAndId() throws IOException {
        String jsx = readClasspathResource("static/IntakeForm.jsx");

        Pattern labelPattern = Pattern.compile("<label\\s+htmlFor=\"([\\w-]+)\"");
        Matcher labelMatcher = labelPattern.matcher(jsx);

        int labelCount = 0;
        while (labelMatcher.find()) {
            labelCount++;
            String forId = labelMatcher.group(1);
            assertThat(jsx)
                .as("expected an element with id=\"%s\" matching <label htmlFor=\"%s\">", forId, forId)
                .contains("id=\"" + forId + "\"");
        }

        assertThat(labelCount).as("IntakeForm should have labelled form fields").isGreaterThan(0);

        // No orphan (unassociated) labels
        long plainLabelCount = jsx.lines().filter(line -> line.trim().startsWith("<label")).count();
        assertThat(plainLabelCount).isEqualTo(labelCount);
    }

    // -------------------------------------------------------------------------
    // G4 — chat message container announces new turns to screen readers
    // -------------------------------------------------------------------------

    @Test
    void interviewScreenJsx_chatContainerHasAriaLiveRegion() throws IOException {
        String jsx = readClasspathResource("static/InterviewScreen.jsx");

        assertThat(jsx).contains("aria-live=\"polite\"");
        assertThat(jsx).contains("aria-relevant=\"additions\"");
    }

    private String readClasspathResource(String path) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("%s must be on the classpath", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
