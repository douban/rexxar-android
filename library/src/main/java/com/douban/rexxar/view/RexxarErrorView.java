package com.douban.rexxar.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.douban.rexxar.Constants;
import com.douban.rexxar.R;
import com.douban.rexxar.utils.BusProvider;

/**
 * Created by luanqian on 15/12/14.
 */
public class RexxarErrorView extends LinearLayout implements View.OnClickListener{

    ImageView mReloadImage;
    TextView mErrorMessage;

    public RexxarErrorView(Context context) {
        super(context);
    }

    public RexxarErrorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RexxarErrorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initIfNeeded() {
        if (getChildCount() > 0) {
            return;
        }
        LayoutInflater.from(getContext()).inflate(R.layout.view_error, this, true);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        mReloadImage = (ImageView) findViewById(R.id.refresh_img);
        mErrorMessage = (TextView) findViewById(R.id.error_title);
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getResources().getDimensionPixelOffset(R.dimen.error_padding_bottom));
    }

    /**
     * 显示错误界面，使用默认错误提示
     */
    public void show() {
        show(null);
    }

    /**
     * 显示错误界面
     *
     * @param text 错误提示
     */
    public void show(String text) {
        initIfNeeded();
        if (!TextUtils.isEmpty(text)) {
            mErrorMessage.setText(text);
        }
        mReloadImage.clearAnimation();
        setOnClickListener(this);
        setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        BusProvider.getInstance().post(new BusProvider.BusEvent(Constants.EVENT_REXXAR_RETRY, null));
    }

}
