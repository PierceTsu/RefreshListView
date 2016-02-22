package com.pierce.library;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @ 创建者     administrator
 * @ 创建时间   2016/2/21 8:13
 * @ 描述      ${TODO}
 * @ 更新者    $Author$
 * @ 更新时间  2016/2/21
 * @ 更新描述  ${TODO}
 */
public class RefreshListView extends ListView implements AbsListView.OnScrollListener {

    private static final int   STATE_PULL_DOWN          = 0;
    private static final int   STATE_RELEASE_REFRESHING = 1;
    public static final  int   STATE_REFRESHING         = 2;
    private              int   mHiddenHeight            = -1; //初始默认值,标记为初始化状态
    private              float mDownY                   = -1;
    private RotateAnimation mUp2DownAnim;
    private RotateAnimation mDown2UpAnim;
    private ImageView       mRefreshHeaderIvArrow;
    private ProgressBar     mRefreshHeaderPbLoading;
    private TextView        mRefreshHeaderTvState;

    private TextView mRefreshHeaderTvDate;
    private View     mRefreshHeader;

    private View mFooterView;
    //记录刷新的状态
    private int mCurrentState = STATE_PULL_DOWN;

    //用来标记是否加载更多
    private boolean                               isLoadingMore;
    private List<onRefreshAndLoadingMoreListener> mListener;

    private int mHeaderHeight;
    private int mFootHeight;

    public RefreshListView(Context context) {
        this(context, null);
    }

    public RefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //初始化刷新头
        initRefreshHeader();
        //初始化更多的底部
        initMoreFooter();

        //初始化监听集合
        mListener = new ArrayList<>();

        //初始化View动画
        mDown2UpAnim = new RotateAnimation(0, 180
                , Animation.RELATIVE_TO_SELF, 0.5f
                , Animation.RELATIVE_TO_SELF, 0.5f);
        mDown2UpAnim.setDuration(400);
        mDown2UpAnim.setFillAfter(true);    //保持最后的状态

        mUp2DownAnim = new RotateAnimation(0, 180
                , Animation.RELATIVE_TO_SELF, 0.5f
                , Animation.RELATIVE_TO_SELF, 0.5f);
        mUp2DownAnim.setDuration(400);
        mDown2UpAnim.setFillAfter(true);    //保持最后的状态

    }


    private void initRefreshHeader() {
        //添加头
        mRefreshHeader = View.inflate(getContext(), R.layout.refresh_header, null);
        this.addHeaderView(mRefreshHeader);

        //查找控件(使用mRefreshHeader控件对象)
        mRefreshHeaderIvArrow = (ImageView) mRefreshHeader.findViewById(R.id.refresh_header_iv_arrow);
        mRefreshHeaderPbLoading = (ProgressBar) mRefreshHeader.findViewById(R.id.refresh_header_pb_loading);
        mRefreshHeaderTvState = (TextView) mRefreshHeader.findViewById(R.id.refresh_header_tv_state);
        mRefreshHeaderTvDate = (TextView) mRefreshHeader.findViewById(R.id.refresh_header_tv_date);

        //初始进来,刷新头被隐藏,通过设置padding
        //1.先对头进行测量(请求的大小)
        //此处0相当于MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED);
        mRefreshHeader.measure(0, 0);
        //2.获取测量的高
        mHeaderHeight = mRefreshHeader.getMeasuredHeight();
        Log.d("sample:", mHeaderHeight + "");
        mRefreshHeader.setPadding(0, -mHeaderHeight, 0, 0);
    }


    private void initMoreFooter() {
        //添加底部布局
        mFooterView = View.inflate(getContext(), R.layout.refresh_footer, null);
        this.addFooterView(mFooterView);

        //计算footer的高度
        mFooterView.measure(0, 0);
        mFootHeight = mFooterView.getMeasuredHeight();

        //设置ListView的滑动监听,当滑动的条目为最后一个时,显示加载更多
        this.setOnScrollListener(this);
    }


    /**
     * 判断手势,处理触摸事件
     *
     * @param ev
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                float moveY = ev.getY();

                //TODO:定义初始默认值
                if(mDownY == -1){
                    mDownY = moveY;
                }

                float diffY = moveY - mDownY;
                //TODO:如果当前是正在刷新,跳出
                if(mCurrentState == STATE_REFRESHING){
                    break;
                }

                //获取ListView的坐标
                int[] lvLocation = new int[2];
                this.getLocationOnScreen(lvLocation);

                //获取条目在屏幕的坐标
                int[] itemLocation = new int[2];
                View itemView = this.getChildAt(0);
                itemView.getLocationOnScreen(itemLocation);

                //获取第一次的隐藏的高度
                if (mHiddenHeight == -1) {
                    mHiddenHeight = lvLocation[1] - itemLocation[1];
                }

                //当第0个条目可见时,且为为下拉状态时,需要显示:下拉刷新
                if (diffY > 0 && getFirstVisiblePosition() == 0) {

                    //1.刷新头可见(改变top的值)
                    //还需减去隐藏部分的高度
                    int top = (int) (diffY - mHiddenHeight - mHeaderHeight + 0.5f);
                    mRefreshHeader.setPadding(0, top, 0, 0);

                    //临界点时(且不为释放刷新状态),显示为释放刷新
                    if (top > 0 && mCurrentState != STATE_RELEASE_REFRESHING) {
                        Log.d("test","释放刷新");
                        //将当前状态置为释放刷新状态
                        mCurrentState = STATE_RELEASE_REFRESHING;
                        //改变UI的显示
                        refreshStateUI();

                    } else if (top < 0 && mCurrentState != STATE_PULL_DOWN) {
                        Log.d("test", "下拉刷新");
                        mCurrentState = STATE_PULL_DOWN;
                        //没有超过临界点,显示下拉刷新
                        refreshStateUI();
                    }

                    //TODO:判断第一次为隐藏一半的情况
                    if(mHiddenHeight == -1){
                        return true;
                    }else{
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //TODO:重置隐藏的值
                mHiddenHeight = -1;
                mDownY = -1;

                //当为释放刷新时松开,显示为正在刷新
                if (mCurrentState == STATE_RELEASE_REFRESHING) {
                    mCurrentState = STATE_REFRESHING;
                    refreshStateUI();
                    //刷新头刚好完整显示
                    int start = mRefreshHeader.getPaddingTop();
                    int end = 0;
                    doHeaderAnimation(start, end, true);

                } else if (mCurrentState == STATE_PULL_DOWN) {    //下拉刷新时松开,隐藏显示头
                    int start = mRefreshHeader.getPaddingTop();
                    int end = -mHeaderHeight;
                    doHeaderAnimation(start, end, false);
                }
                break;
        }

        return super.onTouchEvent(ev);
    }

    /**
     * 设置头的属性动画
     *
     * @param start       动画开始位置
     * @param end         动画结束位置
     * @param needRefresh 是否需要刷新
     */
    private void doHeaderAnimation(int start, int end, final boolean needRefresh) {

        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        long duration = Math.abs(end - start);
        if (duration > 600) {
            duration = 600;
        }
        animator.setDuration(duration);

        //监听属性改变,获取当前的属性值,并对头的padding进行设置
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                mRefreshHeader.setPadding(0, value, 0, 0);
            }
        });

        //监听动画结束
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //动画结束,如果需要刷新,就通知刷新
                if (needRefresh) {
                    notifyOnRefreshing();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }

    /**
     * 根据状态来改变UI的显示
     */
    private void refreshStateUI() {
        switch (mCurrentState) {
            case STATE_PULL_DOWN:
                mRefreshHeaderTvState.setText("下拉刷新");
                mRefreshHeaderPbLoading.setVisibility(View.INVISIBLE);
                mRefreshHeaderIvArrow.setVisibility(View.VISIBLE);
                //箭头动画由上往下
                mRefreshHeaderIvArrow.startAnimation(mUp2DownAnim);
                break;

            case STATE_RELEASE_REFRESHING:
                mRefreshHeaderTvState.setText("释放刷新");
                mRefreshHeaderPbLoading.setVisibility(View.INVISIBLE);
                //箭头右下往上
                mRefreshHeaderIvArrow.startAnimation(mDown2UpAnim);
                break;

            case STATE_REFRESHING:
                mRefreshHeaderTvState.setText("正在刷新");
                //TODO:清除箭头动画
                mRefreshHeaderIvArrow.clearAnimation();
                mRefreshHeaderIvArrow.setVisibility(View.INVISIBLE);
                mRefreshHeaderPbLoading.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * 刷新结束和加载更多结束
     *
     * @param hasMore 是否需要加载更多数据
     */
    public void refreshFinish(boolean hasMore) {
        //判断当前状态是否为加载更多
        if (isLoadingMore) {
            //将状态置为不需要加载更多
            isLoadingMore = false;
            if (!hasMore) {
                //没有更对数据,将底部隐藏
                mFooterView.setPadding(0, -mFootHeight, 0, 0);
            }
        } else {

            //TODO:刷新的状态改变为下拉刷新,并将头部隐藏
            mCurrentState = STATE_PULL_DOWN;
            refreshStateUI();
            doHeaderAnimation(mRefreshHeader.getPaddingTop(), -mHeaderHeight, false);

            //设置刷新的时间
            mRefreshHeaderTvDate.setText("刷新时间:"+getTime(System.currentTimeMillis()));
            //显示底部的加载更多
            mFooterView.setPadding(0, 0, 0, 0);
        }
    }

    private String getTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd日HH:mm:ss");
        return sdf.format(new Date(time));
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

        //当前状态为正在刷新,就返回
        if (mCurrentState == STATE_REFRESHING) {
            return;
        }

        //如果当前的状态为加载更多,就返回
        if (isLoadingMore) {
            return;
        }

        /**
         * 滑动到最后一个就去加载更多,完成下拉加载更多
         */
        //1.获取当前滑动时,最后一个可见的position
        int lastVisiblePosition = getLastVisiblePosition();
        //2.获取最后的条目
        int maxIndex = getAdapter().getCount() -1;

        //3.判断:滑动可见的最后一个条目为最后的条目,且滑动状态为
        if (lastVisiblePosition == maxIndex && scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
            //滑动到最后.加载更多
            isLoadingMore = true;
            Log.d("test", "加载更多中...");
            notifyOnLoadingMore();
        }

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }


    /**
     * 添加一个刷新和加载更多的观察者
     */

    //暴露接口
    public interface onRefreshAndLoadingMoreListener {
        //刷新数据的回调
        void onRefreshing();

        //加载更多
        void onLoadingMore();
    }

    //添加观察着
    public void addOnRefAndLoadingMoreListener(onRefreshAndLoadingMoreListener listener) {
        if (mListener.contains(listener)) {
            return;
        }
        mListener.add(listener);
    }

    //移除观察者
    public void removeOnRefAndLoadingMoreListener(onRefreshAndLoadingMoreListener listener) {
        mListener.remove(listener);
    }

    //通知刷新
    private void notifyOnRefreshing() {
        Iterator<onRefreshAndLoadingMoreListener> iterator = mListener.iterator();
        while (iterator.hasNext()) {
            onRefreshAndLoadingMoreListener listener = iterator.next();
            listener.onRefreshing();
        }
    }

    //通知加载更多
    private void notifyOnLoadingMore() {
        Iterator<onRefreshAndLoadingMoreListener> iterator = mListener.iterator();
        while (iterator.hasNext()) {
            onRefreshAndLoadingMoreListener listener = iterator.next();
            listener.onLoadingMore();
        }
    }
}
