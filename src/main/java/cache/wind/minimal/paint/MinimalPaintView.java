package cache.wind.minimal.paint;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinimalPaintView extends View {
    private static final float TOUCH_TOLERANCE = 4;
    private static final int MAX_POINTERS = 10;

    private final Bitmap mBitmap;
    private final Canvas mCanvas;
    private final Paint mBitmapPaint;
    private final MultiLinePathManager multiLinePathManager;

    private final List<Path> mSavePaths;
    private final List<Path> mDeletePaths;
    private final Map<Path, Paint> mPathPaintMap;

    private Paint mPaint;

    public void setPaint(Paint paint) {
        this.mPaint = paint;
    }

    private class LinePath extends Path {
        private Integer idPointer;
        private float lastX;
        private float lastY;

        LinePath() {
            this.idPointer = null;
        }

        public float getLastX() {
            return lastX;
        }

        public float getLastY() {
            return lastY;
        }

        public void touchStart(float x, float y) {
            this.reset();
            this.moveTo(x, y);
            this.lastX = x;
            this.lastY = y;
        }

        public void touchMove(float x, float y) {
            float dx = Math.abs(x - lastX);
            float dy = Math.abs(y - lastY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                this.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
                lastX = x;
                lastY = y;
            }
        }

        public boolean isDisassociatedFromPointer() {
            return idPointer == null;
        }

        public boolean isAssociatedToPointer(int idPointer) {
            return this.idPointer != null
                    && this.idPointer == idPointer;
        }

        public void disassociateFromPointer() {
            idPointer = null;
        }

        public void associateToPointer(int idPointer) {
            this.idPointer = idPointer;
        }
    }

    private class MultiLinePathManager {
        public final LinePath[] superMultiPaths;

        MultiLinePathManager(int maxPointers) {
            superMultiPaths = new LinePath[maxPointers];
            for (int i = 0; i < maxPointers; i++) {
                superMultiPaths[i] = new LinePath();
            }
        }

        public LinePath findLinePathFromPointer(int idPointer) {
            for (LinePath path : superMultiPaths) {
                if (path.isAssociatedToPointer(idPointer)) {
                    return path;
                }
            }
            return null;
        }

        public LinePath addLinePathWithPointer(int idPointer) {
            for (LinePath path : superMultiPaths) {
                if (path.isDisassociatedFromPointer()) {
                    path.associateToPointer(idPointer);
                    return path;
                }
            }
            return null;
        }
    }

    public void reset() {
        mCanvas.drawColor(Color.WHITE);
        invalidate();

        mSavePaths.clear();
        mDeletePaths.clear();
        mPathPaintMap.clear();
    }

    public void undo() {
        mDeletePaths.add(mSavePaths.remove(mSavePaths.size() - 1));
        mCanvas.drawColor(Color.WHITE);
        for (Path path : mSavePaths) {
            mCanvas.drawPath(path, mPathPaintMap.get(path));
        }
        invalidate();
    }

    public void redo() {
        mSavePaths.add(mDeletePaths.remove(mDeletePaths.size() - 1));
        mCanvas.drawColor(Color.WHITE);
        for (Path path : mSavePaths) {
            mCanvas.drawPath(path, mPathPaintMap.get(path));
        }
        invalidate();
    }

    public boolean hasUndo() {
        return !mSavePaths.isEmpty();
    }

    public boolean hasRedo() {
        return !mDeletePaths.isEmpty();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public MinimalPaintView(Context c) {
        super(c);

        setId(R.id.CanvasId);
        Display display = ((WindowManager) c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            display.getSize(size);
            mBitmap = Bitmap.createBitmap(size.x, size.y,
                    Bitmap.Config.ARGB_8888);
        } else {
            //noinspection deprecation
            mBitmap = Bitmap.createBitmap(display.getWidth(), display.getHeight(),
                    Bitmap.Config.ARGB_8888);
        }
        mCanvas = new Canvas(mBitmap);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        multiLinePathManager = new MultiLinePathManager(MAX_POINTERS);

        mSavePaths = new ArrayList<>();
        mDeletePaths = new ArrayList<>();
        mPathPaintMap = new HashMap<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        for (int i = 0; i < multiLinePathManager.superMultiPaths.length; i++) {
            canvas.drawPath(multiLinePathManager.superMultiPaths[i], mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        LinePath linePath;
        int index;
        int id;
        int eventMasked = event.getActionMasked();
        switch (eventMasked) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                index = event.getActionIndex();
                id = event.getPointerId(index);
                linePath = multiLinePathManager.addLinePathWithPointer(id);
                if (linePath != null) {
                    linePath.touchStart(event.getX(index), event.getY(index));
                    mDeletePaths.clear();
                } else {
                    Log.e("anupam", "Too many fingers!");
                }
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    id = event.getPointerId(i);
                    index = event.findPointerIndex(id);
                    linePath = multiLinePathManager.findLinePathFromPointer(id);
                    if (linePath != null) {
                        linePath.touchMove(event.getX(index), event.getY(index));
                        mDeletePaths.clear();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                index = event.getActionIndex();
                id = event.getPointerId(index);
                linePath = multiLinePathManager.findLinePathFromPointer(id);
                if (linePath != null) {
                    linePath.lineTo(linePath.getLastX(), linePath.getLastY());

                    Path path = new Path(linePath);
                    mSavePaths.add(path);
                    mPathPaintMap.put(path, new Paint(mPaint));

                    // Commit the path to our offscreen
                    mCanvas.drawPath(linePath, mPaint);

                    // Kill this so we don't double draw
                    linePath.reset();

                    // Allow this LinePath to be associated to another idPointer
                    linePath.disassociateFromPointer();
                }
                break;
        }
        invalidate();
        return true;
    }
}
