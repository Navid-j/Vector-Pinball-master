package com.taak.pinball;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.math.MathUtils;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * This class handles the common functionality for Canvas and OpenGL-based views, including mapping
 * world coordinates to pixels and handling touch events.
 */

public class FieldViewManager implements SurfaceHolder.Callback {

    IFieldRenderer view;
    boolean canDraw;
    Runnable startGameAction;

    public void setFieldView(IFieldRenderer view) {
        if (this.view != view) {
            this.view = view;
            view.setManager(this);
            if (view instanceof SurfaceView) {
                canDraw = false;
                ((SurfaceView) view).getHolder().addCallback(this);
            }
            else {
                canDraw = true;
            }
        }
    }

    public boolean canDraw() {
        return canDraw;
    }

    boolean highQuality;
    boolean independentFlippers;

    Field field;

    float maxZoom = 1.0f;

    // x/y offsets and scale are cached at the beginning of draw(), to avoid repeated calls as
    // elements are drawn.
    private float cachedXOffset, cachedYOffset, cachedScale, cachedHeight;

    public void setField(Field value) {
        field = value;
    }

    public Field getField() {
        return field;
    }

    public void setIndependentFlippers(boolean value) {
        independentFlippers = value;
    }

    public void setHighQuality(boolean value) {
        highQuality = value;
    }

    public boolean isHighQuality() {
        return highQuality;
    }

    public void setStartGameAction(Runnable action) {
        startGameAction = action;
    }

    float getScale(float zoom) {
        float xs = view.getWidth() / field.getWidth();
        float ys = view.getHeight() / field.getHeight();
        return Math.min(xs, ys) * zoom;
    }

    float getCachedScale() {
        return cachedScale;
    }

    // Sets maxZoom ivar, zoom will still be 1 when game is not in progress.
    public void setZoom(float value) {
        maxZoom = value;
    }

    /**
     * Saves scale and x and y offsets for use by world2pixel methods, avoiding repeated method
     * calls and math operations.
     */
    private void cacheScaleAndOffsets() {
        cachedHeight = view.getHeight();
        // Don't zoom if game is over or multiball is active.
        List<Ball> balls = field.getBalls();
        if (maxZoom <= 1.0f || !field.getGameState().isGameInProgress() || balls.size() > 1) {
            cachedScale = getScale(1.0f);
            // Center the entire table horizontally and put at at the top vertically.
            // Negative offsets so the 0 coordinate will be on the screen.
            float spaceLeftX = view.getWidth() - (field.getWidth() * cachedScale);
            cachedXOffset = (spaceLeftX > 0) ? -spaceLeftX / (2 * cachedScale) : 0;
            float spaceLeftY = view.getHeight() - (field.getHeight() * cachedScale);
            cachedYOffset = (spaceLeftY > 0) ? -spaceLeftY / cachedScale : 0;
        }
        else {
            cachedScale = getScale(maxZoom);
            // Center the zoomed view on the ball if available, or the launch position if not.
            float centerX, centerY;
            if (balls.size() == 1) {
                Ball b = balls.get(0);
                centerX = b.getPosition().x;
                centerY = b.getPosition().y;
            }
            else {
                List<Float> position = field.layout.getLaunchPosition();
                centerX = position.get(0);
                centerY = position.get(1);
            }
            // `spanX` and `spanY` are how many world units are visible when zoomed. We don't want
            // the zoomed view to extend to less than 0, or greater than the field size.
            float spanX = view.getWidth() / cachedScale;
            float rawXOffset = centerX - spanX / 2;
            float maxXOffset = field.getWidth() - spanX;
            cachedXOffset = MathUtils.clamp(rawXOffset, 0, maxXOffset);

            float spanY = view.getHeight() / cachedScale;
            float rawYOffset = centerY - spanY / 2;
            float maxYOffset = field.getHeight() - spanY;
            cachedYOffset = MathUtils.clamp(rawYOffset, 0, maxYOffset);
        }
    }

    // world2pixel methods assume cacheScaleAndOffsets has been called previously.

    /** Converts an x coordinate from world coordinates to the view's pixel coordinates. */
    public float world2pixelX(float x) {
        return (x - cachedXOffset) * cachedScale;
    }

    /**
     * Converts a y coordinate from world coordinates to the view's pixel coordinates.
     * In world coordinates, positive y is up, in pixel coordinates, positive y is down.
     */
    public float world2pixelY(float y) {
        return cachedHeight - ((y - cachedYOffset) * cachedScale);
    }

    // For compatibility with Android 1.6, use reflection for multitouch features.
    boolean hasMultitouch;
    Method getPointerCountMethod;
    Method getXMethod;
    int MOTIONEVENT_ACTION_MASK = 0xffffffff; // Defaults to no-op AND mask.
    int MOTIONEVENT_ACTION_POINTER_UP;
    int MOTIONEVENT_ACTION_POINTER_INDEX_MASK;
    int MOTIONEVENT_ACTION_POINTER_INDEX_SHIFT;

    {
        try {
            getPointerCountMethod = MotionEvent.class.getMethod("getPointerCount");
            getXMethod = MotionEvent.class.getMethod("getX", int.class);
            MOTIONEVENT_ACTION_MASK = MotionEvent.class.getField("ACTION_MASK").getInt(null);
            MOTIONEVENT_ACTION_POINTER_UP =
                    MotionEvent.class.getField("ACTION_POINTER_UP").getInt(null);
            MOTIONEVENT_ACTION_POINTER_INDEX_MASK =
                    MotionEvent.class.getField("ACTION_POINTER_INDEX_MASK").getInt(null);
            MOTIONEVENT_ACTION_POINTER_INDEX_SHIFT =
                    MotionEvent.class.getField("ACTION_POINTER_INDEX_SHIFT").getInt(null);
            hasMultitouch = true;
        }
        catch (Exception ex) {
            hasMultitouch = false;
        }
    }

    void launchBallIfNeeded() {
        // Remove "dead" balls and launch if none already in play.
        field.removeDeadBalls();
        if (field.getBalls().size() == 0) field.launchBall();
    }

    /**
     * Called when the view is touched. Activates flippers, starts a new game if one is not in
     * progress, and launches a ball if one is not in play.
     */
    public boolean handleTouchEvent(MotionEvent event) {
        int actionType = event.getAction() & MOTIONEVENT_ACTION_MASK;
        synchronized (field) {
            if (!field.getGameState().isGameInProgress() || field.getGameState().isPaused()) {
                if (startGameAction != null) {
                    startGameAction.run();
                    return true;
                }
            }
            // activate or deactivate flippers
            boolean left = false, right = false;
            if (this.independentFlippers && this.hasMultitouch) {
                try {
                    // Determine whether to activate left and/or right flippers,
                    // using reflection for Android 2.2 multitouch APIs.
                    if (actionType != MotionEvent.ACTION_UP) {
                        int npointers = (Integer) getPointerCountMethod.invoke(event);
                        // If pointer was lifted (ACTION_POINTER_UP), get its index so we don't
                        // count it as pressed.
                        int liftedPointerIndex = -1;
                        if (actionType == MOTIONEVENT_ACTION_POINTER_UP) {
                            liftedPointerIndex =
                                    (event.getAction() & MOTIONEVENT_ACTION_POINTER_INDEX_MASK) >>
                                            MOTIONEVENT_ACTION_POINTER_INDEX_SHIFT;
                        }
                        float halfwidth = view.getWidth() / 2;
                        for (int i = 0; i < npointers; i++) {
                            if (i != liftedPointerIndex) {
                                float touchx = (Float) getXMethod.invoke(event, i);
                                if (touchx < halfwidth) left = true;
                                else right = true;
                            }
                        }
                    }
                }
                catch (Exception ignored) {
                }
            }
            else {
                left = right = !(event.getAction() == MotionEvent.ACTION_UP);
            }
            if (actionType == MotionEvent.ACTION_DOWN) {
                launchBallIfNeeded();
            }
            // Treat both active separately because setAllFlippersEngaged will cycle the rollovers,
            // as opposed to separate calls to set(Left|Right)FlippersEngaged which would result in
            // cycling one way and then immediately back the other, for no net change.
            if (left && right) {
                field.setAllFlippersEngaged(true);
            }
            else {
                field.setLeftFlippersEngaged(left);
                field.setRightFlippersEngaged(right);
            }
        }
        return true;
    }

    static List<Integer> LEFT_FLIPPER_KEYS = Arrays.asList(
            KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_DPAD_LEFT);
    static List<Integer> RIGHT_FLIPPER_KEYS = Arrays.asList(
            KeyEvent.KEYCODE_SLASH, KeyEvent.KEYCODE_DPAD_RIGHT);
    static List<Integer> ALL_FLIPPER_KEYS = Arrays.asList(
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER);

    public boolean handleKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            synchronized (field) {
                // Don't let a pressed flipper key start a game, but do launch a ball if needed.
                if (!field.getGameState().isGameInProgress() || field.getGameState().isPaused()) {
                    return false;
                }
                boolean isActionKey = updateFlippersForKeyCode(keyCode, true);
                if (isActionKey) launchBallIfNeeded();
                return isActionKey;
            }
        }
        return false;
    }

    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            synchronized (field) {
                if (!field.getGameState().isGameInProgress() || field.getGameState().isPaused()) {
                    return false;
                }
                return updateFlippersForKeyCode(keyCode, false);
            }
        }
        return false;
    }

    private boolean updateFlippersForKeyCode(int keyCode, boolean isPressed) {
        if (LEFT_FLIPPER_KEYS.contains(keyCode)) {
            field.setLeftFlippersEngaged(isPressed);
            return true;
        }
        if (RIGHT_FLIPPER_KEYS.contains(keyCode)) {
            field.setRightFlippersEngaged(isPressed);
            return true;
        }
        if (ALL_FLIPPER_KEYS.contains(keyCode)) {
            field.setAllFlippersEngaged(isPressed);
            return true;
        }
        return false;
    }

    public void draw() {
        cacheScaleAndOffsets();
        view.doDraw();
    }

    // SurfaceHolder.Callback methods
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Not used.
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        canDraw = true;
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        canDraw = false;
    }
}
