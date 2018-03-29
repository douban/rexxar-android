package com.douban.rexxar.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.Keep;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.douban.rexxar.route.RouteManager;

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
    }

    @TargetApi(21)
    public NestedWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
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
                            mLastY = eventY - mOffsetInWindow[1];
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
                                    mLastY -= mOffsetInWindow[1];
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
                break;
            case MotionEvent.ACTION_UP:
                if (mNestedScrollEstablish) {
                    if (mScrollHorizontalEstablish) {
                        // 横向滑动
                        event.offsetLocation(0, mLastY - eventY);
                    }
                    returnValue = super.onTouchEvent(event);
                    // end NestedScroll
                    stopNestedScroll();
                } else {
                    returnValue = super.onTouchEvent(event);
                }
                mScrollHorizontalEstablish = false;
                mScrollVerticalEstablish = false;
                mFrozenX = 0;
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

    @Keep
    private class NestScrollHelper {
        @JavascriptInterface
        public void optimizeHorizontalScroll() {
            mOptimizeHorizontalScroll = true;
        }
    }

}