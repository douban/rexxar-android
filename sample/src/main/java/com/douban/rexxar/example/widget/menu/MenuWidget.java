package com.douban.rexxar.example.widget.menu;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebView;
import android.widget.Toast;

import com.douban.rexxar.example.R;
import com.douban.rexxar.example.RexxarActivity;
import com.douban.rexxar.utils.GsonHelper;
import com.douban.rexxar.view.RexxarWidget;
import com.google.gson.reflect.TypeToken;
import com.mcxiaoke.next.task.SimpleTaskCallback;
import com.mcxiaoke.next.task.TaskBuilder;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * Created by luanqian on 15/11/24.
 */
public class MenuWidget implements RexxarWidget {

    static final String TAG = MenuWidget.class.getSimpleName();

    static final String KEY_DATA = "data";

    @Override
    public String getPath() {
        return "/widget/nav_menu";
    }

    @Override
    public boolean handle(final WebView view, String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        final Uri uri = Uri.parse(url);
        if (TextUtils.equals(uri.getPath(), getPath())) {
            TaskBuilder.create(new Callable<ArrayList<MenuItem>>() {
                @Override
                public ArrayList<MenuItem> call() throws Exception {
                    String data = uri.getQueryParameter(KEY_DATA);
                    if (TextUtils.isEmpty(data)) {
                        return null;
                    }

                    // get the menus
                    return GsonHelper.getInstance()
                            .fromJson(data, new TypeToken<ArrayList<MenuItem>>() {
                            }.getType());
                }
            }, new SimpleTaskCallback<ArrayList<MenuItem>>(){
                @Override
                public void onTaskSuccess(ArrayList<MenuItem> menuItems, Bundle extras) {
                    if (null != menuItems && !menuItems.isEmpty()) {
                        // show the menus and tilte
                        if (null != view && view.getContext() instanceof RexxarActivity) {
                            ((RexxarActivity) view.getContext()).setMenuItems(menuItems);
                        } else {
                            Toast.makeText(view.getContext(), R.string.error_partial_rexxar_menu, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }, TAG).start();
            return true;
        }
        return false;
    }
}
