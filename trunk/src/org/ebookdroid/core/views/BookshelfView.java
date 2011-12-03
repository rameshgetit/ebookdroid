/*
 * Copyright (C) 2008 Romain Guy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ebookdroid.core.views;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.presentation.BooksAdapter;
import org.ebookdroid.core.presentation.BooksAdapter.BookShelfAdapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import java.io.File;

public class BookshelfView extends GridView implements OnItemClickListener {

    private Bitmap mShelfBackground;
    private Bitmap mShelfBackgroundLeft;
    private Bitmap mShelfBackgroundRight;
    private int mShelfWidth;
    private int mShelfHeight;

    private Bitmap mWebLeft;
    private Bitmap mWebRight;
    private int mWebRightWidth;

    private IBrowserActivity base;
    private BookShelfAdapter adapter;
    private Bookshelves shelves;
    String path;

    public BookshelfView(IBrowserActivity base, Bookshelves shelves, BookShelfAdapter adapter) {
        super(base.getContext());
        this.base = base;
        this.adapter = adapter;
        this.shelves = shelves;
        this.path = adapter.getPath();
        setCacheColorHint(0);
        setSelector(android.R.color.transparent);
        setNumColumns(AUTO_FIT);
        setStretchMode(STRETCH_SPACING);
        setAdapter(adapter);
        setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160, r.getDisplayMetrics());
        setColumnWidth((int) px);

        init(base.getContext());
        setOnItemClickListener(this);

    }

    private void init(Context context) {
        final Bitmap shelfBackground = BitmapManager.getResource(R.drawable.shelf_panel1);
        if (shelfBackground != null) {
            mShelfWidth = shelfBackground.getWidth();
            mShelfHeight = shelfBackground.getHeight();
            mShelfBackground = shelfBackground;
        }

        mShelfBackgroundLeft = BitmapManager.getResource(R.drawable.shelf_panel1_left);
        mShelfBackgroundRight = BitmapManager.getResource(R.drawable.shelf_panel1_right);

        mWebLeft = BitmapManager.getResource(R.drawable.web_left);

        final Bitmap webRight = BitmapManager.getResource(R.drawable.web_right);
        mWebRightWidth = webRight.getWidth();
        mWebRight = webRight;

        StateListDrawable drawable = new StateListDrawable();

        SpotlightDrawable start = new SpotlightDrawable(context, this);
        start.disableOffset();
        SpotlightDrawable end = new SpotlightDrawable(context, this, R.drawable.spotlight_blue);
        end.disableOffset();
        TransitionDrawable transition = new TransitionDrawable(start, end);
        drawable.addState(new int[] { android.R.attr.state_pressed }, transition);

        final SpotlightDrawable normal = new SpotlightDrawable(context, this);
        drawable.addState(new int[] {}, normal);

        normal.setParent(drawable);
        transition.setParent(drawable);

        setSelector(drawable);
        setDrawSelectorOnTop(false);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        final int count = getChildCount();
        int top = count > 0 ? getChildAt(0).getTop() : 0;
        final int shelfWidth = mShelfWidth;
        final int shelfHeight = mShelfHeight;
        final int width = getWidth();
        final int height = getHeight();
        
        for (int y = top; y < height; y += shelfHeight) {
            for (int x = 0; x < width; x += shelfWidth) {
                canvas.drawBitmap(mShelfBackground, x, y, null);
            }
            canvas.drawBitmap(mShelfBackgroundLeft, 0, y, null);
            canvas.drawBitmap(mShelfBackgroundRight, width - 15, y, null);
        }

        
        top = (count > 0) ? getChildAt(count - 1).getTop() + shelfHeight : 0;
        canvas.drawBitmap(mWebLeft, 15, top + 1, null);
        canvas.drawBitmap(mWebRight, width - mWebRightWidth - 15, top + shelfHeight + 1, null);

        super.dispatchDraw(canvas);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BooksAdapter.Node node = (BooksAdapter.Node) adapter.getItem(position);
        File file = new File(node.getPath());
        if (!file.isDirectory()) {
            base.showDocument(Uri.fromFile(file));
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            Thread.sleep(16);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }

        if (shelves.onTouchEvent(ev)) {
            return true;
        }
        return super.onTouchEvent(ev);
    }

}
