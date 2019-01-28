package com.amalbit.trail;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.DashPathEffect;
import android.graphics.PathEffect;
import android.support.annotation.NonNull;
import android.view.animation.DecelerateInterpolator;
import com.amalbit.trail.contract.AnimationCallback;
import com.amalbit.trail.util.AnimatorListener;

/**
 * Created by amal.chandran on 15/11/17.
 */

//public class AnimationArcHelper implements com.amalbit.trail.contract.Animator {

//  private static AnimationArcHelper singletonInstance;
//
//  private static final int ANIM_DURATION_DEFAULT = 1000;
//
//  private static final int ANIM_DURATION_REPEAT = 1500;
//
//  private AnimatorSet animatorArcSet;
//
//  private AnimatorSet animatorRepeatArcSet;
//
//  private RouteOverlayView mRouteOverlayView;
//
//  private ObjectAnimator firstTimeArcAnimator;
//
//  private ObjectAnimator secondTimeArcAnimator;
//
//  private ValueAnimator colorArcAnimation;
//
//  protected float arcLength;
//
//  protected float shadowLength;
//
//  protected float[] arcdDashValue;
//
//  protected float[] shadowDashValue;
//
//  protected boolean isFirstTimeDrawing;
//
//  protected boolean animStarted;
//
//  public static AnimationArcHelper getInstance(RouteOverlayView routeOverlayView) {
//    if (singletonInstance == null) {
//      singletonInstance = new AnimationArcHelper(routeOverlayView);
//    }
//    return singletonInstance;
//  }
//
//  private AnimationArcHelper(RouteOverlayView routeOverlayView) {
//    this.mRouteOverlayView = routeOverlayView;
//  }
//
//  private void init() {
//
//    if (firstTimeArcAnimator == null) {
//      firstTimeArcAnimator = ObjectAnimator.ofFloat(this, "update", 1f, 0f);
//      firstTimeArcAnimator.setDuration(ANIM_DURATION_DEFAULT);
//      firstTimeArcAnimator.setInterpolator(new DecelerateInterpolator());
//    }
//    firstTimeArcAnimator.addListener(new AnimatorListener() {
//      @Override
//      public void onAnimationStart(Animator animator) {
//        super.onAnimationStart(animator);
//        isFirstTimeDrawing = true;
//        animStarted = true;
//      }
//
//      @Override
//      public void onAnimationEnd(Animator animator) {
//        super.onAnimationEnd(animator);
//        isFirstTimeDrawing = false;
//      }
//    });
//
//    if (secondTimeArcAnimator == null) {
//      secondTimeArcAnimator = ObjectAnimator.ofFloat(this, "update1", 0f, 1f);
//      secondTimeArcAnimator.setDuration(ANIM_DURATION_REPEAT);
//      secondTimeArcAnimator.setInterpolator(new DecelerateInterpolator());
//    }
//
//    if (colorArcAnimation == null) {
//      colorArcAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), mRouteOverlayView.routeSecondaryColor,
//          mRouteOverlayView.routeMainColor);
//      colorArcAnimation.setDuration(ANIM_DURATION_REPEAT); // milliseconds
//      colorArcAnimation.setStartDelay(250);
//    }
//
//    colorArcAnimation.addUpdateListener(animator -> {
//      mRouteOverlayView.paintBottomArc.setColor((int) animator.getAnimatedValue());
//      mRouteOverlayView.invalidate();
//    });
//    colorArcAnimation.addListener(new AnimatorListener() {
//      @Override
//      public void onAnimationEnd(Animator animator) {
//        PathEffect effect = new DashPathEffect(new float[]{arcLength, arcLength}, arcLength);
//        mRouteOverlayView.paintTopArc.setPathEffect(effect);
//        mRouteOverlayView.paintBottomArc.setColor(mRouteOverlayView.routeSecondaryColor);
//        mRouteOverlayView.invalidate();
//      }
//    });
//
//    animatorRepeatArcSet = new AnimatorSet();
//    animatorArcSet = new AnimatorSet();
//
//    animatorRepeatArcSet.playTogether(secondTimeArcAnimator, colorArcAnimation);
//    animatorRepeatArcSet.addListener(new AnimatorListenerAdapter() {
//
//      private boolean mCanceled;
//
//      @Override
//      public void onAnimationStart(Animator animation) {
//        mCanceled = false;
//      }
//
//      @Override
//      public void onAnimationCancel(Animator animation) {
//        mCanceled = true;
//      }
//
//      @Override
//      public void onAnimationEnd(@NonNull Animator animation) {
//        if (!mCanceled) {
//          animation.start();
//        }
//      }
//
//    });
//
//    animatorArcSet.playSequentially(firstTimeArcAnimator, animatorRepeatArcSet);
//    animatorArcSet.addListener(new AnimatorListenerAdapter() {
//      @Override
//      public void onAnimationCancel(Animator animation) {
//        super.onAnimationCancel(animation);
//        animatorRepeatArcSet.cancel();
//      }
//    });
//  }
//
//  public void setUpdate(float update) {
//    PathEffect effect = new DashPathEffect(arcdDashValue, arcLength * update);
//    mRouteOverlayView.paintTopArc.setPathEffect(effect);
//
//    PathEffect shadowEffect = new DashPathEffect(shadowDashValue, shadowLength * update);
//    mRouteOverlayView.paintShadow.setPathEffect(shadowEffect);
//
//    mRouteOverlayView.invalidate();
//  }
//
//  public void setUpdate1(float update) {
//    PathEffect effect = new DashPathEffect(arcdDashValue, -arcLength * update);
//    mRouteOverlayView.paintTopArc.setPathEffect(effect);
//    mRouteOverlayView.invalidate();
//  }
//
//  @Override
//  public void play() {
//    stop(null);
//    init();
//    animatorArcSet.start();
//  }
//
//  @Override
//  public void stop(AnimationCallback callback) {
//    if (animatorArcSet != null) {
//      colorArcAnimation.end();
//      colorArcAnimation.cancel();
//      secondTimeArcAnimator.end();
//      secondTimeArcAnimator.cancel();
//      firstTimeArcAnimator.end();
//      firstTimeArcAnimator.cancel();
//      animatorRepeatArcSet.end();
//      animatorRepeatArcSet.cancel();
//      animatorArcSet.end();
//      animatorArcSet.cancel();
//
//      animatorRepeatArcSet = null;
//      animatorArcSet = null;
//      firstTimeArcAnimator = null;
//      secondTimeArcAnimator = null;
//      colorArcAnimation = null;
//    }
//  }
//}
