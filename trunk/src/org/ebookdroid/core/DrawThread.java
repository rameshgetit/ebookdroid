package org.ebookdroid.core;

import org.ebookdroid.core.log.LogContext;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DrawThread extends Thread {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Imaging");

    private SurfaceHolder surfaceHolder;

    private AbstractDocumentView view;

    private final BlockingQueue<DrawTask> queue = new ArrayBlockingQueue<DrawThread.DrawTask>(16, true);

    public DrawThread(SurfaceHolder surfaceHolder, AbstractDocumentView view) {
        this.surfaceHolder = surfaceHolder;
        this.view = view;
    }

    public void finish() {
        queue.add(new DrawTask(null));
        try {
            this.join();
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void run() {
        Canvas canvas;
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);

        while (true) {
            DrawTask task = takeTask();
            if (task == null) {
                continue;
            }
            if (task.viewRect == null) {
                break;
            }
            canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas(null);
                performDrawing(canvas, task);
            } catch (Throwable th) {
                LCTX.e("Unexpected error on drawing: " + th.getMessage(), th);
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private DrawTask takeTask() {
        DrawTask task = null;
        try {
            task = queue.poll(1, TimeUnit.SECONDS);
            if (task != null) {
                ArrayList<DrawTask> list = new ArrayList<DrawTask>();
                if (queue.drainTo(list) > 0) {
                    task = list.get(list.size() - 1);
                }
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        return task;
    }

    private void performDrawing(Canvas canvas, DrawTask task) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawRect(canvas.getClipBounds(), paint);
        view.drawView(canvas, task.viewRect);
    }

    public void draw(RectF viewRect) {
        if (viewRect != null) {
            queue.offer(new DrawTask(viewRect));
        }
    }

    private static class DrawTask {

        final RectF viewRect;

        public DrawTask(RectF viewRect) {
            this.viewRect = viewRect != null ? new RectF(viewRect) : null;
        }
    }
}
