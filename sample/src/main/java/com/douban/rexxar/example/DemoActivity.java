package com.douban.rexxar.example;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.douban.rexxar.example.widget.AlertDialogWidget;
import com.douban.rexxar.example.widget.PullToRefreshWidget;
import com.douban.rexxar.example.widget.TitleWidget;
import com.douban.rexxar.example.widget.ToastWidget;
import com.douban.rexxar.example.widget.menu.MenuWidget;
import com.douban.rexxar.view.RexxarWebView;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by luanqian on 16/9/26.
 */
public class DemoActivity extends AppCompatActivity {

    public static final String TAG = DemoActivity.class.getSimpleName();

    public static void startActivity(Activity activity) {
        Intent intent = new Intent(activity, DemoActivity.class);
        activity.startActivity(intent);
    }

    @InjectView(R.id.webView)
    RexxarWebView mRexxarWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.partial_activity);
        ButterKnife.inject(this);
        setTitle(R.string.title_partial_rexxar);

        // add widget
        mRexxarWebView.addRexxarWidget(new TitleWidget());
        mRexxarWebView.addRexxarWidget(new AlertDialogWidget());
        mRexxarWebView.addRexxarWidget(new ToastWidget());
        mRexxarWebView.addRexxarWidget(new PullToRefreshWidget());
        mRexxarWebView.addRexxarWidget(new MenuWidget());

        // load uri
        mRexxarWebView.loadPartialUri("douban://douban.com/rexxar_demo");
    }

}
