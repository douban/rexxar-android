package com.douban.rexxar.example.widget.menu;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.douban.rexxar.example.R;

/**
 * Created by luanqian on 15/11/24.
 */
@Keep
public class MenuItem {

    public String type;
    public String title;
    public String icon;
    public String uri;
    public String color;

    public void getMenuView(Menu menu, final Context context) {
        if (null == menu) {
            return;
        }
        android.view.MenuItem menuItem = menu.add(title);
        menuItem.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItem.setActionView(R.layout.view_button_menu);
        View actionView = menuItem.getActionView();
        final TextView titleView = (TextView) menuItem.getActionView().findViewById(R.id.title);
        if (!TextUtils.isEmpty(color)) {
            try {
                titleView.setTextColor(Color.parseColor(Uri.decode(color)));
                titleView.setText(title);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        actionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // dispatch uri
                Toast.makeText(context, "click menu, dispatch uri : " + uri, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
