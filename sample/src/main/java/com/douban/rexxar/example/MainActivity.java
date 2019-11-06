package com.douban.rexxar.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.douban.rexxar.resourceproxy.ResourceProxy;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by luanqian on 15/10/28.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.rexxar_page)
    public TextView mRexxarButton;
    @BindView(R.id.partial_rexxar_page)
    public TextView mPartialRexxarButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);
        mRexxarButton.setOnClickListener(this);
        mPartialRexxarButton.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (null == item) {
            return false;
        }
        switch (item.getItemId()) {
            case R.id.clear: {
                ResourceProxy.getInstance().clearCache();
                Toast.makeText(this, R.string.success_cleard_cache, Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (null == v) {
            return;
        }
        switch (v.getId()) {
            case R.id.rexxar_page: {
                RexxarActivity.startActivity(this, "douban://douban.com/rexxar_demo");
                return;
            }
            case R.id.partial_rexxar_page: {
                DemoActivity.startActivity(this);
                return;
            }
        }
    }
}
