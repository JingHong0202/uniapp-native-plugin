package io.surprise.file.libs;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FABBehavior extends FloatingActionButton.Behavior {
    private boolean visible = true;//是否可见
    private long offset = 0;

    public FABBehavior(Context context, AttributeSet attrs) {

        super(context, attrs);
    }

    //当观察的RecyclerView发生滑动开始的时候回调的
    //axes滑动关联轴，我们现在只关心垂直的滑动


    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL || super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type);
    }


    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionButton child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);
        offset += dyConsumed;
        Log.d("SCROLL", "onNestedScroll: " +offset);
        if (offset == 0 && visible) {
            visible = false;
            animateOut(child);
        } else if (offset > 0 && !visible) {
            visible = true;
            animateIn(child);
        }
    }


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

}
