package io.surprise.file.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import io.surprise.file.R;
import io.surprise.file.model.Options;

public class VpAdapter extends RecyclerView.Adapter<VpAdapter.VpViewHolder> {

    public ArrayList<Options> tabPages;

    public VpAdapter(ArrayList<Options> options) {
        tabPages = options;
    }


    @NonNull
    @Override
    public VpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VpViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.vp_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VpViewHolder holder, int position) {
        Options currentTab = tabPages.get(position);
        RecyclerView rv = holder.itemView.findViewById(R.id.rv);
        currentTab.adapter = (rvAdapter) rv.getAdapter();
        currentTab.adapter.queryOptions = currentTab.query != null ? JSON.parseObject(currentTab.query) : new JSONObject();
//        currentTab.adapter.initData(holder.itemView.getContext());
    }



    @Override
    public int getItemCount() {
        return tabPages.size();
    }

    public static class VpViewHolder extends RecyclerView.ViewHolder {

        private long offset = 0;
        private boolean visible = false;

        // FAB隐藏动画
        private void animateOut(FloatingActionButton fab) {
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
            fab.animate().translationY(fab.getHeight() + layoutParams.bottomMargin).setInterpolator(new AccelerateInterpolator(3));
            ViewCompat.animate(fab).scaleX(0f).scaleY(0f).start();
//        fab.hide();
        }

        // FAB显示动画
        private void animateIn(FloatingActionButton fab) {
            fab.animate().translationY(0).setInterpolator(new DecelerateInterpolator(3));
            ViewCompat.animate(fab).scaleX(1f).scaleY(1f).start();
//        fab.show();
        }

        public VpViewHolder(@NonNull View itemView) {
            super(itemView);
            RecyclerView rv = itemView.findViewById(R.id.rv);
            GridLayoutManager layoutManager = new GridLayoutManager(itemView.getContext(), 3);
            rv.setLayoutManager(layoutManager);
//            if (rvPool == null) {
//                rvPool = rv.getRecycledViewPool();
//                rv.setRecycledViewPool(rvPool);
//            } else {
//                rv.setRecycledViewPool(rvPool);
//            }
            rv.setNestedScrollingEnabled(false);
            rv.setFocusableInTouchMode(false);
//            rv.setItemViewCacheSize(6);
//            rv.setDrawingCacheEnabled(true);
//            rv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
            rv.setHasFixedSize(true);

            rvAdapter RecycleViewAdapter = new rvAdapter();
            RecycleViewAdapter.setHasStableIds(true);
            rv.setAdapter(RecycleViewAdapter);
            FloatingActionButton fab = itemView.findViewById(R.id.fab);
            fab.setTranslationY(300);
            fab.setOnClickListener(view -> {
                rv.scrollToPosition(0);
                animateOut(fab);
                visible = false;
                offset = 0;
            });


            rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    offset += dy;
                    if (offset == 0 && visible) {
                        animateOut(fab);
                        visible = false;
                    } else if (offset > 0 && !visible) {
                        animateIn(fab);
                        visible = true;
                    }
                }
            });


        }
    }

}
