package com.douban.rexxar.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.OverScroller;

import androidx.annotation.Keep;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.ViewCompat;

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
    private int mLastX;
    private int mLastY;
    private int mFrozenX;
    private final int[] mOffsetInWindow = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedOffsetY;
    private NestedScrollingChildHelper mChildHelper;
    private int mTouchSlop;
    private boolean mNestedScrollEstablish = false;

    // 是否优化横向滑动
    private boolean mOptimizeHorizontalScroll = false;
    // 是否是横向滑动
    private boolean mScrollHorizontalEstablish = false;
    // 是否为竖向滑动
    private boolean mScrollVerticalEstablish = false;
    private float mLastYWebViewConsume;

    // 默认开启嵌套滑动
    private boolean mEnableNestedScroll = true;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private OverScroller mScroller;
    private int mLastScrollerY;

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

        WebSettings webSettings = getSettings();
        webSettings.setJavaScriptEnabled(true);
        // 通过注入方法，优化横滑体验
        addJavascriptInterface(new NestScrollHelper(), "Android_NestScrollHelper");
        // 不过渡滑动
        setOverScrollMode(OVER_SCROLL_NEVER);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mScroller = new OverScroller(getContext());
    }

    /**
     * 启用/禁用 嵌套滑动
     */
    public void enableNestedScroll(boolean enable) {
        mEnableNestedScroll = enable;
    }

    @TargetApi(21)
    public NestedWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mEnableNestedScroll) {
            return super.onTouchEvent(ev);
        }
        boolean returnValue = false;

        MotionEvent event = MotionEvent.obtain(ev);
        final int action = MotionEventCompat.getActionMasked(event);
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsetY = 0;
        }
        int eventX = (int) event.getX();
        int eventY = (int) event.getY();
        event.offsetLocation(0, -mNestedOffsetY);
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (mNestedScrollEstablish) {
                    mVelocityTracker.addMovement(ev);
                    int deltaX = mLastX - eventX;
                    int deltaY = mLastY - eventY;
                    // 如果页面有横滑操作，则可以优化
                    if (mOptimizeHorizontalScroll) {
                        // 如果没有确定滑动方向，则重新确定
                        if (!mScrollHorizontalEstablish && !mScrollVerticalEstablish) {
                            if (Math.abs(deltaX) > Math.abs(deltaY) * 1.5  && Math.abs(deltaX) > mTouchSlop) {
                                mScrollHorizontalEstablish = true;
                            } else if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > mTouchSlop) {
                                mScrollVerticalEstablish = true;
                                mFrozenX = eventX;
                            }
                        }
                    }
                    mLastX = eventX;
                    if (mScrollHorizontalEstablish) {
                        event.offsetLocation(0, deltaY);
                        // 横向滑动
                        returnValue = super.onTouchEvent(event);
                    } else {
                        // 竖向滑动
                        if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mOffsetInWindow)) {
                            deltaY -= mScrollConsumed[1];
                            mLastY = eventY;
                            mNestedOffsetY += mOffsetInWindow[1];
                            event.offsetLocation(0, -mOffsetInWindow[1]);
                        } else {
                            mLastY = eventY;
                        }

                        // 当parent不能consume所有delta的时候才交给webView处理
                        int oldScrollY = getScrollY();
                        if ((deltaY < 0 && getScrollY() > 0) || deltaY > 0) {
                            // 如果是竖向滑动，则禁止横向滑动
                            if (mScrollVerticalEstablish) {
                                event.offsetLocation(mFrozenX - eventX, 0);
                                returnValue = super.onTouchEvent(event);
                            } else {
                                returnValue = super.onTouchEvent(event);
                            }
                            mLastYWebViewConsume = event.getY();
                        } else {
                            // FIXME 联合滚动
                            if (mScrollVerticalEstablish) {
                                event.offsetLocation(mFrozenX - eventX, mLastYWebViewConsume - event.getY());
                            } else {
                                event.offsetLocation(0, mLastYWebViewConsume - event.getY());
                            }
                            super.onTouchEvent(event);
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
//                                    mLastY -= mOffsetInWindow[1];
                                    event.offsetLocation(0, mOffsetInWindow[1]);
                                }
                            }
                            returnValue  = true;
                        } else {
                            // 上滑未消耗完，不做处理
                        }
                    }
                } else {
                    returnValue = super.onTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_DOWN:
                mLastYWebViewConsume = event.getY();
                returnValue = super.onTouchEvent(event);
                mLastX = eventX;
                mLastY = eventY;
                // start NestedScroll
                mNestedScrollEstablish = startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                mScrollHorizontalEstablish = false;
                mScrollVerticalEstablish = false;
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mNestedScrollEstablish) {
                    returnValue = super.onTouchEvent(event);
                    // end NestedScroll
                    stopNestedScroll();
                } else {
                    returnValue = super.onTouchEvent(event);
                }
                mScrollHorizontalEstablish = false;
                mScrollVerticalEstablish = false;
                mFrozenX = 0;
                recycleVelocityTracker();
                break;
            case MotionEvent.ACTION_UP:
                if (mNestedScrollEstablish) {
                    if (mScrollHorizontalEstablish) {
                        // 横向滑动
                        event.offsetLocation(0, mLastY - eventY);
                    }
                    returnValue = super.onTouchEvent(event);
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getYVelocity();
                    if ((Math.abs(initialVelocity) > mMinimumVelocity) && getScrollY() == 0) {
                        flingWithNestedDispatch(-initialVelocity);
                    } else {// end NestedScroll
                        stopNestedScroll();
                    }
                } else {
                    returnValue = super.onTouchEvent(event);
                }
                mScrollHorizontalEstablish = false;
                mScrollVerticalEstablish = false;
                mFrozenX = 0;
                recycleVelocityTracker();
                break;
        }
        return returnValue;
    }

    private void flingWithNestedDispatch(int velocityY) {
        if (!dispatchNestedPreFling(0, velocityY)) {
            dispatchNestedFling(0, velocityY, false);
            fling(velocityY);
        }
    }

    /**
     * Fling the scroll view
     *
     * @param velocityY The initial velocity in the Y direction. Positive
     *                  numbers mean that the finger/cursor is moving down the screen,
     *                  which means we want to scroll towards the top.
     */
    public void fling(int velocityY) {
        mLastScrollerY = 0;
        flinging = true;
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
        mScroller.fling(0, 0, // start
                0, velocityY, // velocities
                0, 0, // x
                Integer.MIN_VALUE, Integer.MAX_VALUE, // y
                0, 0); // overscroll
        ViewCompat.postInvalidateOnAnimation(getRootView());
    }

    private boolean flinging = false;

    @Override
    public void computeScroll() {
        if (flinging) {
            if (mScroller.computeScrollOffset()) {
                final int y = mScroller.getCurrY();
                int dy = y - mLastScrollerY;

                final int[] scrollConsumedTemp = new int[2];
                // Dispatch up to parent
                if (dispatchNestedPreScroll(0, dy, scrollConsumedTemp, null)) {
                    dy -= scrollConsumedTemp[1];
                }

                if (dy != 0) {
                    dispatchNestedScroll(0, 0, 0, dy, null);
                }

                // Finally update the scroll positions and post an invalidation
                mLastScrollerY = y;
                ViewCompat.postInvalidateOnAnimation(this);
            } else {
                flinging = false;
                // and reset the scroller y
                mLastScrollerY = 0;
                // We can't scroll any more, so stop any indirect scrolling
                if (hasNestedScrollingParent()) {
                    stopNestedScroll();
                }
             }
        } else {
            super.computeScroll();
        }
    }

    // Nested Scroll implements
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        if (mEnableNestedScroll) {
            mChildHelper.setNestedScrollingEnabled(enabled);
        }
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        if (mEnableNestedScroll) {
            return mChildHelper.isNestedScrollingEnabled();
        }
        return false;
    }

    @Override
    public boolean startNestedScroll(int axes) {
        if (mEnableNestedScroll) {
            return mChildHelper.startNestedScroll(axes);
        }
        return false;
    }

    @Override
    public void stopNestedScroll() {
        if (mEnableNestedScroll && !flinging) {
            mChildHelper.stopNestedScroll();
        }
    }

    @Override
    public boolean hasNestedScrollingParent() {
        if (mEnableNestedScroll) {
            return mChildHelper.hasNestedScrollingParent();
        }
        return false;
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
                                        int[] offsetInWindow) {
        if (mEnableNestedScroll) {
            return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
        }
        return false;
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        if (mEnableNestedScroll) {
            return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
        }
        return false;
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        if (mEnableNestedScroll) {
            return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
        }
        return false;
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        if (mEnableNestedScroll) {
            return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
        }
        return false;
    }

    @Keep
    private class NestScrollHelper {
        @JavascriptInterface
        public void optimizeHorizontalScroll() {
            mOptimizeHorizontalScroll = true;
        }
    }

}