package org.ebookdroid;

import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.droids.cbx.CbrContext;
import org.ebookdroid.droids.cbx.CbzContext;
import org.ebookdroid.droids.djvu.codec.DjvuContext;
import org.ebookdroid.droids.fb2.codec.FB2Context;
import org.ebookdroid.droids.mupdf.codec.PdfContext;
import org.ebookdroid.droids.mupdf.codec.XpsContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum CodecType {

    PDF(PdfContext.class, Arrays.asList("pdf"), Arrays.asList("application/pdf")),

    DJVU(DjvuContext.class, Arrays.asList("djvu", "djv"), Arrays.asList("image/djvu", "image/vnd.djvu", "image/x-djvu")),

    XPS(XpsContext.class, Arrays.asList("xps", "oxps"), Arrays.asList("application/vnd.ms-xpsdocument",
            "application/oxps")),

    CBZ(CbzContext.class, Arrays.asList("cbz"), Arrays.asList("application/x-cbz")),

    CBR(CbrContext.class, Arrays.asList("cbr"), Arrays.asList("application/x-cbr")),

    FB2(FB2Context.class, Arrays.asList("fb2", "fb2.zip"), Arrays.asList("application/x-fb2"));

    private final static Map<String, CodecType> extensionToActivity;

    private final static Map<String, CodecType> mimeTypesToActivity;

    static {
        extensionToActivity = new HashMap<String, CodecType>();
        for (final CodecType a : values()) {
            for (final String ext : a.extensions) {
                extensionToActivity.put(ext.toLowerCase(), a);
            }
        }
        mimeTypesToActivity = new HashMap<String, CodecType>();
        for (final CodecType a : values()) {
            for (final String type : a.mimeTypes) {
                mimeTypesToActivity.put(type.toLowerCase(), a);
            }
        }
    }

    private final Class<? extends CodecContext> contextClass;

    private final List<String> extensions;

    private final List<String> mimeTypes;

    private CodecType(final Class<? extends CodecContext> contextClass, final List<String> extensions,
            final List<String> mimeTypes) {
        this.contextClass = contextClass;
        this.extensions = extensions;
        this.mimeTypes = mimeTypes;
    }

    public Class<? extends CodecContext> getContextClass() {
        return contextClass;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public static Set<String> getAllExtensions() {
        return extensionToActivity.keySet();
    }

    public static Set<String> getAllMimeTypes() {
        return mimeTypesToActivity.keySet();
    }

    public static CodecType getByUri(final String uri) {
        final String uriString = uri.toLowerCase();
        for (final String ext : extensionToActivity.keySet()) {
            if (uriString.endsWith("." + ext)) {
                return extensionToActivity.get(ext);
            }
        }
        return null;
    }

    public static CodecType getByExtension(final String ext) {
        return extensionToActivity.get(ext.toLowerCase());
    }

    public static CodecType getByMimeType(final String type) {
        return mimeTypesToActivity.get(type.toLowerCase());
    }
}
