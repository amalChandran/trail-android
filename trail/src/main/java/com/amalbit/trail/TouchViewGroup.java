package com.amalbit.trail;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class TouchViewGroup extends FrameLayout {

  public interface OnInterceptTouchListener{
    void onTouchEvent(MotionEvent ev);
  }

  private OnInterceptTouchListener onInterceptTouchListener;

  public TouchViewGroup(@NonNull Context context) {
    super(context);
  }

  public TouchViewGroup(@NonNull Context context,
      @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public TouchViewGroup(@NonNull Context context,
      @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (onInterceptTouchListener != null) {
      onInterceptTouchListener.onTouchEvent(ev);
      return true;
    } else {
      return super.dispatchTouchEvent(ev);
    }
  }

  public void setMotionEventListner(OnInterceptTouchListener onInterceptTouchListener) {
    this.onInterceptTouchListener = onInterceptTouchListener;
  }
}