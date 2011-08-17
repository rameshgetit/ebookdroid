package org.ebookdroid.core.curl;

import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageDocumentView;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class SinglePageSlider extends AbstractPageSlider {

    public SinglePageSlider(final SinglePageDocumentView singlePageDocumentView) {
        super(PageAnimationType.SLIDER, singlePageDocumentView);
    }

    /**
     * Draw the foreground
     *
     * @param canvas
     * @param rect
     * @param paint
     */
    @Override
    protected void drawForeground(final Canvas canvas) {
        Page page = view.getBase().getDocumentModel().getPageObject(foreIndex);
        if (page == null) {
            page = view.getBase().getDocumentModel().getCurrentPageObject();
        }
        if (page != null) {
            final Bitmap fore = getBitmap(canvas);
            final Canvas tmp = new Canvas(fore);
            page.draw(tmp, true);

            final Rect src = new Rect((int) mA.x, 0, view.getWidth(), view.getHeight());
            final RectF dst = new RectF(0, 0, view.getWidth() - mA.x, view.getHeight());
            // Rect src = new Rect(0, 0, view.getWidth(), view.getHeight());
            // RectF dst = new RectF(0, 0, view.getWidth() - mA.x, view.getHeight());
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            canvas.drawBitmap(fore, src, dst, paint);
        }
    }

    /**
     * Draw the background image.
     *
     * @param canvas
     * @param rect
     * @param paint
     */
    @Override
    protected void drawBackground(final Canvas canvas) {
        final Page page = view.getBase().getDocumentModel().getPageObject(backIndex);
        if (page != null) {
            final Bitmap back = getBitmap(canvas);
            final Canvas tmp = new Canvas(back);
            page.draw(tmp, true);

            // Rect src = new Rect(0, 0, view.getWidth(), view.getHeight());
            // RectF dst = new RectF(view.getWidth() - mA.x, 0, view.getWidth(), view.getHeight());
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            final Rect src = new Rect(0, 0, (int) mA.x, view.getHeight());
            final RectF dst = new RectF(view.getWidth() - mA.x, 0, view.getWidth(), view.getHeight());
            canvas.drawBitmap(back, src, dst, paint);
        }

    }

}
