package cz.oluwagbemiga.eutax.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartWindowTest {

    private StartWindow window;

    @BeforeAll
    static void configureHeadless() {
        System.setProperty("java.awt.headless", "false");
    }

    @AfterEach
    void tearDown() {
        SwingTestUtils.dispose(window);
        SwingTestUtils.disposeAll(SourcesSelectionWindow.class);
        window = null;
    }

    @Test
    void newReportButtonOpensSelectionWindow() {
        window = SwingTestUtils.createOnEdt(StartWindow::new);

        assertNotNull(window, "Start window should be created");
        assertTrue(window.isDisplayable(), "Start window should be realized");

        AbstractButton newReport = SwingTestUtils.findButton(window.getContentPane(), "Nová kontrola");
        assertNotNull(newReport, "New report button should exist");

        SwingTestUtils.runOnEdt(newReport::doClick);
        SwingTestUtils.flushEdt();

        assertTrue(SwingTestUtils.windowExists(SourcesSelectionWindow.class),
                "SourcesSelectionWindow should be shown after clicking the button");
        assertTrue(!window.isDisplayable(), "Original StartWindow should be disposed after navigation");
    }
}
