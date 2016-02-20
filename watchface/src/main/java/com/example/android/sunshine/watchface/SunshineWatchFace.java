/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.sunshine.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {

    private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
//    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1) / 2;


    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    // Handler to update the time periodically in interactive mode???
    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {

//        Time mTime;
        Calendar mCalendar;

        final Handler mUpdateTimeHandler = new EngineHandler(this);

        // Keep track of the registration state to prevent throwing an exception when unregistering an unregistered receiver
        boolean mRegisteredTimeZoneReceiver = false;

        // Handles time zone changes
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
//                mTime.clear(intent.getStringExtra("time-zone"));
//                mTime.setToNow();
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        Paint mBackgroundPaint;
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        Paint mDayPeriodPaint;
        Paint mDatePaint;
        Paint mHighTempPaint;
        Paint mLowTempPaint;
        boolean mAmbient;

        boolean mShouldDrawColons;

        float mTimeXOffset;
        float mTimeYOffset;
        float mTimeXSpace;
        float mColonWidth;
        float mDateXOffset;
        float mTempXOffset;
        float mTempYOffset;
        float mTempXSpace;
        float mTempWidth;

        // Whether the display supports fewer bits for each color in ambient mode.
        // When true, we disable anti-aliasing in ambient mode.
        boolean mLowBitAmbient;


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = SunshineWatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(ContextCompat.getColor(getApplicationContext(), R.color.background));

            mHourPaint = createTextPaint(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text),
                    BOLD_TYPEFACE,
                    resources.getDimension(R.dimen.time_size));

            mMinutePaint = createTextPaint(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text),
                    NORMAL_TYPEFACE,
                    resources.getDimension(R.dimen.time_size));

            mDatePaint = createTextPaint(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text_light),
                    NORMAL_TYPEFACE,
                    resources.getDimension(R.dimen.date_size));

            mHighTempPaint = createTextPaint(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text),
                    BOLD_TYPEFACE,
                    resources.getDimension(R.dimen.temperature_size));

            mLowTempPaint = createTextPaint(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text_light),
                    NORMAL_TYPEFACE,
                    resources.getDimension(R.dimen.temperature_size));


//            mTime = new Time();

            // allocate a Calendar to calculate local time using the UTC time and time zone
            mCalendar = Calendar.getInstance();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        private Paint createTextPaint(int textColor, Typeface typeface, float textSize) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(typeface);
            paint.setTextSize(textSize);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
//                mTime.clear(TimeZone.getDefault().getID());
//                mTime.setToNow();
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        // Register Receiver on ACTION_TIMEZONE_CHANGED
        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        // Unregister receiver on ACTION_TIMEZONE_CHANGED
        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();

//            mTimeXOffset = resources.getDimension(isRound
//                    ? R.dimen.time_x_offset_round : R.dimen.time_x_offset);

//            float textSize = resources.getDimension(isRound
//                    ? R.dimen.digital_time_size_round : R.dimen.digital_time_size);
//            mHourPaint.setTextSize(textSize);

            mTimeXOffset = resources.getDimension(R.dimen.time_x_offset);
            mTimeYOffset = resources.getDimension(R.dimen.time_y_offset);
            mTimeXSpace = resources.getDimension(R.dimen.time_x_space);

            mColonWidth = mHourPaint.measureText(":") / 2;

            mDateXOffset = mDatePaint.measureText(getString(R.string.dummy_date)) / 2;

            mTempXOffset = resources.getDimension(R.dimen.temperature_x_offset);
            mTempYOffset = resources.getDimension(R.dimen.temperature_y_offset);
            mTempXSpace = resources.getDimension(R.dimen.temperature_x_space);

            mTempWidth = mHighTempPaint.measureText("25°") / 2;

        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHourPaint.setAntiAlias(!inAmbientMode);
                    mMinutePaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }
            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Update the time
//            mTime.setToNow();
            mCalendar.setTimeInMillis(System.currentTimeMillis());

            // Draw H:MM in ambient mode / interactive mode.
//            String hour = String.format("%2d", mTime.hour);
//            String minute = String.format("%02d", mTime.minute);
            String hour = String.format("%02d", mCalendar.get(Calendar.HOUR_OF_DAY));
            String minute = String.format("%02d", mCalendar.get(Calendar.MINUTE));

            canvas.drawText(hour,
                    bounds.centerX() - mTimeXOffset - mTimeXSpace - mColonWidth,
                    bounds.centerY() - mTimeYOffset,
                    mHourPaint);

            mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;
            if (isInAmbientMode()) {
                canvas.drawText(":",
                        bounds.centerX() - mColonWidth,
                        bounds.centerY() - mTimeYOffset,
                        mHourPaint);
            } else {
                if (mShouldDrawColons){
                    canvas.drawText(":",
                            bounds.centerX() - mColonWidth,
                            bounds.centerY() - mTimeYOffset,
                            mHourPaint);
                }
            }

            canvas.drawText(minute,
                    bounds.centerX() + mColonWidth + mTimeXSpace,
                    bounds.centerY() - mTimeYOffset,
                    mMinutePaint);


            // Display more info in interactive mode
            if (!mAmbient) {

                String date = getString(R.string.dummy_date);
                canvas.drawText(date,
                        bounds.centerX() - mDateXOffset,
                        bounds.centerY(),
                        mDatePaint);

                String highTemp = getString(R.string.dummy_temperature_high);
                canvas.drawText(highTemp,
                        bounds.centerX() - mTempWidth,
                        bounds.centerY() + mTempYOffset,
                        mHighTempPaint);

                String lowTemp = getString(R.string.dummy_temperature_low);
                canvas.drawText(lowTemp,
                        bounds.centerX() + mTempWidth + mTempXSpace,
                        bounds.centerY() + mTempYOffset,
                        mLowTempPaint);

            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
