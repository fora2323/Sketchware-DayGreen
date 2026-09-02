package extensions.anbui.daydream.ui;

import android.animation.ObjectAnimator;
import android.util.Log;
import android.view.View;

public class QuickAnimation {

    public static final String TAG = "QuickAnimation";

    public static void rotateOnce(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f);
        animator.setDuration(500);
        animator.start();
    }

    public static void scaleOut(View view) {
        ObjectAnimator animatorx = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f);
        animatorx.setDuration(500);
        animatorx.start();

        ObjectAnimator animatory = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f);
        animatory.setDuration(500);
        animatory.start();
    }

    public static void scaleXOut(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f);
        animator.setDuration(500);
        animator.start();
    }

    public static void leftToRight(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX", -300f, 0f);
        animator.setDuration(500);
        animator.start();
    }

    public static void scaleOutForViewSelected(View view) {
        try {
            ObjectAnimator animatorx = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f);
            animatorx.setDuration(200);
            animatorx.start();

            ObjectAnimator animatory = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f);
            animatory.setDuration(200);
            animatory.start();
        } catch (Exception e) {
            Log.e(TAG, "scaleOutForViewSelected: " + e.getMessage());
        }
    }
}
