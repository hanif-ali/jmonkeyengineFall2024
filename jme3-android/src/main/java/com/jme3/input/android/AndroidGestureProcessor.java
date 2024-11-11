/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jme3.input.android;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.jme3.input.event.TouchEvent;
import java.util.logging.Logger;

/**
 * AndroidGestureHandler uses Gesture type listeners to create jME TouchEvents
 * for gestures.  This class is designed to handle the gestures supported
 * on Android rev 9 (Android 2.3).  Extend this class to add functionality
 * added by Android after rev 9.
 *
 * @author iwgeric
 */
public class AndroidGestureProcessor implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener {
    private static final Logger logger = Logger.getLogger(AndroidGestureProcessor.class.getName());

    private AndroidTouchInput touchInput;
    float gestureDownX = -1f;
    float gestureDownY = -1f;
    float scaleStartX = -1f;
    float scaleStartY = -1f;

    public AndroidGestureProcessor(AndroidTouchInput touchInput) {
        this.touchInput = touchInput;
    }

    /* Events from onGestureListener */

    @Override
    public boolean onDown(MotionEvent event) {
        // The start of all GestureListeners. Not really a gesture by itself,
        // so we don't create an event.
        // However, reset the scaleInProgress here since this is the beginning
        // of a series of gesture events.
        gestureDownX = touchInput.getJmeX(event.getX());
        gestureDownY = touchInput.invertY(touchInput.getJmeY(event.getY()));
        return true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        // Up of single tap.  May be followed by a double tap later.
        // use onSingleTapConfirmed instead.
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        pressEventHandler(event, TouchEvent.Type.SHOWPRESS);
    }

    @Override
    public void onLongPress(MotionEvent event) {
        pressEventHandler(event, TouchEvent.Type.LONGPRESSED);
    }

    private void pressEventHandler(MotionEvent event, TouchEvent.Type eventType) {
        float jmeX = touchInput.getJmeX(event.getX());
        float jmeY = touchInput.invertY(touchInput.getJmeY(event.getY()));
        TouchEvent touchEvent = touchInput.getFreeTouchEvent();
        touchEvent.set(eventType, jmeX, jmeY, 0, 0);
        touchEvent.setPointerId(touchInput.getPointerId(event));
        touchEvent.setTime(event.getEventTime());
        touchEvent.setPressure(event.getPressure());
        touchInput.addEvent(touchEvent);
    }

    @Override
    public boolean onScroll(MotionEvent startEvent, MotionEvent endEvent, float distX, float distY) {
        // if not scaleInProgress, send scroll events.  This is to avoid sending
        // scroll events when one of the fingers is lifted just before the other one.
        // Avoids sending the scroll for that brief period of time.
        // Return true so that the next event doesn't accumulate the distX and distY values.
        // Apparently, both distX and distY are negative.
        // Negate distX to get the real value, but leave distY negative to compensate
        // for the fact that jME has y=0 at bottom where Android has y=0 at top.
        if (!touchInput.getScaleDetector().isInProgress()) {

            float jmeX = touchInput.getJmeX(endEvent.getX());
            float jmeY = touchInput.invertY(touchInput.getJmeY(endEvent.getY()));
            TouchEvent touchEvent = touchInput.getFreeTouchEvent();
            touchEvent.set(TouchEvent.Type.SCROLL, jmeX, jmeY, touchInput.getJmeX(-distX), touchInput.getJmeY(distY));
            touchEvent.setPointerId(touchInput.getPointerId(endEvent));
            touchEvent.setTime(endEvent.getEventTime());
            touchEvent.setPressure(endEvent.getPressure());
            touchInput.addEvent(touchEvent);
        }
        return true;
    }

    @Override
    public boolean onFling(MotionEvent startEvent, MotionEvent endEvent, float velocityX, float velocityY) {
        // Fling happens only once at the end of the gesture (all fingers up).
        // Fling returns the velocity of the finger movement in pixels/sec.
        // Therefore, the dX and dY values are actually velocity instead of distance values
        // Since this does not track the movement, use the start position and velocity values.

        float jmeX = touchInput.getJmeX(startEvent.getX());
        float jmeY = touchInput.invertY(touchInput.getJmeY(startEvent.getY()));
        TouchEvent touchEvent = touchInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.FLING, jmeX, jmeY, velocityX, velocityY);
        touchEvent.setPointerId(touchInput.getPointerId(endEvent));
        touchEvent.setTime(endEvent.getEventTime());
        touchEvent.setPressure(endEvent.getPressure());
        touchInput.addEvent(touchEvent);
        return true;
    }

    /* Events from onDoubleTapListener */

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        // Up of single tap when no double tap followed.
        float jmeX = touchInput.getJmeX(event.getX());
        float jmeY = touchInput.invertY(touchInput.getJmeY(event.getY()));
        TouchEvent touchEvent = touchInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.TAP, jmeX, jmeY, 0, 0);
        touchEvent.setPointerId(touchInput.getPointerId(event));
        touchEvent.setTime(event.getEventTime());
        touchEvent.setPressure(event.getPressure());
        touchInput.addEvent(touchEvent);
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        //The down motion event of the first tap of the double-tap
        // We could use this event to fire off a double tap event, or use
        // DoubleTapEvent with a check for the UP action
        float jmeX = touchInput.getJmeX(event.getX());
        float jmeY = touchInput.invertY(touchInput.getJmeY(event.getY()));
        TouchEvent touchEvent = touchInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.DOUBLETAP, jmeX, jmeY, 0, 0);
        touchEvent.setPointerId(touchInput.getPointerId(event));
        touchEvent.setTime(event.getEventTime());
        touchEvent.setPressure(event.getPressure());
        touchInput.addEvent(touchEvent);
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        //Notified when an event within a double-tap gesture occurs, including the down, move(s), and up events.
        // this means it will get called multiple times for a single double tap
        if (touchInput.getAction(event) == MotionEvent.ACTION_UP) {
            TouchEvent touchEvent = touchInput.getFreeTouchEvent();
            touchEvent.set(TouchEvent.Type.DOUBLETAP, event.getX(), touchInput.invertY(event.getY()), 0, 0);
            touchEvent.setPointerId(touchInput.getPointerId(event));
            touchEvent.setTime(event.getEventTime());
            touchEvent.setPressure(event.getPressure());
            touchInput.addEvent(touchEvent);
        }
        return true;
    }

    /* Events from ScaleGestureDetector */

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        // Scale uses a focusX and focusY instead of x and y.  Focus is the middle
        // of the fingers.  Therefore, use the x and y values from the Down event
        // so that the x and y values don't jump to the middle position.
        // return true or all gestures for this beginning event will be discarded
        scaleStartX = gestureDownX;
        scaleStartY = gestureDownY;
        TouchEvent touchEvent = touchInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.SCALE_START, scaleStartX, scaleStartY, 0f, 0f);
        touchEvent.setPointerId(0);
        touchEvent.setTime(scaleGestureDetector.getEventTime());
        touchEvent.setScaleSpan(scaleGestureDetector.getCurrentSpan());
        touchEvent.setDeltaScaleSpan(0f);
        touchEvent.setScaleFactor(scaleGestureDetector.getScaleFactor());
        touchEvent.setScaleSpanInProgress(touchInput.getScaleDetector().isInProgress());
        touchInput.addEvent(touchEvent);

        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        // return true or all gestures for this event will be accumulated
        scaleStartX = gestureDownX;
        scaleStartY = gestureDownY;
        TouchEvent touchEvent = touchInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.SCALE_MOVE, scaleStartX, scaleStartY, 0f, 0f);
        touchEvent.setPointerId(0);
        touchEvent.setTime(scaleGestureDetector.getEventTime());
        touchEvent.setScaleSpan(scaleGestureDetector.getCurrentSpan());
        touchEvent.setDeltaScaleSpan(scaleGestureDetector.getCurrentSpan() - scaleGestureDetector.getPreviousSpan());
        touchEvent.setScaleFactor(scaleGestureDetector.getScaleFactor());
        touchEvent.setScaleSpanInProgress(touchInput.getScaleDetector().isInProgress());
        touchInput.addEvent(touchEvent);
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        scaleStartX = gestureDownX;
        scaleStartY = gestureDownY;
        TouchEvent touchEvent = touchInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.SCALE_END, scaleStartX, scaleStartY, 0f, 0f);
        touchEvent.setPointerId(0);
        touchEvent.setTime(scaleGestureDetector.getEventTime());
        touchEvent.setScaleSpan(scaleGestureDetector.getCurrentSpan());
        touchEvent.setDeltaScaleSpan(scaleGestureDetector.getCurrentSpan() - scaleGestureDetector.getPreviousSpan());
        touchEvent.setScaleFactor(scaleGestureDetector.getScaleFactor());
        touchEvent.setScaleSpanInProgress(touchInput.getScaleDetector().isInProgress());
        touchInput.addEvent(touchEvent);
    }
}
