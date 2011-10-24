package org.ebookdroid.pdfdroid.codec;

import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecPage;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

public class PdfPage implements CodecPage {

    private static final boolean useFzGeometry = true;

    private static final boolean useNativeGraphics;

    static {
        useNativeGraphics = isNativeGraphicsAvailable();
    }

    private long pageHandle;
    private final long docHandle;
    private final RectF mediaBox;
    private final int rotation;

    private PdfPage(final long pageHandle, final long docHandle) {
        this.pageHandle = pageHandle;
        this.docHandle = docHandle;
        this.mediaBox = getMediaBox();
        this.rotation = getRotate(pageHandle);
    }

    @Override
    public int getWidth() {
        return PdfContext.getWidthInPixels(mediaBox.width());
    }

    @Override
    public int getHeight() {
        return PdfContext.getHeightInPixels(mediaBox.height());
    }

    @Override
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        final float[] matrixArray = useFzGeometry ? calculateFz(width, height, pageSliceBounds) : calculate(width,
                height, pageSliceBounds);
        return render(new Rect(0, 0, width, height), matrixArray);
    }

    private float[] calculate(final int width, final int height, final RectF pageSliceBounds) {
        final Matrix matrix = new Matrix();
        matrix.postTranslate(-mediaBox.left, -mediaBox.top);
        matrix.postScale(width / mediaBox.width(), -height / mediaBox.height());
        matrix.postTranslate(0, height);
        matrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
        matrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());

        final float[] matrixSource = new float[9];
        final float[] matrixArray = new float[6];

        matrix.getValues(matrixSource);
        matrixArray[0] = matrixSource[0];
        matrixArray[1] = matrixSource[3];
        matrixArray[2] = matrixSource[1];
        matrixArray[3] = matrixSource[4];
        matrixArray[4] = matrixSource[2];
        matrixArray[5] = matrixSource[5];

        return matrixArray;
    }

    private float[] calculateFz(final int width, final int height, final RectF pageSliceBounds) {
        FzGeometry.fz_matrix matrix = FzGeometry.fz_identity;

        // Check rotation
        switch (this.rotation) {
            case 90:
                matrix = FzGeometry.fz_concat(matrix, FzGeometry.fz_translate(-mediaBox.left, -mediaBox.bottom));
                break;
            case 180:
                matrix = FzGeometry.fz_concat(matrix, FzGeometry.fz_translate(-mediaBox.right, -mediaBox.bottom));
                break;
            case 270:
                matrix = FzGeometry.fz_concat(matrix, FzGeometry.fz_translate(-mediaBox.right, -mediaBox.top));
                break;
            case 0:
            default:
                matrix = FzGeometry.fz_concat(matrix, FzGeometry.fz_translate(-mediaBox.left, -mediaBox.top));
                break;
        }

        matrix = FzGeometry.fz_concat(matrix, FzGeometry.fz_rotate(rotation));

        matrix = FzGeometry.fz_concat(matrix,
                FzGeometry.fz_scale(width / mediaBox.width(), -height / mediaBox.height()));
        matrix = FzGeometry.fz_concat(matrix, FzGeometry.fz_translate(0, height));

        matrix = FzGeometry.fz_concat(matrix,
                FzGeometry.fz_translate(-pageSliceBounds.left * width, -pageSliceBounds.top * height));
        matrix = FzGeometry.fz_concat(matrix,
                FzGeometry.fz_scale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height()));

        final float[] matrixArray = new float[6];

        matrixArray[0] = matrix.a;
        matrixArray[1] = matrix.b;
        matrixArray[2] = matrix.c;
        matrixArray[3] = matrix.d;
        matrixArray[4] = matrix.e;
        matrixArray[5] = matrix.f;

        return matrixArray;
    }

    static PdfPage createPage(final long dochandle, final int pageno) {
        return new PdfPage(open(dochandle, pageno), dochandle);
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    @Override
    public synchronized void recycle() {
        if (pageHandle != 0) {
            free(pageHandle);
            pageHandle = 0;
        }
    }

    @Override
    public boolean isRecycled() {
        return pageHandle == 0;
    }

    private RectF getMediaBox() {
        final float[] box = new float[4];
        getMediaBox(pageHandle, box);
        return new RectF(box[0], box[1], box[2], box[3]);
    }

    public BitmapRef render(final Rect viewbox, final float[] ctm) {
        final int[] mRect = new int[4];
        mRect[0] = viewbox.left;
        mRect[1] = viewbox.top;
        mRect[2] = viewbox.right;
        mRect[3] = viewbox.bottom;

        final int width = viewbox.width();
        final int height = viewbox.height();

        System.out.println(String.format("Matrix: %f %f %f %f %f %f", ctm[0], ctm[1], ctm[2], ctm[3], ctm[4], ctm[5]));

        if (useNativeGraphics) {
            final BitmapRef bmp = BitmapManager.getBitmap(width, height, Bitmap.Config.ARGB_8888);
            if (renderPageBitmap(docHandle, pageHandle, mRect, ctm, bmp.getBitmap())) {
                return bmp;
            } else {
                BitmapManager.release(bmp);
                return null;
            }
        }

        final int[] bufferarray = new int[width * height];
        renderPage(docHandle, pageHandle, mRect, ctm, bufferarray);
        final BitmapRef b = BitmapManager.getBitmap(width, height, Bitmap.Config.RGB_565);
        b.getBitmap().setPixels(bufferarray, 0, width, 0, 0, width, height);
        return b;
    }

    private static native void getMediaBox(long handle, float[] mediabox);

    // TODO: use rotation when draw page
    private static native int getRotate(long handle);

    private static native void free(long handle);

    private static native long open(long dochandle, int pageno);

    private static native void renderPage(long dochandle, long pagehandle, int[] viewboxarray, float[] matrixarray,
            int[] bufferarray);

    private static native boolean isNativeGraphicsAvailable();

    private static native boolean renderPageBitmap(long dochandle, long pagehandle, int[] viewboxarray,
            float[] matrixarray, Bitmap bitmap);
}
