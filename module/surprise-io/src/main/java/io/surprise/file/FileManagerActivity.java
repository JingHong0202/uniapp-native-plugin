package io.surprise.file;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.HashMap;

import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.surprise.file.adapter.VpAdapter;
import io.surprise.file.adapter.rvAdapter;
import io.surprise.file.model.File;
import io.surprise.file.model.Options;


public class FileManagerActivity extends AppCompatActivity {

    private ArrayList<Options> options = new ArrayList<>();
    private VpAdapter adapter;
    int currentPosition;
    public static HashMap<Integer, Object> selectList = new HashMap<>();
    public static UniJSCallback callback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 接受参数
        Intent intent = getIntent();
        String StrOptions = intent.getStringExtra("options");
        JSONArray parseOptions = JSON.parseArray(StrOptions);
        for (int i = 0; i < parseOptions.size(); i++) {
            options.add(JSON.parseObject(String.valueOf(parseOptions.get(i)), Options.class));
        }
        setContentView(R.layout.activity_file_manager);

        // 初始化toolbar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("文件选择器");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager.LayoutParams localLayoutParams = getWindow().getAttributes();
            localLayoutParams.flags = (WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | localLayoutParams.flags);
        }


        // 初始化viewpage or tablayout
        ViewPager2 viewPager = findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(options.size());
        TabLayout tabLayout = findViewById(R.id.tab);
        adapter = new VpAdapter(options);
        viewPager.setAdapter(adapter);
        new TabLayoutMediator(tabLayout, viewPager, true, (tab, position) -> tab.setText(options.get(position).title)).attach();
        if (options.size() == 1) tabLayout.setVisibility(View.GONE);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
//            @Override
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
//            }

            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                rvAdapter currentViewPage = adapter.tabPages.get(position).adapter;
                if (!currentViewPage.inited) {
                    currentViewPage.initData(getBaseContext());
                    currentViewPage.notifyItemRangeInserted(0, currentViewPage.count);
                    currentViewPage.setOnItemClickListener((view, parent, position2) -> {
                        File current = currentViewPage.list.get(position2);
                        CheckBox checkBox = view.findViewById(R.id.checkBox);
                        if (!selectList.containsKey(current.id)) {
                            selectList.put(current.id, current);
                            checkBox.setChecked(true);
                        } else {
                            selectList.remove(current.id);
                            checkBox.setChecked(false);
                        }
                        boolean selectListNotNull = selectList.size() > 0;
                        setTitle(selectListNotNull ? "已选择" + selectList.size() + "项" : "文件选择器");
                        getSupportActionBar().setDisplayHomeAsUpEnabled(!selectListNotNull);
                        myToolbar.getMenu().findItem(R.id.confirm).setVisible(selectListNotNull);
                        myToolbar.getMenu().findItem(R.id.cancel).setVisible(selectListNotNull);
                    });
                } else {
                    currentViewPage.notifyDataSetChanged();
                }
//                Log.d("CLICK", "onClick: url: " + position);
                super.onPageSelected(position);
            }

//            @Override
//            public void onPageScrollStateChanged(int state) {
//                super.onPageScrollStateChanged(state);
//            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            onResult();
        } else if (item.getItemId() == R.id.cancel) {
            selectList.clear();
            setTitle("文件选择器");
            Toolbar myToolbar = findViewById(R.id.my_toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            myToolbar.getMenu().findItem(R.id.confirm).setVisible(false);
            myToolbar.getMenu().findItem(R.id.cancel).setVisible(false);
            adapter.tabPages.get(currentPosition).adapter.notifyDataSetChanged();
        } else if (item.getItemId() == R.id.all) {
            ArrayList<File> list = adapter.tabPages.get(currentPosition).adapter.list;
            for (int i = 0; i < list.size(); i++) {
                selectList.put(list.get(i).id, list.get(i));
            }
            setTitle("已选择" + selectList.size() + "项");
            Toolbar myToolbar = findViewById(R.id.my_toolbar);
            myToolbar.getMenu().findItem(R.id.confirm).setVisible(true);
            myToolbar.getMenu().findItem(R.id.cancel).setVisible(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            adapter.tabPages.get(currentPosition).adapter.notifyDataSetChanged();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.list_menu, menu);
        menu.findItem(R.id.confirm).setVisible(false);
        menu.findItem(R.id.cancel).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDestroy() {
        options = null;
        for (int i = 0; i < adapter.tabPages.size(); i++) {
            if (adapter.tabPages.get(i).adapter.list.size() > 0) {
                adapter.tabPages.get(i).adapter.list.clear();
            }
        }
        adapter.tabPages.clear();
        adapter = null;
        currentPosition = 0;
        selectList.clear();
        callback = null;
        super.onDestroy();
    }

    private void onResult() {
        JSONArray result = new JSONArray();
        for (int index : selectList.keySet()) {
            result.add(selectList.get(index));
        }
        callback.invoke(result);
        finish();
    }

}