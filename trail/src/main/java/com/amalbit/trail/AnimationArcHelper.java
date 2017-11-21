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
import javax.security.auth.callback.Callback;

/**
 * Created by amal.chandran on 15/11/17.
 */

public class AnimationArcHelper implements com.amalbit.trail.contract.Animator {

  static AnimationArcHelper ourInstance;

  float length;

  float[] dashValue;

  private AnimatorSet animatorArcSet;
  private AnimatorSet animatorRepeatArcSet;

  //private ValueAnimator hideArcAnimation;

  boolean isFirstTimeDrawing;

  private boolean isAnimating;

  private RouteOverlayView mRouteOverlayView;

  private ObjectAnimator firstTimeArcAnimator;

  private ObjectAnimator secondTimeArcAnimator;

  private ValueAnimator colorArcAnimation;

  public static AnimationArcHelper getInstance(RouteOverlayView routeOverlayView) {
    if( ourInstance == null ) {
      ourInstance = new AnimationArcHelper(routeOverlayView);
    }
    return ourInstance;
  }

  private AnimationArcHelper(RouteOverlayView routeOverlayView) {
    this.mRouteOverlayView = routeOverlayView;
  }

  public void init() {

    if(firstTimeArcAnimator == null) {
      firstTimeArcAnimator = ObjectAnimator.ofFloat(this, "update", 1f, 0f);
      firstTimeArcAnimator.setDuration(1000);
      firstTimeArcAnimator.setInterpolator(new DecelerateInterpolator());
    }
    firstTimeArcAnimator.addListener(new Animator.AnimatorListener() {
      @Override public void onAnimationStart(Animator animator) {
        isFirstTimeDrawing = true;
      }

      @Override public void onAnimationEnd(Animator animator) {
        isFirstTimeDrawing = false;
      }

      @Override public void onAnimationCancel(Animator animator) {

      }

      @Override public void onAnimationRepeat(Animator animator) {

      }
    });

    if(secondTimeArcAnimator == null) {
      secondTimeArcAnimator = ObjectAnimator.ofFloat(this, "update1", 0f, 1f);
      secondTimeArcAnimator.setDuration(1000);
      secondTimeArcAnimator.setInterpolator(new DecelerateInterpolator());
    }

    if(colorArcAnimation == null) {
      colorArcAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), mRouteOverlayView.routeShadwoColor,
          mRouteOverlayView.routeMainColor);
      colorArcAnimation.setDuration(750); // milliseconds
      colorArcAnimation.setStartDelay(450);
    }

    colorArcAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animator) {
        mRouteOverlayView.paintBottomArc.setColor((int)animator.getAnimatedValue());
        mRouteOverlayView.invalidate();
      }
    });
    colorArcAnimation.addListener(new Animator.AnimatorListener() {
      @Override public void onAnimationStart(Animator animator) {

      }

      @Override public void onAnimationEnd(Animator animator) {
        PathEffect effect = new DashPathEffect(new float[] { length, length }, length);
        mRouteOverlayView.paintTopArc.setPathEffect(effect);
        mRouteOverlayView.paintBottomArc.setColor(mRouteOverlayView.routeShadwoColor);
        mRouteOverlayView.invalidate();
      }

      @Override public void onAnimationCancel(Animator animator) {

      }

      @Override public void onAnimationRepeat(Animator animator) {

      }
    });

    //hideArcAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), mRouteOverlayView.routeShadwoColor, mRouteOverlayView.routeMainColor);
    //hideArcAnimation.setDuration(300); // milliseconds
    //hideArcAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
    //  @Override
    //  public void onAnimationUpdate(ValueAnimator animator) {
    //    mRouteOverlayView.paintBottomArc.setColor((int)animator.getAnimatedValue());
    //    mRouteOverlayView.invalidate();
    //  }
    //});
    //hideArcAnimation.addListener(new Animator.AnimatorListener() {
    //  @Override public void onAnimationStart(Animator animator) {
    //
    //  }
    //
    //  @Override public void onAnimationEnd(Animator animator) {
    //    PathEffect effect = new DashPathEffect(new float[] { length, length }, length);
    //    mRouteOverlayView.paintTopArc.setPathEffect(effect);
    //    mRouteOverlayView.paintBottomArc.setColor(mRouteOverlayView.routeShadwoColor);
    //    mRouteOverlayView.invalidate();
    //  }
    //
    //  @Override public void onAnimationCancel(Animator animator) {
    //
    //  }
    //
    //  @Override public void onAnimationRepeat(Animator animator) {
    //
    //  }
    //});

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
      public void onAnimationEnd(Animator animation) {
        if (!mCanceled) {
          animation.start();
        } else {

        }
      }

    });



    animatorArcSet.playSequentially(firstTimeArcAnimator, animatorRepeatArcSet);
    animatorArcSet.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationCancel(Animator animation) {
        super.onAnimationCancel(animation);
        animatorRepeatArcSet.cancel();
      }
    });
  }

  public void setUpdate(float update) {
    PathEffect effect = new DashPathEffect(dashValue, length * update);
    mRouteOverlayView.paintTopArc.setPathEffect(effect);
    mRouteOverlayView.invalidate();
  }

  public void setUpdate1(float update) {
    PathEffect effect = new DashPathEffect(dashValue, -length * update);
    mRouteOverlayView.paintTopArc.setPathEffect(effect);
    mRouteOverlayView.invalidate();
  }

  @Override public void play() {
    if(isAnimating) {
      stop(null);
    }

    init();
    animatorArcSet.start();

    isAnimating = true;
  }
  //if(isAnimating) {
  //  stop(null);
  //}
  //
  //init();
  //  animatorRouteSet.start();
  //
  //isAnimating = true;

  @Override public void stop(AnimationCallback callback) {

    if(animatorArcSet != null) {
      animatorArcSet.end();
      animatorArcSet.cancel();
      firstTimeArcAnimator.end();
      firstTimeArcAnimator.cancel();
      secondTimeArcAnimator.end();
      secondTimeArcAnimator.cancel();
      colorArcAnimation.end();
      colorArcAnimation.cancel();
    }
    isAnimating = false;
  }
}
