package com.amalbit.trail;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.DashPathEffect;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.amalbit.trail.contract.AnimationCallback;

/**
 * Created by amal.chandran on 15/11/17.
 */

public class AnimationRouteHelper implements com.amalbit.trail.contract.Animator {

    private static final int ANIM_DURATION_DEFAULT = 1000;

    private static final int ANIM_DURATION_REPEAT = 1500;

    private AnimatorSet animatorRouteSet;

    private AnimatorSet animatorRepeatRouteSet;

    private ObjectAnimator firstTimeRouteAnimator;

    private ObjectAnimator secondTimeRouteAnimator;

    private ValueAnimator colorRouteAnimation;

    private boolean isAnimating;

    private OverlayPolyline overlayPolyline;

    private RouteOverlayView routeOverlayView;

    protected float length;

    protected float[] dashValue;

    protected boolean isFirstTimeDrawing;

    public static AnimationRouteHelper getInstance(RouteOverlayView routeOverlayView, OverlayPolyline overlayPolyline) {
        return new AnimationRouteHelper(routeOverlayView, overlayPolyline);
    }

    private AnimationRouteHelper(RouteOverlayView routeOverlayView, OverlayPolyline overlayPolyline) {
        this.routeOverlayView = routeOverlayView;
        this.overlayPolyline = overlayPolyline;
    }

    public void init() {

        if (firstTimeRouteAnimator == null) {
            firstTimeRouteAnimator = ObjectAnimator.ofFloat(this, "update", 1f, 0f);
            firstTimeRouteAnimator.setDuration(ANIM_DURATION_DEFAULT);
            firstTimeRouteAnimator.setInterpolator(new DecelerateInterpolator());
        }

        firstTimeRouteAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isFirstTimeDrawing = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                isFirstTimeDrawing = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        if (secondTimeRouteAnimator == null) {
            secondTimeRouteAnimator = ObjectAnimator.ofFloat(this, "update1", 0f, 1f);
            secondTimeRouteAnimator.setDuration(ANIM_DURATION_REPEAT);
            secondTimeRouteAnimator.setInterpolator(new AccelerateInterpolator());
        }

        if (colorRouteAnimation == null) {
            colorRouteAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), overlayPolyline.getBottomLayerColor(),
                    overlayPolyline.getTopLayerColor());
            colorRouteAnimation.setDuration(ANIM_DURATION_REPEAT); // milliseconds
            colorRouteAnimation.setStartDelay(750);
        }
        colorRouteAnimation.addUpdateListener(animator -> {
            overlayPolyline.getBottomLayerPaint().setColor((int) animator.getAnimatedValue());
            routeOverlayView.invalidate();
        });
        colorRouteAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                PathEffect effect = new DashPathEffect(new float[]{length, length}, length);
                overlayPolyline.getTopLayerPaint().setPathEffect(effect);
                overlayPolyline.getBottomLayerPaint().setColor(overlayPolyline.getBottomLayerColor());
                routeOverlayView.invalidate();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        animatorRepeatRouteSet = new AnimatorSet();
        animatorRouteSet = new AnimatorSet();

        animatorRepeatRouteSet.playTogether(secondTimeRouteAnimator, colorRouteAnimation);
        animatorRepeatRouteSet.addListener(new AnimatorListenerAdapter() {

            private boolean mCanceled;

            @Override
            public void onAnimationStart(Animator animation) {
                mCanceled = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCanceled) {
                    animation.start();
                }
            }

        });

        animatorRouteSet.playSequentially(firstTimeRouteAnimator, animatorRepeatRouteSet);
        animatorRouteSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                animatorRepeatRouteSet.cancel();
            }
        });
    }

    public void setUpdate(float update) {
        PathEffect effect = new DashPathEffect(dashValue, length * update);
        overlayPolyline.getTopLayerPaint().setPathEffect(effect);
        routeOverlayView.invalidate();
    }

    public void setUpdate1(float update) {
        PathEffect effect = new DashPathEffect(dashValue, -length * update);
        overlayPolyline.getTopLayerPaint().setPathEffect(effect);
        routeOverlayView.invalidate();
    }

    @Override
    public void play() {
        stop(null);
        init();
        animatorRouteSet.start();
        isAnimating = true;
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    @Override
    public void stop(AnimationCallback callback) {
        if (animatorRouteSet != null) {
            animatorRouteSet.end();
            animatorRouteSet.cancel();
            animatorRepeatRouteSet.end();
            animatorRepeatRouteSet.cancel();
            firstTimeRouteAnimator.end();
            firstTimeRouteAnimator.cancel();
            secondTimeRouteAnimator.end();
            secondTimeRouteAnimator.cancel();
            colorRouteAnimation.end();
            colorRouteAnimation.cancel();

            animatorRouteSet = null;
            animatorRepeatRouteSet = null;
            firstTimeRouteAnimator = null;
            secondTimeRouteAnimator = null;
            colorRouteAnimation = null;
        }
        isAnimating = false;
    }

    @Override
    public void onPathMeasureChange() {
        PathMeasure pathMeasure = new PathMeasure(overlayPolyline.getDrawPath(), false);
        length = pathMeasure.getLength();
        dashValue =
                new float[]{length, length};
    }
}
