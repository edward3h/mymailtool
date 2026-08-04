package org.ethelred.mymailtool2;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Computes a deterministic fingerprint over an ordered list of config files.
 * <p>
 * The fingerprint includes each file's normalized absolute path and whether it
 * exists, in addition to its content. Hashing the path and existence flag - not
 * just the content - means that adding or removing a config file from the
 * resolved set (even one that does not exist on disk) changes the fingerprint,
 * not just editing the content of files that happen to exist.
 */
public final class ConfigFingerprint
{
    private ConfigFingerprint()
    {
    }

    /**
     * Compute a fingerprint for the given files, iterated in the order provided.
     *
     * @param files the config files to fingerprint, in a deterministic order
     * @return a hex-encoded SHA-256 based fingerprint
     * @throws IOException if a file that exists cannot be read
     */
    public static String compute(List<File> files) throws IOException
    {
        Hasher hasher = Hashing.sha256().newHasher();
        for (File file : files)
        {
            File normalized = file.getAbsoluteFile();
            hasher.putString(normalized.toPath().normalize().toString(), StandardCharsets.UTF_8);
            boolean exists = normalized.exists();
            hasher.putByte((byte) (exists ? 1 : 0));
            if (exists)
            {
                hasher.putBytes(Files.readAllBytes(normalized.toPath()));
            }
        }
        return hasher.hash().toString();
    }
}
