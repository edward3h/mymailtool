package org.ethelred.mymailtool2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * tests for ConfigFingerprint
 */
public class ConfigFingerprintTest
{
    @Test
    public void sameFileSameContentHashedTwiceIsIdentical(@TempDir File dir) throws IOException
    {
        File f = new File(dir, "a.conf");
        Files.writeString(f.toPath(), "content");

        String fp1 = ConfigFingerprint.compute(List.of(f));
        String fp2 = ConfigFingerprint.compute(List.of(f));

        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    public void sameContentDifferentPathsProducesDifferentFingerprint(@TempDir File dir) throws IOException
    {
        File a = new File(dir, "a.conf");
        File b = new File(dir, "b.conf");
        Files.writeString(a.toPath(), "content");
        Files.writeString(b.toPath(), "content");

        String fpA = ConfigFingerprint.compute(List.of(a));
        String fpB = ConfigFingerprint.compute(List.of(b));

        assertThat(fpA).isNotEqualTo(fpB);
    }

    @Test
    public void contentChangeProducesDifferentFingerprint(@TempDir File dir) throws IOException
    {
        File f = new File(dir, "a.conf");
        Files.writeString(f.toPath(), "content");
        String fp1 = ConfigFingerprint.compute(List.of(f));

        Files.writeString(f.toPath(), "different content");
        String fp2 = ConfigFingerprint.compute(List.of(f));

        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    public void addingFileToListChangesFingerprint(@TempDir File dir) throws IOException
    {
        File a = new File(dir, "a.conf");
        File b = new File(dir, "b.conf");
        Files.writeString(a.toPath(), "content-a");
        Files.writeString(b.toPath(), "content-b");

        String fpBefore = ConfigFingerprint.compute(List.of(a));
        String fpAfter = ConfigFingerprint.compute(List.of(a, b));

        assertThat(fpBefore).isNotEqualTo(fpAfter);
    }

    @Test
    public void removingFileFromListChangesFingerprint(@TempDir File dir) throws IOException
    {
        File a = new File(dir, "a.conf");
        File b = new File(dir, "b.conf");
        Files.writeString(a.toPath(), "content-a");
        Files.writeString(b.toPath(), "content-b");

        String fpBefore = ConfigFingerprint.compute(List.of(a, b));
        String fpAfter = ConfigFingerprint.compute(List.of(a));

        assertThat(fpBefore).isNotEqualTo(fpAfter);
    }

    @Test
    public void missingFileDoesNotThrowAndProducesFingerprint(@TempDir File dir) throws IOException
    {
        File missing = new File(dir, "does-not-exist.conf");

        String fingerprint = ConfigFingerprint.compute(List.of(missing));

        assertThat(fingerprint).isNotNull();
    }

    @Test
    public void missingFileFingerprintDiffersFromExistingFileWithContent(@TempDir File dir) throws IOException
    {
        File f = new File(dir, "maybe.conf");

        String fpMissing = ConfigFingerprint.compute(List.of(f));

        Files.writeString(f.toPath(), "some content");
        String fpExisting = ConfigFingerprint.compute(List.of(f));

        assertThat(fpMissing).isNotEqualTo(fpExisting);
    }

    @Test
    public void emptyFileListProducesValidDeterministicFingerprint() throws IOException
    {
        String fp1 = ConfigFingerprint.compute(List.of());
        String fp2 = ConfigFingerprint.compute(List.of());

        assertThat(fp1).isNotNull();
        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    public void orderOfFilesAffectsFingerprint(@TempDir File dir) throws IOException
    {
        File a = new File(dir, "a.conf");
        File b = new File(dir, "b.conf");
        Files.writeString(a.toPath(), "content-a");
        Files.writeString(b.toPath(), "content-b");

        String fpAB = ConfigFingerprint.compute(List.of(a, b));
        String fpBA = ConfigFingerprint.compute(List.of(b, a));

        assertThat(fpAB).isNotEqualTo(fpBA);
    }
}
