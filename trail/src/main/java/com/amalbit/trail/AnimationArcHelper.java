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
import android.support.annotation.NonNull;
import android.view.animation.DecelerateInterpolator;
import com.amalbit.trail.contract.AnimationCallback;
import com.amalbit.trail.util.AnimatorListener;

/**
 * Created by amal.chandran on 15/11/17.
 */

public class AnimationArcHelper implements com.amalbit.trail.contract.Animator {

  private static final int ANIM_DURATION_DEFAULT = 1000;

  private static final int ANIM_DURATION_REPEAT = 1500;

  private AnimatorSet animatorArcSet;

  private AnimatorSet animatorRepeatArcSet;

  private RouteOverlayView mRouteOverlayView;

  private ObjectAnimator firstTimeArcAnimator;

  private ObjectAnimator secondTimeArcAnimator;

  private ValueAnimator colorArcAnimation;

  protected float arcLength;

  protected float shadowLength;

  protected float[] arcdDashValue;

  protected float[] shadowDashValue;

  protected boolean isFirstTimeDrawing;

  protected boolean animStarted;

  private Route route;

  public static AnimationArcHelper getInstance(RouteOverlayView routeOverlayView, Route route) {
    return new AnimationArcHelper(routeOverlayView, route);
  }

  private AnimationArcHelper(RouteOverlayView routeOverlayView, Route route) {
    this.mRouteOverlayView = routeOverlayView;
    this.route = route;
  }

  private void init() {

    if (firstTimeArcAnimator == null) {
      firstTimeArcAnimator = ObjectAnimator.ofFloat(this, "update", 1f, 0f);
      firstTimeArcAnimator.setDuration(ANIM_DURATION_DEFAULT);
      firstTimeArcAnimator.setInterpolator(new DecelerateInterpolator());
    }
    firstTimeArcAnimator.addListener(new AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animator) {
        super.onAnimationStart(animator);
        isFirstTimeDrawing = true;
        animStarted = true;
      }

      @Override
      public void onAnimationEnd(Animator animator) {
        super.onAnimationEnd(animator);
        isFirstTimeDrawing = false;
        route.getShadowPaint().setPathEffect(null);
      }
    });

    if (secondTimeArcAnimator == null) {
      secondTimeArcAnimator = ObjectAnimator.ofFloat(this, "update1", 0f, 1f);
      secondTimeArcAnimator.setDuration(ANIM_DURATION_REPEAT);
      secondTimeArcAnimator.setInterpolator(new DecelerateInterpolator());
    }

    if (colorArcAnimation == null) {
      colorArcAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), route.getBottomLayerColor(),
          route.getTopLayerColor());
      colorArcAnimation.setDuration(ANIM_DURATION_REPEAT); // milliseconds
      colorArcAnimation.setStartDelay(250);
    }

    colorArcAnimation.addUpdateListener(animator -> {
      route.getBottomLayerPaint().setColor((int) animator.getAnimatedValue());
      mRouteOverlayView.invalidate();
    });
    colorArcAnimation.addListener(new AnimatorListener() {
      @Override
      public void onAnimationEnd(Animator animator) {
        PathEffect effect = new DashPathEffect(new float[]{arcLength, arcLength}, arcLength);
        route.getTopLayerPaint().setPathEffect(effect);
        route.getBottomLayerPaint().setColor(route.getBottomLayerColor());
        mRouteOverlayView.invalidate();
      }
    });

    animatorRepeatArcSet = new AnimatorSet();
    animatorArcSet = new AnimatorSet();

    animatorRepeatArcSet.playTogether(secondTimeArcAnimator, colorArcAnimation);
    animatorRepeatArcSet.addListener(new AnimatorListenerAdapter() {

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
      public void onAnimationEnd(@NonNull Animator animation) {
        if (!mCanceled) {
          animation.start();
        }
      }

    });

    animatorArcSet.playSequentially(firstTimeArcAnimator, animatorRepeatArcSet);
    animatorArcSet.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationCancel(Animator animation) {
        super.onAnimationCancel(animation);
        animatorRepeatArcSet.cancel();
      }
    });
  }

  public void setUpdate(float update) {
    PathEffect effect = new DashPathEffect(arcdDashValue, arcLength * update);
    route.getTopLayerPaint().setPathEffect(effect);

    PathEffect shadowEffect = new DashPathEffect(shadowDashValue, shadowLength * update);
    route.getShadowPaint().setPathEffect(shadowEffect);

    mRouteOverlayView.invalidate();
  }

  public void setUpdate1(float update) {
    PathEffect effect = new DashPathEffect(arcdDashValue, -arcLength * update);
    route.getTopLayerPaint().setPathEffect(effect);
    mRouteOverlayView.invalidate();
  }

  @Override
  public void play() {
    stop(null);
    init();
    animatorArcSet.start();
  }

  @Override
  public void stop(AnimationCallback callback) {
    if (animatorArcSet != null) {
      colorArcAnimation.end();
      colorArcAnimation.cancel();
      secondTimeArcAnimator.end();
      secondTimeArcAnimator.cancel();
      firstTimeArcAnimator.end();
      firstTimeArcAnimator.cancel();
      animatorRepeatArcSet.end();
      animatorRepeatArcSet.cancel();
      animatorArcSet.end();
      animatorArcSet.cancel();

      animatorRepeatArcSet = null;
      animatorArcSet = null;
      firstTimeArcAnimator = null;
      secondTimeArcAnimator = null;
      colorArcAnimation = null;
    }
  }

  @Override
  public void onPathMeasureChange() {
    PathMeasure pathMeasure = new PathMeasure(route.getDrawPath(), false);
    arcLength = pathMeasure.getLength();
    arcdDashValue =
        new float[]{arcLength, arcLength};
  }
}
