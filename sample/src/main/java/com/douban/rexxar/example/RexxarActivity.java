package com.douban.rexxar.example;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;

import com.douban.rexxar.example.widget.AlertDialogWidget;
import com.douban.rexxar.example.widget.PullToRefreshWidget;
import com.douban.rexxar.example.widget.TitleWidget;
import com.douban.rexxar.example.widget.ToastWidget;
import com.douban.rexxar.example.widget.menu.MenuItem;
import com.douban.rexxar.example.widget.menu.MenuWidget;
import com.douban.rexxar.view.RexxarWebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by luanqian on 16/9/26.
 */
public class RexxarActivity extends AppCompatActivity {

    public static final String TAG = RexxarActivity.class.getSimpleName();

    public static void startActivity(Activity activity, String uri) {
        Intent intent = new Intent(activity, RexxarActivity.class);
        intent.setData(Uri.parse(uri));
        activity.startActivity(intent);
    }

    @InjectView(R.id.webView)
    RexxarWebView mRexxarWebView;

    private List<MenuItem> mMenuItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rexxar_activity);
        ButterKnife.inject(this);
        setTitle(R.string.title_rexxar);

        String uri = getIntent().getData().toString();

        if (TextUtils.isEmpty(uri)) {
            finish();
            return;
        }

        // add widget
        mRexxarWebView.addRexxarWidget(new TitleWidget());
        mRexxarWebView.addRexxarWidget(new AlertDialogWidget());
        mRexxarWebView.addRexxarWidget(new ToastWidget());
        mRexxarWebView.addRexxarWidget(new PullToRefreshWidget());
        mRexxarWebView.addRexxarWidget(new MenuWidget());

        // load uri
        mRexxarWebView.loadUri(uri);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        testFunc();
    }

    private void testFunc() {
        try {
            JSONObject user = new JSONObject();
            user.put("name", "name");
            user.put("age", 18);
            mRexxarWebView.callFunction("alert", user.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setMenuItems(List<MenuItem> menuItems) {
        if (null == menuItems || menuItems.size() == 0) {
            return;
        }
        mMenuItems.clear();
        mMenuItems.addAll(menuItems);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        for (MenuItem menuItem : mMenuItems) {
            menuItem.getMenuView(menu, this);
        }
        return super.onCreateOptionsMenu(menu);
    }

}
