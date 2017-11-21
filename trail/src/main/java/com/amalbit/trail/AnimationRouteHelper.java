package com.amalbit.trail;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.DashPathEffect;
import android.graphics.PathEffect;
import android.view.animation.DecelerateInterpolator;
import com.amalbit.trail.contract.AnimationCallback;

/**
 * Created by amal.chandran on 15/11/17.
 */

public class AnimationRouteHelper implements com.amalbit.trail.contract.Animator {

  static AnimationRouteHelper ourInstance;

  float length;

  float[] dashValue;

  private AnimatorSet animatorRouteSet;

  private AnimatorSet animatorRepeatRouteSet;

  private ObjectAnimator firstTimeRouteAnimator;

  private ObjectAnimator secondTimeRouteAnimator;

  private ValueAnimator colorRouteAnimation;

  boolean isFirstTimeDrawing;

  private boolean isAnimating;

  private RouteOverlayView mRouteOverlayView;

  public static AnimationRouteHelper getInstance(RouteOverlayView routeOverlayView) {
    if( ourInstance == null ) {
      ourInstance = new AnimationRouteHelper(routeOverlayView);
    }
    return ourInstance;
  }

  private AnimationRouteHelper(RouteOverlayView routeOverlayView) {
    this.mRouteOverlayView = routeOverlayView;
  }

  public void init() {

    if(firstTimeRouteAnimator == null) {
      firstTimeRouteAnimator = ObjectAnimator.ofFloat(this, "update", 1f, 0f);
      firstTimeRouteAnimator.setDuration(2000);
      firstTimeRouteAnimator.setInterpolator(new DecelerateInterpolator());
    }

    firstTimeRouteAnimator.addListener(new Animator.AnimatorListener() {
      @Override public void onAnimationStart(Animator animator) { isFirstTimeDrawing = true;
      }
      @Override public void onAnimationEnd(Animator animator) {
        isFirstTimeDrawing = false;
      }

      @Override public void onAnimationCancel(Animator animator) {
      }

      @Override public void onAnimationRepeat(Animator animator) {

      }
    });

    if(secondTimeRouteAnimator == null) {
      secondTimeRouteAnimator = ObjectAnimator.ofFloat(this, "update1", 0f, 1f);
      secondTimeRouteAnimator.setDuration(2000);
      secondTimeRouteAnimator.setInterpolator(new DecelerateInterpolator());
    }

    if(colorRouteAnimation == null) {
      colorRouteAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), mRouteOverlayView.routeShadwoColor,
          mRouteOverlayView.routeMainColor);
      colorRouteAnimation.setDuration(1500); // milliseconds
      colorRouteAnimation.setStartDelay(1000);
    }
    colorRouteAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animator) {
        mRouteOverlayView.paintBottom.setColor((int)animator.getAnimatedValue());
        mRouteOverlayView.invalidate();
      }
    });
    colorRouteAnimation.addListener(new Animator.AnimatorListener() {
      @Override public void onAnimationStart(Animator animator) {

      }

      @Override public void onAnimationEnd(Animator animator) {
        PathEffect effect = new DashPathEffect(new float[] { length, length }, length);
        mRouteOverlayView.paintTop.setPathEffect(effect);
        mRouteOverlayView.paintBottom.setColor(mRouteOverlayView.routeShadwoColor);
        mRouteOverlayView.invalidate();
      }

      @Override public void onAnimationCancel(Animator animator) {

      }

      @Override public void onAnimationRepeat(Animator animator) {

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
        } else {

        }
      }

    });

    animatorRouteSet.playSequentially(firstTimeRouteAnimator, animatorRepeatRouteSet);
    animatorRouteSet.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationCancel(Animator animation) {
        super.onAnimationCancel(animation);
        animatorRepeatRouteSet.cancel();
      }
    });
  }

  public void setUpdate(float update) {
    PathEffect effect = new DashPathEffect(dashValue, length * update);
    mRouteOverlayView.paintTop.setPathEffect(effect);
    mRouteOverlayView.invalidate();
  }

  public void setUpdate1(float update) {
    PathEffect effect = new DashPathEffect(dashValue, -length * update);
    mRouteOverlayView.paintTop.setPathEffect(effect);
    mRouteOverlayView.invalidate();
  }

  @Override public void play() {
    if(isAnimating) {
      stop(null);
    }

    init();
    animatorRouteSet.start();

    isAnimating = true;
  }

  @Override public void stop(AnimationCallback callback) {
    //mRouteOverlayView.clearAnimation();
    if(animatorRouteSet != null) {
      animatorRouteSet.end();
      animatorRouteSet.cancel();
      firstTimeRouteAnimator.end();
      firstTimeRouteAnimator.cancel();
      secondTimeRouteAnimator.end();
      secondTimeRouteAnimator.cancel();
      colorRouteAnimation.end();
      colorRouteAnimation.cancel();
    }
    isAnimating = false;
  }

  public interface Update {
    void onUpdate();
  }
}
