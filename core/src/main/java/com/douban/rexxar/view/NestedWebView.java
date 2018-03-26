package com.douban.rexxar.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.webkit.WebView;

/*
 * Copyright (C) 2015 takahirom
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
public class NestedWebView extends WebView implements NestedScrollingChild {

    static final String TAG = "NestedWebView";
    private int mLastY;
    private final int[] mOffsetInWindow = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedOffsetY;
    private NestedScrollingChildHelper mChildHelper;
    // webview是否消耗了move事件
    private boolean mWebViewConsumeDelta;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private boolean mNestedScrollEstablish = false;

    private boolean mMoved = false;
    private int mLastY1;

    public NestedWebView(Context context) {
        this(context, null);
    }

    public NestedWebView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.webViewStyle);
    }

    public NestedWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mVelocityTracker = VelocityTracker.obtain();
    }

    @TargetApi(21)
    public NestedWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }



    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        // 计算是否move
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mMoved = false;
                mLastY1 = (int) ev.getY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (Math.abs(ev.getY() - mLastY1) > mTouchSlop) {
                    mMoved = true;
                }
                mLastY1 = (int) ev.getY();
                break;
            }
        }

        boolean returnValue = false;

        MotionEvent event = MotionEvent.obtain(ev);
        final int action = MotionEventCompat.getActionMasked(event);
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsetY = 0;
            mWebViewConsumeDelta = false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            mVelocityTracker.addMovement(ev);
        }
        int eventX = (int) event.getX();
        int eventY = (int) event.getY();
        event.offsetLocation(0, -mNestedOffsetY);
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (mNestedScrollEstablish) {
                    int deltaY = mLastY - eventY;
                    // NestedPreScroll
                    if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mOffsetInWindow)) {
                        deltaY -= mScrollConsumed[1];
                        mLastY = eventY - mOffsetInWindow[1];
                        mNestedOffsetY += mOffsetInWindow[1];
                        event.offsetLocation(0, -mOffsetInWindow[1]);
                    } else {
                        mLastY = eventY;
                    }

                    // 当parent不能consume所有delta的时候才交给webView处理
                    int oldScrollY = getScrollY();
                    if ((deltaY < 0 && getScrollY() > 0) || deltaY > 0) {
                        returnValue = super.onTouchEvent(event);
                        mWebViewConsumeDelta = true;
                    }

                    // 修正deltaY
                    if (deltaY == getScrollY() - oldScrollY) {
                        // 完全消耗完，不做处理
                    } else if (deltaY < getScrollY() - oldScrollY) {
                        // 下滑时候未消耗完
                        if (getScrollY() <= 5) {
                            int dyConsumed = oldScrollY - getScrollY();
                            int dyUnconsumed = deltaY - (getScrollY() - oldScrollY);
                            if (dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, mOffsetInWindow)) {
                                mNestedOffsetY += mOffsetInWindow[1];
                                mLastY -= mOffsetInWindow[1];
                                event.offsetLocation(0, mOffsetInWindow[1]);
                            }
                        }
                    } else {
                        // 上滑未消耗完，不做处理
                    }
                } else {
                    returnValue = super.onTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_DOWN:
                returnValue = super.onTouchEvent(event);
                mLastY = eventY;
                // start NestedScroll
                mNestedScrollEstablish = startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mNestedScrollEstablish) {
                    int deltaY = mLastY - eventY;
                    // 如果嵌套滑动 & webview没有滑动的话，为了避免webview接收到down，up事件误认为点击，所以改成cancel
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) VelocityTrackerCompat.getYVelocity(mVelocityTracker, 0);
                    Log.i("xxxx", "initialVelocity : " + initialVelocity);
                    if (mNestedOffsetY != 0 && !mWebViewConsumeDelta) {
                        // 如果产生了嵌套滑动且WebView没消耗距离，则有一下逻辑
                        if (deltaY <= 0) {
                            // 往下滑动，直接cancel
                            // TODO how about fling 到最大值
                            event.setAction(MotionEvent.ACTION_CANCEL);
                        } else if (Math.abs(initialVelocity) < mMinimumVelocity) {
                            // 往上滑动，速度没有达到阈值，则直接cancel
                            event.setAction(MotionEvent.ACTION_CANCEL);
                            // TODO how about fling 到最大值
                        } else {
                            Log.i("xxxx", "mMinimumVelocity : " + mMinimumVelocity + " ; initialVelocity : " + initialVelocity);
                        }
                    } else if (mNestedOffsetY == 0 && !mWebViewConsumeDelta) {
                        // 如果没有产生嵌套滑动且webview没有消耗距离
                        // 如果使劲向下拉动，由于move事件没有传递给webview，所以需要主动cancel
                        if (mMoved && deltaY <= 0 && getScrollY() == 0) {
                            event.setAction(MotionEvent.ACTION_CANCEL);
                        } else {
                            Log.i("xxxx", "initialVelocity : " + initialVelocity + "; hhh");
                        }
                    } else {
                        Log.i("xxxx", "mNestedOffsetY is : " + mNestedOffsetY + " , mWebViewConsumeDelta" + mWebViewConsumeDelta + ";");
                    }
                    mVelocityTracker.clear();
//                    if (mMoved) {
//                        event.setAction(MotionEvent.ACTION_CANCEL);
//                    }
                    returnValue = super.onTouchEvent(event);
                    // end NestedScroll
                    stopNestedScroll();
                } else {
                    returnValue = super.onTouchEvent(event);
                }
                break;
        }
        return returnValue;
    }

    // Nested Scroll implements
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
                                        int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

}