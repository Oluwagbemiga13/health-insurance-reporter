package cz.oluwagbemiga.eutax.tools;

import cz.oluwagbemiga.eutax.pojo.InsuranceCompany;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class FileMoverTest {

    // Reflection helper to call private static methods
    private static Object callPrivateStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Throwable {
        Method m = FileMover.class.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        try {
            return m.invoke(null, args);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }

    @Test
    void normalizeForComparison_nullOrVariants() throws Throwable {
        assertEquals("", callPrivateStatic("normalizeForComparison", new Class<?>[]{String.class}, (Object) null));
        assertEquals("A", callPrivateStatic("normalizeForComparison", new Class<?>[]{String.class}, " a "));
        // diacritics removed and normalized
        assertEquals("ZP SKODA", callPrivateStatic("normalizeForComparison", new Class<?>[]{String.class}, "ZP Škoda"));
        // underscores and dashes collapsed to spaces and uppercased
        // note: separators become individual spaces so duplicates can remain
        assertEquals("ABC DEF DEF", callPrivateStatic("normalizeForComparison", new Class<?>[]{String.class}, "abc_def---DEF"));
        // leading non-alphanumeric removed
        assertEquals("TEST", callPrivateStatic("normalizeForComparison", new Class<?>[]{String.class}, "#@!test"));
    }

    @Test
    void parentMatches_tokenContainment() throws Throwable {
        // exact match
        assertTrue((Boolean) callPrivateStatic("parentMatches", new Class<?>[]{String.class, String.class}, "A B C", "A B C"));
        // display tokens contiguous sequence inside parent
        assertTrue((Boolean) callPrivateStatic("parentMatches", new Class<?>[]{String.class, String.class}, "X A B C Y", "A B C"));
        // not a match
        assertFalse((Boolean) callPrivateStatic("parentMatches", new Class<?>[]{String.class, String.class}, "A B D", "A C"));
        // display tokens empty -> false
        assertFalse((Boolean) callPrivateStatic("parentMatches", new Class<?>[]{String.class, String.class}, "A B", ""));
    }

    @Test
    void findTargetFolder_matchesDisplayName(@TempDir Path tempRoot) throws IOException {
        // create a directory that should match InsuranceCompany.ZP_SKODA (display name contains "ZP Škoda")
        Path folder = tempRoot.resolve("ZP Škoda Reports");
        Files.createDirectories(folder);

        FileMover mover = new FileMover(tempRoot);
        Optional<Path> found = mover.findTargetFolder(InsuranceCompany.ZP_SKODA);
        assertTrue(found.isPresent());
        assertEquals(folder.getFileName().toString(), found.get().getFileName().toString());
    }

    @Test
    void ensureOrCreateFolder_createsWhenMissing(@TempDir Path tempRoot) throws IOException {
        FileMover mover = new FileMover(tempRoot);
        Path created = mover.ensureOrCreateFolder(InsuranceCompany.OZP);
        assertTrue(Files.isDirectory(created));
        assertEquals(InsuranceCompany.OZP.getDisplayNames().get(0), created.getFileName().toString());
    }

    @Test
    void moveFileToFolder_movesAndReplaces(@TempDir Path tempRoot) throws IOException {
        Path sourceFile = tempRoot.resolve("report.pdf");
        Files.writeString(sourceFile, "content");

        Path targetDir = tempRoot.resolve("targetDir");
        FileMover mover = new FileMover(tempRoot);
        Path dest = mover.moveFileToFolder(sourceFile, targetDir);

        assertTrue(Files.exists(dest));
        assertFalse(Files.exists(sourceFile));
        assertEquals("report.pdf", dest.getFileName().toString());

        // moving again should replace existing file
        Path newSource = tempRoot.resolve("report.pdf");
        Files.writeString(newSource, "new");
        Path dest2 = mover.moveFileToFolder(newSource, targetDir);
        assertTrue(Files.exists(dest2));
        assertFalse(Files.exists(newSource));
    }

    @Test
    void findTargetFolder_returnsEmptyWhenRootNotDirectory() throws IOException {
        // create a file instead of a directory
        Path file = Files.createTempFile("notadir", ".tmp");
        FileMover mover = new FileMover(file);
        Optional<Path> found = mover.findTargetFolder(InsuranceCompany.VZP);
        assertTrue(found.isEmpty());
    }
}
