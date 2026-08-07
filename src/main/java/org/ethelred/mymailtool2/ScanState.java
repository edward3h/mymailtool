package org.ethelred.mymailtool2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Persisted incremental scan state: for each (account, IMAP folder) pair, the
 * folder's {@code UIDVALIDITY} and the next UID to scan from, plus a single
 * config fingerprint used elsewhere to decide whether this whole cache is
 * still trustworthy.
 * <p>
 * This class purely loads/holds/saves that state; it makes no decisions about
 * UID ranges or cache trust - see {@code FolderScanCache} for that.
 */
public class ScanState
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIG_FINGERPRINT_KEY = "configFingerprint";
    private static final Pattern FOLDER_ACCOUNT_KEY = Pattern.compile("^folder\\.(\\d+)\\.account$");
    private static final String FILE_COMMENT = "mymailtool scan-state file - do not edit by hand";

    /**
     * Per-folder scan state: the folder's {@code UIDVALIDITY} at the time of the
     * last scan, and the next UID to resume scanning from.
     */
    public record FolderScanState(long uidValidity, long nextUid)
    {
    }

    private record FolderKey(String account, String folderFullName)
    {
    }

    private final Map<FolderKey, FolderScanState> folderStates = new HashMap<>();

    private String configFingerprint;

    public String getConfigFingerprint()
    {
        return configFingerprint;
    }

    public void setConfigFingerprint(String configFingerprint)
    {
        this.configFingerprint = configFingerprint;
    }

    public Optional<FolderScanState> get(String account, String folderFullName)
    {
        return Optional.ofNullable(folderStates.get(new FolderKey(account, folderFullName)));
    }

    public void put(String account, String folderFullName, FolderScanState state)
    {
        folderStates.put(new FolderKey(account, folderFullName), state);
    }

    /**
     * Load scan state from {@code file}. Never throws: a missing file yields a
     * fresh empty state with no warning logged, while a file that exists but
     * cannot be parsed yields a fresh empty state with a warning logged.
     */
    public static ScanState loadOrEmpty(File file)
    {
        if (!file.exists())
        {
            return new ScanState();
        }
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(file))
        {
            properties.load(in);
        }
        catch (Exception ex)
        {
            LOGGER.warn("Failed to read scan-state file {}, ignoring and starting fresh", file, ex);
            return new ScanState();
        }
        ScanState state = new ScanState();
        state.configFingerprint = properties.getProperty(CONFIG_FINGERPRINT_KEY);
        for (String key : properties.stringPropertyNames())
        {
            Matcher m = FOLDER_ACCOUNT_KEY.matcher(key);
            if (!m.matches())
            {
                continue;
            }
            String index = m.group(1);
            try
            {
                String account = requireProperty(properties, "folder." + index + ".account");
                String name = requireProperty(properties, "folder." + index + ".name");
                long uidValidity = Long.parseLong(requireProperty(properties, "folder." + index + ".uidValidity"));
                long nextUid = Long.parseLong(requireProperty(properties, "folder." + index + ".nextUid"));
                state.put(account, name, new FolderScanState(uidValidity, nextUid));
            }
            catch (RuntimeException ex)
            {
                LOGGER.warn("Skipping malformed scan-state folder entry at index {} in {}", index, file, ex);
            }
        }
        return state;
    }

    private static String requireProperty(Properties properties, String key)
    {
        String value = properties.getProperty(key);
        if (value == null)
        {
            throw new IllegalStateException("Missing required property " + key);
        }
        return value;
    }

    /**
     * Save this state to {@code file}, atomically. Writes to a sibling temp file
     * first, then renames it into place so a crash mid-write cannot leave a
     * corrupt or partially-written target file.
     */
    public void save(File file) throws IOException
    {
        File parent = file.getParentFile();
        if (parent != null)
        {
            parent.mkdirs();
        }

        Properties properties = new Properties();
        if (configFingerprint != null)
        {
            properties.setProperty(CONFIG_FINGERPRINT_KEY, configFingerprint);
        }
        int index = 0;
        for (Map.Entry<FolderKey, FolderScanState> entry : folderStates.entrySet())
        {
            String prefix = "folder." + index + ".";
            properties.setProperty(prefix + "account", entry.getKey().account());
            properties.setProperty(prefix + "name", entry.getKey().folderFullName());
            properties.setProperty(prefix + "uidValidity", Long.toString(entry.getValue().uidValidity()));
            properties.setProperty(prefix + "nextUid", Long.toString(entry.getValue().nextUid()));
            index++;
        }

        File tmpFile = new File(parent, file.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmpFile))
        {
            properties.store(out, FILE_COMMENT);
        }
        Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
