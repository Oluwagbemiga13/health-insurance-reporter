package cz.oluwagbemiga.eutax.ui;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import java.awt.Image;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides the shared application icon for every window, ensuring future frames reuse the same image.
 */
@Slf4j
public final class AppIconProvider {

    private static final String ICON_RESOURCE = "/assets/at_logo.jpg";
    private static List<Image> cachedIcons;

    private AppIconProvider() {
        throw new IllegalStateException("Utility class");
    }

    public static void apply(Window window) {
        if (window == null) {
            return;
        }
        List<Image> icons = getIcons();
        if (!icons.isEmpty()) {
            window.setIconImages(icons);
        }
    }

    private static List<Image> getIcons() {
        List<Image> icons = cachedIcons;
        if (icons != null) {
            return icons;
        }
        synchronized (AppIconProvider.class) {
            if (cachedIcons == null) {
                cachedIcons = loadIcons();
            }
            return cachedIcons;
        }
    }

    private static List<Image> loadIcons() {
        try (InputStream stream = AppIconProvider.class.getResourceAsStream(ICON_RESOURCE)) {
            if (stream == null) {
                log.warn("Application icon {} not found; falling back to default.", ICON_RESOURCE);
                return fallbackIcon();
            }
            Image image = ImageIO.read(stream);
            if (image == null) {
                log.warn("Unable to decode application icon {}; falling back to default.", ICON_RESOURCE);
                return fallbackIcon();
            }
            List<Image> icons = new ArrayList<>();
            icons.add(image);
            return Collections.unmodifiableList(icons);
        } catch (IOException ex) {
            log.warn("Failed to load application icon {}; falling back to default.", ICON_RESOURCE, ex);
            return fallbackIcon();
        }
    }

    private static List<Image> fallbackIcon() {
        Icon i = UIManager.getIcon("OptionPane.informationIcon");
        if (i instanceof ImageIcon icon) {
            Image image = icon.getImage();
            if (image != null) {
                return Collections.singletonList(image);
            }
        }
        return Collections.emptyList();
    }
}

