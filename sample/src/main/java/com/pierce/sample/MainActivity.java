package com.pierce.sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pierce.library.RefreshListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements RefreshListView.onRefreshAndLoadingMoreListener {

    private RefreshListView mRefreshListView;
    private List<String>    mDatas;
    private int[] pics = new int[]{
            R.mipmap.pic_1,
            R.mipmap.pic_2,
            R.mipmap.pic_3,
            R.mipmap.pic_4
    };
    private ViewPager      mPager;
    private RefreshAdapter mRefreshAdapter;
    private int mRefreshCount     = 0;
    private int mLoadingMoreCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
        initListener();
    }

    private void initView() {
        mRefreshListView = (RefreshListView) findViewById(R.id.refreshListView);
        //添加自定义头
        View head = View.inflate(this, R.layout.header, null);
        mPager = (ViewPager) head.findViewById(R.id.viewpager);
        mRefreshListView.addHeaderView(head);
    }

    private void initData() {

        //设置ViewPager数据
        mPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                if (pics != null) {
                    return pics.length;
                }
                return 0;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                ImageView iv = new ImageView(MainActivity.this);
                iv.setImageResource(pics[position]);
                iv.setScaleType(ImageView.ScaleType.FIT_XY);
                container.addView(iv);
                return iv;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }
        });


        //初始化数据
        mDatas = new ArrayList<>();

        for (int i = 0; i < 15; i++) {
            mDatas.add("条目-"+i);
        }

        mRefreshAdapter = new RefreshAdapter();
        mRefreshListView.setAdapter(mRefreshAdapter);
    }

    private void initListener() {
        //设置刷新和加载更多监听
        mRefreshListView.addOnRefAndLoadingMoreListener(this);
        //设置点击监听
        mRefreshListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "点击了条目"+(position-2), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRefreshing() {
        //延迟刷新
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //此处须将mDatas重新赋值进行覆盖
                mDatas = new ArrayList<String>();
                for (int i = 0; i < 20; i++) {
                    mDatas.add("第"+mRefreshCount+"次刷新的条目-"+i);
                }
                //更新ListView
                mRefreshAdapter.notifyDataSetChanged();

                mRefreshCount++;

                //TODO:改变刷新状态
                mRefreshListView.refreshFinish(true);
                mLoadingMoreCount = 0;
            }
        },1500);
    }

    @Override
    public void onLoadingMore() {
        //Handler发送延迟
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mLoadingMoreCount >= 3){
                    Toast.makeText(MainActivity.this, "没有更多数据了", Toast.LENGTH_SHORT).show();
                    mRefreshListView.refreshFinish(false);
                }else{

                    //模拟添加数据
                    List<String> moreList = new ArrayList<String>();
                    for (int i = 0; i < 10; i++) {
                        moreList.add("更多数据-"+(mDatas.size()+i));
                    }

                    mLoadingMoreCount++;

                    //向先前集合中添加数据
                    mDatas.addAll(moreList);
                    //通知数据刷新
                    mRefreshAdapter.notifyDataSetChanged();
                    mRefreshListView.refreshFinish(true);
                }
            }
        },1500);
    }

    private class RefreshAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            if (mDatas != null) {
                return mDatas.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (mDatas != null) {
                return mDatas.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, android.view.View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = View.inflate(MainActivity.this,R.layout.item,null);
                holder = new ViewHolder();
                holder.tv = (TextView) convertView.findViewById(R.id.item_tv);
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder) convertView.getTag();
            }

            //设置数据
            holder.tv.setText(mDatas.get(position));
            return convertView;
        }
    }

    private class ViewHolder{
        TextView tv;
    }
}
