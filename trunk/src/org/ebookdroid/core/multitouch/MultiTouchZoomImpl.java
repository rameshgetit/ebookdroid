package org.ebookdroid.core.multitouch;

import org.ebookdroid.core.models.ZoomModel;

import android.view.MotionEvent;

public class MultiTouchZoomImpl implements MultiTouchZoom {

    private final ZoomModel zoomModel;
    private boolean resetLastPointAfterZoom;
    private float lastZoomDistance;
    private boolean multiEventCatched;

    public MultiTouchZoomImpl(final ZoomModel zoomModel) {
        this.zoomModel = zoomModel;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        if ((ev.getAction() & MotionEvent.ACTION_POINTER_DOWN) == MotionEvent.ACTION_POINTER_DOWN) {
            lastZoomDistance = getZoomDistance(ev);
            multiEventCatched = true;
            return true;
        }
        if ((ev.getAction() & MotionEvent.ACTION_POINTER_UP) == MotionEvent.ACTION_POINTER_UP) {
            lastZoomDistance = 0;
            zoomModel.commit();
            resetLastPointAfterZoom = true;
            multiEventCatched = true;
            return true;
        }
        if (ev.getAction() == MotionEvent.ACTION_MOVE && lastZoomDistance != 0) {
            final float zoomDistance = getZoomDistance(ev);
            zoomModel.setZoom(zoomModel.getZoom() * zoomDistance / lastZoomDistance);
            lastZoomDistance = zoomDistance;
            multiEventCatched = true;
            return true;
        }
        if (ev.getAction() == MotionEvent.ACTION_UP && multiEventCatched) {
            multiEventCatched = false;
            return true;
        }
        if (ev.getAction() == MotionEvent.ACTION_MOVE && multiEventCatched) {
            return true;
        }
        return false;
    }

    private float getZoomDistance(final MotionEvent ev) {
        return (float) Math.sqrt(Math.pow(ev.getX(0) - ev.getX(1), 2) + Math.pow(ev.getY(0) - ev.getY(1), 2));
    }

    @Override
    public boolean isResetLastPointAfterZoom() {
        return resetLastPointAfterZoom;
    }

    @Override
    public void setResetLastPointAfterZoom(final boolean resetLastPointAfterZoom) {
        this.resetLastPointAfterZoom = resetLastPointAfterZoom;
    }
}
