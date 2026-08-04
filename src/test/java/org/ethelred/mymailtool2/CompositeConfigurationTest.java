package org.ethelred.mymailtool2;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.ethelred.util.TestUtil;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.ethelred.util.TestUtil.assertEmpty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class CompositeConfigurationTest
{
    @Test
    public void testEmptyConfiguration()
    {
        MailToolConfiguration empty = new CompositeConfiguration();
        assertThat(empty.getOperationLimit()).isEqualTo(-1);
        assertThat(empty.getUser()).isNull();
        assertThat(empty.getMinAge()).isNull();
        assertThat(empty.getPassword()).isNull();
        assertEmpty(empty.getFileLocations());
        assertEmpty(empty.getFileHandlers());
    }

    @Test
    public void testSingleConfiguration()
    {

        MailToolConfiguration mock = new MailToolConfiguration()
        {
            @Override
            public String getPassword()
            {
                return "password";
            }

            @Override
            public Map<String, String> getMailProperties()
            {
                return Map.of("test", "mail");
            }

            @Override
            public String getUser()
            {
                return "user";
            }

            @Override
            public Iterable<String> getFileLocations()
            {
                return Lists.newArrayList("file1");
            }

            @Override
            public Task getTask() throws Exception
            {
                return null;
            }

            @Override
            public int getOperationLimit()
            {
                return 1000;
            }

            @Override
            public String getMinAge()
            {
                return "3 months";
            }

            @Override
            public Iterable<FileConfigurationHandler> getFileHandlers()
            {
                return Collections.emptyList();
            }

            @Override
            public String getTimeLimit()
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public boolean verbose()
            {
                return false;
            }

            @Override
            public int getChunkSize()
            {
                return PRIMITIVE_DEFAULT;
            }

            @Override
            public boolean randomTraversal() {
                return false;
            }

            @Override
            public String getScanStateFile()
            {
                return null;
            }

            @Override
            public boolean disableScanCache()
            {
                return false;
            }
        };

        MailToolConfiguration comp = new CompositeConfiguration(mock);
        assertThat(comp.getOperationLimit()).isEqualTo(mock.getOperationLimit());
        assertThat(comp.getUser()).isEqualTo(mock.getUser());
        assertThat(comp.getMinAge()).isEqualTo(mock.getMinAge());
        assertThat(comp.getPassword()).isEqualTo(mock.getPassword());
        TestUtil.assertEquals(mock.getFileLocations(), comp.getFileLocations());
        TestUtil.assertEquals(mock.getFileHandlers(), comp.getFileHandlers());
    }

    @Test
    public void testInsertion()
    {
        final Iterable<String> testFileLocs = ImmutableList.of("loc1", "loc2");
        final Iterable<String> testFileLocs2 = ImmutableList.of("ins1");
        final MailToolConfiguration mockDefault = mock(MailToolConfiguration.class, "MTCdefault");
        final MailToolConfiguration mockFile1 = mock(MailToolConfiguration.class, "MTCfile1");
        final MailToolConfiguration mockFile2 = mock(MailToolConfiguration.class, "MTCfile2");
        Map<String, MailToolConfiguration> mockFiles = ImmutableMap.of(
                "loc1", mockFile1,
                "ins1", mockFile2
        );
        CompositeConfiguration cmp = new CompositeConfiguration(mockDefault);

        when(mockDefault.getFileLocations()).thenReturn(testFileLocs);
        when(mockFile1.getFileLocations()).thenReturn(testFileLocs2);
        when(mockFile2.getFileLocations()).thenReturn(Collections.emptyList());

        int counter = 0;
        for (String filename : cmp.getFileLocations())
        {
            MailToolConfiguration fileConf = mockFiles.get(filename);
            if (fileConf != null)
            {
                cmp.insert(fileConf);
            }
            counter++;
            assertThat(counter).isLessThan(5);
        }

        verify(mockDefault).getFileLocations();
        verify(mockFile1).getFileLocations();
        verify(mockFile2).getFileLocations();
    }

    @Test
    public void testGetScanStateFilePicksFirstNonNull()
    {
        final MailToolConfiguration mockFirst = mock(MailToolConfiguration.class);
        final MailToolConfiguration mockSecond = mock(MailToolConfiguration.class);
        when(mockFirst.getScanStateFile()).thenReturn(null);
        when(mockSecond.getScanStateFile()).thenReturn("/path/to/scan-state.properties");

        CompositeConfiguration cmp = new CompositeConfiguration(mockFirst, mockSecond);

        assertThat(cmp.getScanStateFile()).isEqualTo("/path/to/scan-state.properties");
    }

    @Test
    public void testDisableScanCacheTrueIfAnySubConfigTrue()
    {
        final MailToolConfiguration mockFirst = mock(MailToolConfiguration.class);
        final MailToolConfiguration mockSecond = mock(MailToolConfiguration.class);
        when(mockFirst.disableScanCache()).thenReturn(false);
        when(mockSecond.disableScanCache()).thenReturn(true);

        CompositeConfiguration cmp = new CompositeConfiguration(mockFirst, mockSecond);

        assertThat(cmp.disableScanCache()).isTrue();
    }
}
