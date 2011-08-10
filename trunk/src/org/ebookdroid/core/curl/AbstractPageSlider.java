package org.ebookdroid.core.curl;

import org.ebookdroid.R;
import org.ebookdroid.core.SinglePageDocumentView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;


public abstract class AbstractPageSlider extends AbstractPageAnimator {

    protected Bitmap bitmap;
    protected Bitmap arrowsBitmap;

    
    public AbstractPageSlider(SinglePageDocumentView singlePageDocumentView) {
        super(singlePageDocumentView);
    }

    /**
     * Initialize specific value for the view
     */
    @Override
    public void init() {
        super.init();
        mInitialEdgeOffset = 0;
        arrowsBitmap = BitmapFactory.decodeResource(view.getBase().getContext().getResources(), R.drawable.arrows);
    }

    /**
     * Called on the first draw event of the view
     * 
     * @param canvas
     */
    protected void onFirstDrawEvent(final Canvas canvas) {
        resetClipEdge();
        updateValues();
    }

    /**
     * Reset points to it's initial clip edge state
     */
    protected void resetClipEdge() {
        // Set our base movement
        mMovement.x = mInitialEdgeOffset;
        mMovement.y = mInitialEdgeOffset;
        mOldMovement.x = 0;
        mOldMovement.y = 0;

        // Now set the points
        mA = new Vector2D(mInitialEdgeOffset, 0);
    }

    /**
     * Do the page curl depending on the methods we are using
     */
    protected void updateValues() {
        // Calculate point A
        mA.x = mMovement.x;
        mA.y = 0;
    }

    protected Bitmap getBitmap(final Canvas canvas) {
        if (bitmap == null || bitmap.getWidth() != canvas.getWidth() || bitmap.getHeight() != canvas.getHeight()) {
            if (bitmap != null) {
                bitmap.recycle();
            }
            bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.RGB_565);
        }
        return bitmap;
    }

    @Override
    protected void drawExtraObjects(Canvas canvas) {
        final Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        paint.setDither(true);
        canvas.drawBitmap(arrowsBitmap, view.getWidth() - arrowsBitmap.getWidth(), view.getHeight() - arrowsBitmap.getHeight(), paint);
    }

    @Override
    protected Vector2D fixMovement(Vector2D movement, boolean bMaintainMoveDir) {
        return movement;
    }
    
}
