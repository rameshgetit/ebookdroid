package org.ebookdroid.core.utils.archives.zip;

import org.ebookdroid.core.utils.archives.ArchiveFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ZipArchive implements ArchiveFile<ZipArchiveEntry> {

    private final ZipFile zipfile;

    public ZipArchive(final File zipfile) throws IOException {
        try {
            this.zipfile = new ZipFile(zipfile);
        } catch (final ZipException ex) {
            final IOException exx = new IOException(ex.getMessage());
            exx.initCause(ex);
            throw exx;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            zipfile.close();
        } catch (Exception e) {
        }
        super.finalize();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.utils.archives.ArchiveFile#randomAccessAllowed()
     */
    @Override
    public boolean randomAccessAllowed() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.utils.archives.ArchiveFile#entries()
     */
    @Override
    public Enumeration<ZipArchiveEntry> entries() {
        return new Enumeration<ZipArchiveEntry>() {

            private final Enumeration<? extends ZipEntry> en = zipfile.entries();

            @Override
            public boolean hasMoreElements() {
                return en.hasMoreElements();
            }

            @Override
            public ZipArchiveEntry nextElement() {
                return new ZipArchiveEntry(ZipArchive.this, en.nextElement());
            }
        };
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.utils.archives.ArchiveFile#open(org.ebookdroid.core.utils.archives.ArchiveEntry)
     */
    @Override
    public InputStream open(final ZipArchiveEntry entry) throws IOException {
        return zipfile.getInputStream(entry.entry);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.utils.archives.ArchiveFile#close()
     */
    @Override
    public void close() throws IOException {
        zipfile.close();
    }

}
