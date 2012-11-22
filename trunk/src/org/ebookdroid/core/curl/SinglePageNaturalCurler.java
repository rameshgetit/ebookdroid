/*
 * Copyright (C) 2007-2011 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.ebookdroid.core.curl;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.EventGLDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageController;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.ui.viewer.views.DragMark;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import org.emdev.ui.gl.GLCanvas;

/**
 * The Class SinglePageNaturalCurler.
 *
 * Used code from FBReader.
 */
public class SinglePageNaturalCurler extends AbstractPageAnimator {

    final Path forePath = new Path();
    final Path edgePath = new Path();
    final Path quadPath = new Path();

    private final Paint backPaint = new Paint();
    private final Paint edgePaint = new Paint();

    public SinglePageNaturalCurler(final SinglePageController singlePageDocumentView) {
        super(PageAnimationType.CURLER_DYNAMIC, singlePageDocumentView);

        backPaint.setAntiAlias(false);
        backPaint.setAlpha(0x40);

        edgePaint.setAntiAlias(true);
        edgePaint.setStyle(Paint.Style.FILL);
        edgePaint.setShadowLayer(15, 0, 0, 0xC0000000);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#getInitialXForBackFlip(int)
     */
    @Override
    protected int getInitialXForBackFlip(final int width) {
        return width << 1;
    }

    /**
     * Do the page curl depending on the methods we are using
     */
    @Override
    protected void updateValues() {
        final int width = view.getWidth();
        final int height = view.getHeight();

        mA.x = width - mMovement.x + 0.1f;
        mA.y = height - mMovement.y + 0.1f;

    }

//    @Override
//    protected void drawInternal(final EventDraw event) {
//        drawBackground(event);
//
//        final Canvas canvas = event.canvas;
//
//        final IBitmapRef fgBitmap = BitmapManager.getBitmap("Foreground", canvas.getWidth(), canvas.getHeight(),
//                Bitmap.Config.ARGB_8888);
//        try {
//            fgBitmap.eraseColor(Color.BLACK);
//            drawForeground(EventPool.newEventDraw(event, fgBitmap.getCanvas()));
//
//            final int myWidth = canvas.getWidth();
//            final int myHeight = canvas.getHeight();
//
//            final int cornerX = myWidth;
//            final int cornerY = myHeight;
//            final int oppositeX = -cornerX;
//            final int oppositeY = -cornerY;
//            final int dX = Math.max(1, Math.abs((int) mA.x - cornerX));
//            final int dY = Math.max(1, Math.abs((int) mA.y - cornerY));
//
//            final int x1 = cornerX == 0 ? (dY * dY / dX + dX) / 2 : cornerX - (dY * dY / dX + dX) / 2;
//            final int y1 = cornerY == 0 ? (dX * dX / dY + dY) / 2 : cornerY - (dX * dX / dY + dY) / 2;
//
//            float sX, sY;
//            {
//                final float d1 = (int) mA.x - x1;
//                final float d2 = (int) mA.y - cornerY;
//                sX = FloatMath.sqrt(d1 * d1 + d2 * d2) / 2;
//                if (cornerX == 0) {
//                    sX = -sX;
//                }
//            }
//            {
//                final float d1 = (int) mA.x - cornerX;
//                final float d2 = (int) mA.y - y1;
//                sY = FloatMath.sqrt(d1 * d1 + d2 * d2) / 2;
//                if (cornerY == 0) {
//                    sY = -sY;
//                }
//            }
//
//            forePath.rewind();
//            forePath.moveTo((int) mA.x, (int) mA.y);
//            forePath.lineTo(((int) mA.x + cornerX) / 2, ((int) mA.y + y1) / 2);
//            forePath.quadTo(cornerX, y1, cornerX, y1 - sY);
//            if (Math.abs(y1 - sY - cornerY) < myHeight) {
//                forePath.lineTo(cornerX, oppositeY);
//            }
//            forePath.lineTo(oppositeX, oppositeY);
//            if (Math.abs(x1 - sX - cornerX) < myWidth) {
//                forePath.lineTo(oppositeX, cornerY);
//            }
//            forePath.lineTo(x1 - sX, cornerY);
//            forePath.quadTo(x1, cornerY, ((int) mA.x + x1) / 2, ((int) mA.y + cornerY) / 2);
//
//            quadPath.moveTo(x1 - sX, cornerY);
//            quadPath.quadTo(x1, cornerY, ((int) mA.x + x1) / 2, ((int) mA.y + cornerY) / 2);
//            canvas.drawPath(quadPath, edgePaint);
//            quadPath.rewind();
//            quadPath.moveTo(((int) mA.x + cornerX) / 2, ((int) mA.y + y1) / 2);
//            quadPath.quadTo(cornerX, y1, cornerX, y1 - sY);
//            canvas.drawPath(quadPath, edgePaint);
//            quadPath.rewind();
//
//            canvas.save();
//            canvas.clipPath(forePath);
//            fgBitmap.draw(canvas, 0, 0, null);
//            canvas.restore();
//
//            edgePaint.setColor(fgBitmap.getAverageColor());
//
//            edgePath.rewind();
//            edgePath.moveTo((int) mA.x, (int) mA.y);
//            edgePath.lineTo(((int) mA.x + cornerX) / 2, ((int) mA.y + y1) / 2);
//            edgePath.quadTo(((int) mA.x + 3 * cornerX) / 4, ((int) mA.y + 3 * y1) / 4, ((int) mA.x + 7 * cornerX) / 8,
//                    ((int) mA.y + 7 * y1 - 2 * sY) / 8);
//            edgePath.lineTo(((int) mA.x + 7 * x1 - 2 * sX) / 8, ((int) mA.y + 7 * cornerY) / 8);
//            edgePath.quadTo(((int) mA.x + 3 * x1) / 4, ((int) mA.y + 3 * cornerY) / 4, ((int) mA.x + x1) / 2,
//                    ((int) mA.y + cornerY) / 2);
//
//            canvas.drawPath(edgePath, edgePaint);
//
//            canvas.save();
//            canvas.clipPath(edgePath);
//            final Matrix m = MatrixUtils.get();
//            m.postScale(1, -1);
//            m.postTranslate((int) mA.x - cornerX, (int) mA.y + cornerY);
//            final float angle;
//            if (cornerY == 0) {
//                angle = -180 / 3.1416f * (float) FastMath.atan2((int) mA.x - cornerX, (int) mA.y - y1);
//            } else {
//                angle = 180 - 180 / 3.1416f * (float) FastMath.atan2((int) mA.x - cornerX, (int) mA.y - y1);
//            }
//            m.postRotate(angle, (int) mA.x, (int) mA.y);
//            fgBitmap.draw(canvas, m, backPaint);
//            canvas.restore();
//        } finally {
//            BitmapManager.release(fgBitmap);
//        }
//
//    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#getLeftBound()
     */
    @Override
    protected float getLeftBound() {
        return 1 - view.getWidth();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#resetClipEdge()
     */
    @Override
    protected void resetClipEdge() {
        mMovement.x = 0;
        mMovement.y = 0;
        mOldMovement.x = 0;
        mOldMovement.y = 0;

        // Now set the points
        mA.set(0, 0);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#fixMovement(org.ebookdroid.core.curl.Vector2D, boolean)
     */
    @Override
    protected Vector2D fixMovement(final Vector2D point, final boolean bMaintainMoveDir) {
        return point;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawBackground(org.ebookdroid.core.EventGLDraw)
     */
    @Override
    protected void drawBackground(final EventGLDraw event) {
        final GLCanvas canvas = event.canvas;

        Page page = event.viewState.model.getPageObject(backIndex);
        if (page == null) {
            page = event.viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            canvas.save();
            event.process(page);
            canvas.restore();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawForeground(org.ebookdroid.core.EventGLDraw)
     */
    @Override
    protected void drawForeground(final EventGLDraw event) {
        final GLCanvas canvas = event.canvas;

        Page page = event.viewState.model.getPageObject(foreIndex);
        if (page == null) {
            page = event.viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            canvas.save();
            event.process(page);
            canvas.restore();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawExtraObjects(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawExtraObjects(final EventGLDraw event) {

        if (AppSettings.current().showAnimIcon) {
            DragMark.CURLER.draw(event.canvas, event.viewState);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#onFirstDrawEvent(android.graphics.Canvas,
     *      org.ebookdroid.core.ViewState)
     */
    @Override
    protected void onFirstDrawEvent(final Canvas canvas, final ViewState viewState) {
        resetClipEdge();

        lock.writeLock().lock();
        try {
            updateValues();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#onFirstDrawEvent(org.emdev.ui.gl.GLCanvas,
     *      org.ebookdroid.core.ViewState)
     */
    @Override
    protected void onFirstDrawEvent(final GLCanvas canvas, final ViewState viewState) {
        resetClipEdge();

        lock.writeLock().lock();
        try {
            updateValues();
        } finally {
            lock.writeLock().unlock();
        }
    }

}
