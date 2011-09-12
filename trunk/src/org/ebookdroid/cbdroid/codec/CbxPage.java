package org.ebookdroid.cbdroid.codec;

import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.utils.archives.ArchiveEntry;
import org.ebookdroid.utils.BitmapManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

import java.io.IOException;
import java.io.InputStream;

public class CbxPage<ArchiveEntryType extends ArchiveEntry> implements CodecPage {

    private final ArchiveEntryType entry;

    private CodecPageInfo pageInfo;

    public CbxPage(final ArchiveEntryType entry) {
        this.entry = entry;
    }

    private Bitmap decode(final boolean onlyBounds) {
        return decode(onlyBounds, 1);
    }

    private Bitmap decode(final boolean onlyBounds, final int scale) {
        if (entry == null) {
            return null;
        }

        if (CbxDocument.LCTX.isDebugEnabled()) {
            CbxDocument.LCTX.d("Starting " + (onlyBounds ? " partial" : "full") + " decompressing: " + entry.getName());
        }
        try {
            final InputStream is = entry.open();
            try {
                final Options opts = new Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                opts.inJustDecodeBounds = onlyBounds;
                opts.inSampleSize = scale;

                final Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
                pageInfo = new CodecPageInfo();
                if (onlyBounds) {
                    pageInfo.setHeight(opts.outHeight * scale);
                    pageInfo.setWidth(opts.outWidth * scale);
                } else {
                    pageInfo.setHeight(bitmap.getHeight() * scale);
                    pageInfo.setWidth(bitmap.getWidth() * scale);
                }
                return bitmap;
            } finally {
                try {
                    is.close();
                } catch (final IOException ex) {
                }
                if (CbxDocument.LCTX.isDebugEnabled()) {
                    CbxDocument.LCTX.d("Finishing" + (onlyBounds ? " partial" : "full") + " decompressing: " + entry.getName());
                }
            }
        } catch (final Throwable e) {
            if (CbxDocument.LCTX.isDebugEnabled()) {
                CbxDocument.LCTX.d("Can not decompress page: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public int getHeight() {
        return getPageInfo().getHeight();
    }

    @Override
    public int getWidth() {
        return getPageInfo().getWidth();
    }

    @Override
    public void recycle() {
    }

    @Override
    public Bitmap renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        if (getPageInfo() == null) {
            return null;
        }
        Bitmap bitmap = null;
        try {

            float requiredWidth = (float) width / pageSliceBounds.width();
            float requiredHeight = (float) height / pageSliceBounds.height();

            int scale = 1;
            int widthTmp = getWidth();
            int heightTmp = getHeight();
            while (true) {
                if (widthTmp / 2 < requiredWidth || heightTmp / 2 < requiredHeight) {
                    break;
                }
                widthTmp /= 2;
                heightTmp /= 2;

                scale *= 2;
            }

            bitmap = decode(false, scale);
            if (bitmap == null) {
                return null;
            }

            final Matrix matrix = new Matrix();
            matrix.postScale((float) width / bitmap.getWidth(), (float) height / bitmap.getHeight());
            matrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
            matrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());

            final Bitmap bmp = BitmapManager.getBitmap(width, height, Bitmap.Config.RGB_565);

            final Canvas c = new Canvas(bmp);
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            c.drawBitmap(bitmap, matrix, paint);

            return bmp;
        } finally {
            if (bitmap != null) {
                BitmapManager.recycle(bitmap);
            }
        }
    }

    public CodecPageInfo getPageInfo() {
        if (pageInfo == null) {
            decode(true);
        }
        return pageInfo;
    }
}
