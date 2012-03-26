package org.ebookdroid.droids.djvu.codec;

import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;

import java.util.List;

public class DjvuDocument extends AbstractCodecDocument {

    DjvuDocument(final DjvuContext djvuContext, final String fileName) {
        super(djvuContext, open(djvuContext.getContextHandle(), fileName));
    }

    @Override
    public List<OutlineLink> getOutline() {
        final DjvuOutline ou = new DjvuOutline();
        return ou.getOutline(documentHandle);
    }

    @Override
    public DjvuPage getPage(final int pageNumber) {
        return new DjvuPage(documentHandle, getPage(documentHandle, pageNumber), pageNumber);
    }

    @Override
    public int getPageCount() {
        return getPageCount(documentHandle);
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageNumber) {
        final CodecPageInfo info = new CodecPageInfo();
        final int res = getPageInfo(documentHandle, pageNumber, context.getContextHandle(), info);
        if (res == -1) {
            return null;
        } else {
            return info;
        }
    }

    @Override
    protected void freeDocument() {
        free(documentHandle);
    }

    private native static int getPageInfo(long docHandle, int pageNumber, long contextHandle, CodecPageInfo cpi);

    private native static long open(long contextHandle, String fileName);

    private native static long getPage(long docHandle, int pageNumber);

    private native static int getPageCount(long docHandle);

    private native static void free(long pageHandle);

}
