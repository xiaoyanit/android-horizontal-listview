/*
 * HorizontalListView.java v1.5
 *
 * 
 * The MIT License
 * Copyright (c) 2011 Paul Soucy (paul@dev-smart.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.xiaoai.hlv.widget;

import java.util.LinkedList;
import java.util.Queue;

import com.xiaoai.hlv.adapter.AdapterHListView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Debug;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Scroller;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class HorizontalListView extends AdapterView<ListAdapter> {

	private float mInitialX;
	private float mInitialY;

	public boolean mAlwaysOverrideTouch = true;

	protected ListAdapter mAdapter;

	private int mLeftViewIndex = -1;
	private int mRightViewIndex = 0;
	protected int mCurrentX;
	protected int mNextX;
	private int mMaxX = Integer.MAX_VALUE;
	private int mDisplayOffset = 0;

	protected Scroller mScroller;
	private GestureDetector mGesture;

	private Queue<View> mRemovedViewQueue = new LinkedList<View>();

	private OnItemSelectedListener mOnItemSelected;
	private OnItemClickListener mOnItemClicked;
	private OnItemLongClickListener mOnItemLongClicked;

	private boolean mDataChanged = false;

	private MyOnScrollListener onScrollListener;// 设置滑动监听事件

	private static final int FLING_MIN_DISTANCE = 50;
	private static final int FLING_MIN_VELOCITY = 0;

	private boolean isLoading = false;// true表示正在加载,false表示没有加载可以开线程加载

	public HorizontalListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public void setIOnScrollListener(MyOnScrollListener onScrollListener) {
		this.onScrollListener = onScrollListener;
	}

	private synchronized void initView() {
		mLeftViewIndex = -1;
		mRightViewIndex = 0;
		mDisplayOffset = 0;
		mCurrentX = 0;
		mNextX = 0;
		mMaxX = Integer.MAX_VALUE;
		mScroller = new Scroller(getContext());
		mGesture = new GestureDetector(getContext(), mOnGesture);
	}

	@Override
	public void setOnItemSelectedListener(
			AdapterView.OnItemSelectedListener listener) {
		mOnItemSelected = listener;
	}

	@Override
	public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
		mOnItemClicked = listener;
	}

	@Override
	public void setOnItemLongClickListener(
			AdapterView.OnItemLongClickListener listener) {
		mOnItemLongClicked = listener;
	}

	private DataSetObserver mDataObserver = new DataSetObserver() {

		@Override
		public void onChanged() {
			synchronized (HorizontalListView.this) {
				mDataChanged = true;
				isLoading = false;// 数据更新完成,设置标志量为false,表示可以继续更新
			}
			invalidate();
			requestLayout();
		}

		@Override
		public void onInvalidated() {
			reset();
			invalidate();
			requestLayout();
		}

	};

	// 解决点击事件处理不正确Bug
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			mInitialX = ev.getX();
			mInitialY = ev.getY();
			return false;
		case MotionEvent.ACTION_MOVE:
			float deltaX = Math.abs(ev.getX() - mInitialX);
			float deltaY = Math.abs(ev.getY() - mInitialY);
			return (deltaX > 10 || deltaY > 10);
		default:
			return super.onInterceptTouchEvent(ev);
		}
	}
	

	@Override
	public ListAdapter getAdapter() {
		return mAdapter;
	}

	@Override
	public View getSelectedView() {
		return null;
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		if (mAdapter != null) {
			mAdapter.unregisterDataSetObserver(mDataObserver);
		}
		mAdapter = adapter;
		mAdapter.registerDataSetObserver(mDataObserver);
		reset();
	}

	public synchronized void reset() {
		initView();
		removeAllViewsInLayout();
		requestLayout();
	}

	@Override
	public void setSelection(int position) {
	}

	private void addAndMeasureChild(final View child, int viewPos) {
		AdapterView.LayoutParams params = child.getLayoutParams();
		if (params == null) {
			params = new LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.FILL_PARENT);
		}

		addViewInLayout(child, viewPos, params, true);
		child.measure(
				MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST),
				MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
	}

	@Override
	protected synchronized void onLayout(boolean changed, int left, int top,
			int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		if (mAdapter == null) {
			return;
		}

		if (mDataChanged) {
			int oldCurrentX = mCurrentX;
			initView();
			removeAllViewsInLayout();
			mNextX = oldCurrentX;
			mDataChanged = false;
		}

		if (mScroller.computeScrollOffset()) {
			int scrollx = mScroller.getCurrX();
			mNextX = scrollx;
		}

		if (mNextX <= 0) {
			mNextX = 0;
			mScroller.forceFinished(true);
		}
		if (mNextX >= mMaxX) {
			mNextX = mMaxX;
			mScroller.forceFinished(true);
		}

		int dx = mCurrentX - mNextX;

		removeNonVisibleItems(dx);
		fillList(dx);
		positionItems(dx);

		mCurrentX = mNextX;

		if (!mScroller.isFinished()) {
			post(new Runnable() {
				@Override
				public void run() {
					requestLayout();
				}
			});

		}
		
		if (mScroller.isFinished() && isUp) {
			if (!isLoading && mAdapter != null) {
				android.util.Log.e("test",
						"getLastPosition: " + ((AdapterHListView) mAdapter).getLastPosition());
				if (mAdapter instanceof AdapterHListView
						&& ((AdapterHListView) mAdapter)
								.getLastPosition() == mAdapter
								.getCount() - 1) {
					android.util.Log.e("test",
							"********* load more");
					if (onScrollListener != null) {
						isLoading = true;// 设置正在加载,不用加载了!
						onScrollListener.loadMore();
					}
				}
			}
		}
	}
	
	private boolean isUp;

	private void fillList(final int dx) {
		int edge = 0;
		View child = getChildAt(getChildCount() - 1);
		if (child != null) {
			edge = child.getRight();
		}
		fillListRight(edge, dx);

		edge = 0;
		child = getChildAt(0);
		if (child != null) {
			edge = child.getLeft();
		}
		fillListLeft(edge, dx);

	}

	private void fillListRight(int rightEdge, final int dx) {
		while (rightEdge + dx < getWidth()
				&& mRightViewIndex < mAdapter.getCount()) {

			View child = mAdapter.getView(mRightViewIndex,
					mRemovedViewQueue.poll(), this);
			addAndMeasureChild(child, -1);
			rightEdge += child.getMeasuredWidth();

			if (mRightViewIndex == mAdapter.getCount() - 1) {
				mMaxX = mCurrentX + rightEdge - getWidth();
			}

			if (mMaxX < 0) {
				mMaxX = 0;
			}
			mRightViewIndex++;
		}
	}

	private void fillListLeft(int leftEdge, final int dx) {
		while (leftEdge + dx > 0 && mLeftViewIndex >= 0) {
			View child = mAdapter.getView(mLeftViewIndex,
					mRemovedViewQueue.poll(), this);
			addAndMeasureChild(child, 0);
			leftEdge -= child.getMeasuredWidth();
			mLeftViewIndex--;
			mDisplayOffset -= child.getMeasuredWidth();
		}
	}

	private void removeNonVisibleItems(final int dx) {
		View child = getChildAt(0);
		while (child != null && child.getRight() + dx <= 0) {
			mDisplayOffset += child.getMeasuredWidth();
			mRemovedViewQueue.offer(child);
			removeViewInLayout(child);
			mLeftViewIndex++;
			child = getChildAt(0);

		}

		child = getChildAt(getChildCount() - 1);
		while (child != null && child.getLeft() + dx >= getWidth()) {
			mRemovedViewQueue.offer(child);
			removeViewInLayout(child);
			mRightViewIndex--;
			child = getChildAt(getChildCount() - 1);
		}
	}

	private void positionItems(final int dx) {
		if (getChildCount() > 0) {
			mDisplayOffset += dx;
			int left = mDisplayOffset;
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				int childWidth = child.getMeasuredWidth();
				child.layout(left, 0, left + childWidth,
						child.getMeasuredHeight());
				left += childWidth + child.getPaddingRight();
			}
		}
	}

	public synchronized void scrollTo(int x) {
		mScroller.startScroll(mNextX, 0, x - mNextX, 0);
		requestLayout();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			isUp = false;
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			isUp = true;
		}
		return super.onTouchEvent(event);
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		boolean handled = super.dispatchTouchEvent(ev);
		handled |= mGesture.onTouchEvent(ev);
		return handled;
	}

	protected boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		synchronized (HorizontalListView.this) {
			mScroller.fling(mNextX, 0, (int) -velocityX, 0, 0, mMaxX, 0, 0);
		}
		requestLayout();
		return true;
	}
	
	
	protected boolean onDown(MotionEvent e) {
		mScroller.forceFinished(true);
		return true;
	}
	
	
	
	private PointF touchPoint = new PointF();

	private OnGestureListener mOnGesture = new GestureDetector.SimpleOnGestureListener() {

		@Override
		public boolean onDown(MotionEvent e) {
			touchPoint.set(e.getRawX(), e.getRawY());
			return HorizontalListView.this.onDown(e);
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			return HorizontalListView.this
					.onFling(e1, e2, velocityX, velocityY);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
//			Log.e("test", "scrollX: " + getScrollX() + ", scrollY: " + getScrollY());
//			Log.e("test", "e1: " + e1.getX() + ", e2: " + e2.getX() + ", distanceX: " +
//					distanceX + ", distanceY: " + distanceY);
			synchronized (HorizontalListView.this) {
				// mNextX += (int) distanceX;
				mNextX += (int) (touchPoint.x - e2.getRawX());
			}
			touchPoint.set(e2.getRawX(), e2.getRawY());
			requestLayout();
			
			return true;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				if (isEventWithinView(e, child)) {
					if (mOnItemClicked != null) {
						mOnItemClicked.onItemClick(HorizontalListView.this,
								child, mLeftViewIndex + 1 + i,
								mAdapter.getItemId(mLeftViewIndex + 1 + i));
					}
					if (mOnItemSelected != null) {
						mOnItemSelected.onItemSelected(HorizontalListView.this,
								child, mLeftViewIndex + 1 + i,
								mAdapter.getItemId(mLeftViewIndex + 1 + i));
					}
					break;
				}

			}
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			int childCount = getChildCount();
			for (int i = 0; i < childCount; i++) {
				View child = getChildAt(i);
				if (isEventWithinView(e, child)) {
					if (mOnItemLongClicked != null) {
						mOnItemLongClicked.onItemLongClick(
								HorizontalListView.this, child, mLeftViewIndex
										+ 1 + i,
								mAdapter.getItemId(mLeftViewIndex + 1 + i));
					}
					break;
				}

			}
		}

		private boolean isEventWithinView(MotionEvent e, View child) {
			Rect viewRect = new Rect();
			int[] childPosition = new int[2];
			child.getLocationOnScreen(childPosition);
			int left = childPosition[0];
			int right = left + child.getWidth();
			int top = childPosition[1];
			int bottom = top + child.getHeight();
			viewRect.set(left, top, right, bottom);
			return viewRect.contains((int) e.getRawX(), (int) e.getRawY());
		}
	};

	public interface MyOnScrollListener {
		public void loadMore();
	}

	public boolean isLoading() {
		return isLoading;
	}

	public void setLoading(boolean isLoading) {
		this.isLoading = isLoading;
	}

}
