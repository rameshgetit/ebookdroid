package org.ebookdroid.core;

import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.utils.CompareUtils;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

public class ContiniousDocumentView extends AbstractDocumentView {

    public ContiniousDocumentView(final IViewerActivity base) {
        super(base);
    }

    @Override
    protected void goToPageImpl(final int toPage) {
        final DocumentModel dm = getBase().getDocumentModel();
        if (toPage >= 0 && toPage <= dm.getPageCount()) {
            final Page pageObject = dm.getPageObject(toPage);
            if (pageObject != null) {
                final RectF viewRect = this.getViewRect();
                final RectF bounds = pageObject.getBounds();
                scrollTo(0, pageObject.getTop() - ((int) viewRect.height() - (int) bounds.height()) / 2);
            }
        }

    }

    public void setCurrentPageByFirstVisible() {
        final DocumentModel dm = getBase().getDocumentModel();
        final int index = getCurrentPage();
        final Page page = dm.getPageObject(index);
        post(new Runnable() {
            public void run() {
                dm.setCurrentPageIndex(page.getDocumentPageIndex(), page.getIndex());
            }
        });
    }

    @Override
    public int getCurrentPage() {
        int result = 0;
        long bestDistance = Long.MAX_VALUE;

        final RectF viewRect = getViewRect();
        final int viewY = Math.round((viewRect.top + viewRect.bottom) / 2);

        boolean foundVisible = false;
        for (final Page page : getBase().getDocumentModel().getPages()) {
            if (page.isVisible()) {
                foundVisible = true;
                final RectF bounds = page.getBounds();
                final int pageY = Math.round((bounds.top + bounds.bottom) / 2);
                final long dist = Math.abs(pageY - viewY);
                if (dist < bestDistance) {
                    bestDistance = dist;
                    result = page.getIndex();
                }
            } else {
                if (foundVisible) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public int compare(final PageTreeNode node1, final PageTreeNode node2) {
        final RectF viewRect = getViewRect();
        final Rect rect1 = node1.getTargetRect(viewRect, node1.page.getBounds());
        final Rect rect2 = node2.getTargetRect(viewRect, node2.page.getBounds());

        final int cp = getCurrentPage();

        if (node1.page.index == cp && node2.page.index == cp) {
            int res = CompareUtils.compare(rect1.top, rect2.top);
            if (res == 0) {
                res = CompareUtils.compare(rect1.left, rect2.left);
            }
            return res;
        }

        final long centerX = ((long) viewRect.left + (long) viewRect.right) / 2;
        final long centerY = ((long) viewRect.top + (long) viewRect.bottom) / 2;

        final long centerX1 = ((long) rect1.left + (long) rect1.right) / 2;
        final long centerY1 = ((long) rect1.top + (long) rect1.bottom) / 2;

        final long centerX2 = ((long) rect2.left + (long) rect2.right) / 2;
        final long centerY2 = ((long) rect2.top + (long) rect2.bottom) / 2;

        final long dist1 = (centerX1 - centerX) * (centerX1 - centerX) + (centerY1 - centerY) * (centerY1 - centerY);
        final long dist2 = (centerX2 - centerX) * (centerX2 - centerX) + (centerY2 - centerY) * (centerY2 - centerY);

        return CompareUtils.compare(dist1, dist2);
    }

    protected void onScrollChanged() {
        super.onScrollChanged();
        setCurrentPageByFirstVisible();
        redrawView();
    }

    @Override
    protected void verticalConfigScroll(final int direction) {
        final int scrollheight = getBase().getAppSettings().getScrollHeight();
        getScroller().startScroll(getScrollX(), getScrollY(), 0,
                (int) (direction * getHeight() * (scrollheight / 100.0)));

        redrawView();
    }

    @Override
    protected void verticalDpadScroll(final int direction) {
        getScroller().startScroll(getScrollX(), getScrollY(), 0, direction * getHeight() / 2);

        redrawView();
    }

    @Override
    protected int getTopLimit() {
        return 0;
    }

    @Override
    protected int getLeftLimit() {
        return 0;
    }

    @Override
    protected int getBottomLimit() {
        return (int) getBase().getDocumentModel().getLastPageObject().getBounds().bottom - getHeight();
    }

    @Override
    protected int getRightLimit() {
        return (int) (getWidth() * getBase().getZoomModel().getZoom()) - getWidth();
    }

    @Override
    public void drawView(final Canvas canvas, RectF viewRect) {
        DocumentModel dm = getBase().getDocumentModel();
        for (int i = firstVisiblePage; i <= lastVisiblePage; i++) {
            final Page page = dm.getPageObject(i);
            if (page != null) {
                page.draw(canvas, viewRect);
            }
        }
        setCurrentPageByFirstVisible();
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        int page = -1;
        if (changed) {
            page = getCurrentPage();
        }
        super.onLayout(changed, left, top, right, bottom);

        invalidatePageSizes();
        invalidateScroll();
        commitZoom();
        if (page > 0) {
            goToPage(page);
        }
    }

    /**
     * Invalidate page sizes.
     */
    @Override
    public void invalidatePageSizes() {
        if (!isInitialized()) {
            return;
        }
        float heightAccum = 0;

        final int width = getWidth();
        final float zoom = getBase().getZoomModel().getZoom();

        for (final Page page : getBase().getDocumentModel().getPages()) {
            final float pageHeight = page.getPageHeight(width, zoom);
            page.setBounds(new RectF(0, heightAccum, width * zoom, heightAccum + pageHeight));
            heightAccum += pageHeight;
        }
    }

    @Override
    protected boolean isPageVisibleImpl(final Page page) {
        return RectF.intersects(getViewRect(), page.getBounds());
    }

    @Override
    public void updateAnimationType() {
        // This mode do not use animation

    }
}
