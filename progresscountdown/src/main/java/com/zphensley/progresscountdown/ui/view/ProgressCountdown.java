package com.zphensley.progresscountdown.ui.view;

import com.zphensley.progresscountdown.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class ProgressCountdown extends View {

    // Attributes
    private long mCountdownDuration = 30;                    // in seconds
    private int mCountdownTextColor = Color.WHITE;
    private int mCountdownForegroundColor = Color.BLACK;
    private int mCountdownBackgroundColor = Color.WHITE;
    private float mCountdownTextSize = 22.0f;               // in pixels
    private float mCountdownCircleStroke = 14.0f;           // in pixels

    // Timer variables
    private long currentProgress = 0;
    private long startTime = 0;
    private long pauseTime = 0;
    private boolean countingDown = false;
    private boolean countdownPaused = false;

    // Our callback
    private CountdownCallback mCallback;

    // Drawing variables
    private Paint mPaint;
    private Paint mBackgroundPaint;
    private Paint mTextPaint;
    private RectF boundingRect = new RectF();
    private RectF arcRect = new RectF();

    public ProgressCountdown(Context context) {
        super(context);
        init(null, 0);
    }

    public ProgressCountdown(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ProgressCountdown(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.CountdownProgress, defStyle, 0);

        if (a.hasValue(R.styleable.CountdownProgress_countdown_duration)) {

            mCountdownDuration = a.getInteger(R.styleable.CountdownProgress_countdown_duration, 30);
        }

        if (a.hasValue(R.styleable.CountdownProgress_countdown_foreground_color)) {

            mCountdownForegroundColor = a.getColor(R.styleable.CountdownProgress_countdown_foreground_color, Color.BLACK);
        }

        if (a.hasValue(R.styleable.CountdownProgress_countdown_background_color)) {

            mCountdownBackgroundColor = a.getColor(R.styleable.CountdownProgress_countdown_background_color, Color.WHITE);
        }

        if (a.hasValue(R.styleable.CountdownProgress_countdown_text_color)) {

            mCountdownTextColor = a.getColor(R.styleable.CountdownProgress_countdown_text_color, Color.WHITE);
        }

        if (a.hasValue(R.styleable.CountdownProgress_countdown_text_size)) {

            mCountdownTextSize = a.getDimensionPixelSize(R.styleable.CountdownProgress_countdown_text_size, 22);
        }

        if (a.hasValue(R.styleable.CountdownProgress_countdown_circle_stroke)) {

            mCountdownCircleStroke = a.getDimension(R.styleable.CountdownProgress_countdown_circle_stroke, 14.0f);
        }

        a.recycle();

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.BUTT);
        mPaint.setStrokeWidth(mCountdownCircleStroke);
        mPaint.setColor(mCountdownForegroundColor);
        mPaint.setAntiAlias(true);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setStyle(Paint.Style.STROKE);
        mBackgroundPaint.setStrokeWidth(mCountdownCircleStroke);
        mBackgroundPaint.setColor(mCountdownBackgroundColor);
        mBackgroundPaint.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setColor(mCountdownTextColor);
        mTextPaint.setTextSize(mCountdownTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setStrokeWidth(1.0f);

        setWillNotDraw(false);
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        ViewGroup.LayoutParams lp = getLayoutParams();
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        int minSize = (int) (50 * getContext().getResources().getDisplayMetrics().density);


        // We can't wrap our content because we don't know our content until we draw (so set a minimum)
        if(lp.width == ViewGroup.LayoutParams.WRAP_CONTENT) { width = minSize; }
        if(lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) { height = minSize; }

        if(height > width) { width = height; }
        if(width > height) { height = width; }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        boundingRect.set(0, 0, w, h);
        super.onSizeChanged(w, h, oldw, oldh);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        if(isInEditMode()) {

            float radius = (boundingRect.width() - mCountdownCircleStroke) / 2;
            canvas.drawCircle(boundingRect.width() / 2, boundingRect.height() / 2, radius, mBackgroundPaint);

            arcRect.set(boundingRect.left + (mCountdownCircleStroke / 2), boundingRect.top + (mCountdownCircleStroke / 2), boundingRect.right - (mCountdownCircleStroke / 2), boundingRect.bottom - (mCountdownCircleStroke / 2));
            canvas.drawArc(arcRect, 0.0f, 90.0f, false, mPaint);

            int xPos = (int) (boundingRect.width() / 2);
            int yPos = (int) ((boundingRect.height() / 2) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2)) ;
            canvas.drawText(mCountdownDuration + "", xPos, yPos, mTextPaint);

            return;
        }

        currentProgress = ((mCountdownDuration * 1000) - (System.currentTimeMillis() - startTime)) / 1000;


        if(countdownPaused) {

            drawProgress(canvas, currentProgress);
            return;
        }

        if(countingDown && currentProgress >= 0) {

            drawProgress(canvas, currentProgress);
            invalidate();
        }
        else if(countingDown) {

            countingDown = false;
            drawProgress(canvas, (currentProgress < 0 ? 0 : currentProgress));

            if(mCallback != null) {

                mCallback.onCountdownFinished();
            }
        }
        else {

            // if not counting down, but current progress is still valid (should only happen when first started)
            startTime = System.currentTimeMillis();
            currentProgress = ((mCountdownDuration * 1000) - (System.currentTimeMillis() - startTime)) / 1000;
            drawProgress(canvas, (currentProgress < 0 ? 0 : currentProgress));
        }
    }

    private void drawProgress(Canvas canvas, long currentProgress) {

        float angle = 360.0f - (((float) currentProgress / (float) mCountdownDuration) * 360.0f);
        float radius = (boundingRect.width() - mCountdownCircleStroke) / 2;
        canvas.drawCircle(boundingRect.width() / 2, boundingRect.height() / 2, radius, mBackgroundPaint);
        arcRect.set(boundingRect.left + (mCountdownCircleStroke / 2), boundingRect.top + (mCountdownCircleStroke / 2), boundingRect.right - (mCountdownCircleStroke / 2), boundingRect.bottom - (mCountdownCircleStroke / 2));
        canvas.drawArc(arcRect, 0.0f, angle, false, mPaint);

        int xPos = (int) (boundingRect.width() / 2);
        int yPos = (int) ((boundingRect.height() / 2) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2)) ;
        canvas.drawText(currentProgress + "", xPos, yPos, mTextPaint);
    }

    public long getCountdownDuration() {

        return mCountdownDuration;
    }

    public void setCountdownDuration(long countdownDuration) {

        mCountdownDuration = countdownDuration;
        invalidate();
    }

    public long getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(long currentProgress) {

        long difference = currentProgress - this.currentProgress;
        startTime = startTime + (difference * 1000);

        this.currentProgress = currentProgress;

        if(this.currentProgress > mCountdownDuration) {

            startTime = System.currentTimeMillis();
            mCountdownDuration = this.currentProgress;
        }

        invalidate();
    }

    public float getCountdownCircleStroke() {

        return mCountdownCircleStroke;
    }

    public void setCountdownCircleStroke(float countdownCircleStroke) {

        mCountdownCircleStroke = countdownCircleStroke;
        invalidate();
    }

    public void startCountdown() {

        if(countingDown) { return; }

        countingDown = true;
        startTime = System.currentTimeMillis();

        invalidate();

        if(mCallback != null) {

            mCallback.onCountdownStarted();
        }
    }

    public void stopCountdown() {

        if(!countingDown) { return; }

        countingDown = false;

        invalidate();

        if(mCallback != null) {

            mCallback.onCountdownStopped();
        }
    }

    public void pauseCountdown() {

        if(!countingDown || countdownPaused) { return; }

        countdownPaused = true;
        pauseTime = System.currentTimeMillis();
    }

    public void resumeCountdown() {

        if(!countingDown || !countdownPaused) { return; }

        countdownPaused = false;

        // Use our pauseTime to offset the startTime
        long pausedTime = System.currentTimeMillis() - pauseTime;
        startTime = startTime + pausedTime;

        invalidate();
    }

    public boolean isCountdownPaused() {
        return countdownPaused;
    }

    public boolean isCountingDown() {
        return countingDown;
    }

    public void setCountdownCallback(CountdownCallback callback) {

        mCallback = callback;
    }

    public static interface CountdownCallback {

        public void onCountdownStopped();
        public void onCountdownStarted();
        public void onCountdownFinished();
        public void onCountdownPaused();
    }
}
