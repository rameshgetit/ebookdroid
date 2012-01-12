package org.ebookdroid.fb2droid.codec;

import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecPage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.ArrayList;

public class FB2Page implements CodecPage {

    public static final int PAGE_WIDTH = 800;

    public static final int PAGE_HEIGHT = 1176;

    public static final int MARGIN_X = 20;

    public static final int MARGIN_Y = 20;

    private static final Bitmap bitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.RGB_565);

    private static final RectF PAGE_RECT = new RectF(0, 0, PAGE_WIDTH, PAGE_HEIGHT);

    private final ArrayList<FB2Line> lines = new ArrayList<FB2Line>(PAGE_HEIGHT / RenderingStyle.TEXT_SIZE);
    final ArrayList<FB2Line> noteLines = new ArrayList<FB2Line>(PAGE_HEIGHT / RenderingStyle.FOOTNOTE_SIZE);

    private boolean committed = false;
    int contentHeight = 0;

    @Override
    public int getHeight() {
        return PAGE_HEIGHT;
    }

    @Override
    public int getWidth() {
        return PAGE_WIDTH;
    }

    @Override
    public void recycle() {
    }

    @Override
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        try {
            renderPage();

            final Matrix matrix = new Matrix();
            matrix.postScale((float) width / bitmap.getWidth(), (float) height / bitmap.getHeight());
            matrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
            matrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());

            final BitmapRef bmp = BitmapManager.getBitmap("FB2 page", width, height, Bitmap.Config.RGB_565);

            final Canvas c = new Canvas(bmp.getBitmap());
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            c.drawBitmap(bitmap, matrix, paint);

            return bmp;
        } finally {
        }
    }

    private void renderPage() {
        final Canvas c = new Canvas(bitmap);

        final Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        c.drawRect(PAGE_RECT, paint);
        paint.setColor(Color.BLACK);

        int y = MARGIN_Y;
        for (final FB2Line line : lines) {
            y += line.getHeight();
            line.render(c, y);
        }
        for (final FB2Line line : noteLines) {
            y += line.getHeight();
            line.render(c, y);
        }
    }

    public void appendLine(final FB2Line line) {
        if (committed) {
            return;
        }
        lines.add(line);
        contentHeight += line.getHeight();
    }

    public static FB2Page getLastPage(final ArrayList<FB2Page> pages) {
        if (pages.size() == 0) {
            pages.add(new FB2Page());
        }
        FB2Page fb2Page = pages.get(pages.size() - 1);
        if (fb2Page.committed) {
            fb2Page = new FB2Page();
            pages.add(fb2Page);
        }
        return fb2Page;
    }

    public void appendNoteLine(final FB2Line line) {
        noteLines.add(line);
        contentHeight += line.getHeight();
    }

    public void commit() {
        if (committed) {
            return;
        }
        final int h = FB2Page.PAGE_HEIGHT - contentHeight - 2 * FB2Page.MARGIN_Y;
        if (h > 0) {
            lines.add(new FB2Line().append(new FB2LineFixedWhiteSpace(0, h)));
            contentHeight += h;
        }
        for (final FB2Line line : noteLines) {
            lines.add(line);
        }
        noteLines.clear();
        committed = true;
    }

    @Override
    public boolean isRecycled() {
        return false;
    }
}
