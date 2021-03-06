package com.royole.yogu.mymediaplayer.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.markmao.pulltorefresh.widget.XListView;
import com.royole.yogu.mymediaplayer.R;
import com.royole.yogu.mymediaplayer.utils.FileUtils;
import com.royole.yogu.mymediaplayer.utils.StringUtils;
import com.royole.yogu.videoplayerlibrary.VideoPlayerActivity;
import com.royole.yogu.videoplayerlibrary.model.Video;
import com.google.gson.Gson;

public class MainActivity extends Activity implements XListView.IXListViewListener {

    private String Tag = "MainActivity";

    private XListView mListView;
    private List<Video> mData = new ArrayList<Video>();
    private VedioAdapter mAdapter;
    private Handler mHandler;
    private Gson gson = new Gson();
    private String jsonData;

    // Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    // End Lifecycle

    // Private Method
    private void initView() {
        mHandler = new Handler();
        mListView = (XListView) findViewById(R.id.list_view);
        mListView.setPullLoadEnable(true);
        mListView.setXListViewListener(this);
        mListView.setRefreshTime(StringUtils.getTime());
        mAdapter = new VedioAdapter(this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // because of the header, the position = position - 1
                Log.d(Tag, "position:" + (position - 1));
                Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
                intent.putExtra("path", mData.get(position - 1).getvURL());
                startActivity(intent);
            }
        });
    }

    private void geneItems() {
        jsonData = FileUtils.readAssertJson(this,"video_test.json");
        ArrayList<Video> results = gson.fromJson(jsonData, new TypeToken<List<Video>>(){}.getType());
        mData.addAll(results);
    }

    private void onLoad() {
        mListView.stopRefresh();
        mListView.stopLoadMore();
        mListView.setRefreshTime(StringUtils.getTime());
    }
    // End Private Method

    /**
     * ViewHolder pattern consists in storing a data structure in the tag of the view
     * returned by getView().This data structures contains references to the views we want to bind data to,
     * thus avoiding calling to findViewById() every time getView() is invoked
     */
    static class ViewHolder {
        public ImageView img;
        public TextView title;
        public TextView desc;
    }

    public class VedioAdapter extends BaseAdapter {
        private LayoutInflater mInflater = null;

        private VedioAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            //How many items are in the data set represented by this Adapter.
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            // Get the data item associated with the specified position in the data set.
            return position;
        }

        @Override
        public long getItemId(int position) {
            //Get the row id associated with the specified position in the list.
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            // check convertView buffer
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.vw_list_item, null);
                holder.img = (ImageView) convertView.findViewById(R.id.vedioShotImgView);
                holder.title = (TextView) convertView.findViewById(R.id.titleTextView);
                holder.desc = (TextView) convertView.findViewById(R.id.descTextView);
                // save layout in convertView in order to get by getTag()
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Log.d(Tag, "position:" + position);
//            Log.d(Tag,"Image:"+getResources().getIdentifier(mData.get(position).getImageShotPath(), "drawable", getApplicationInfo().packageName));
//            holder.img.setBackgroundResource(getResources().getIdentifier(mData.get(position).getImageShotPath(), "drawable", getApplicationInfo().packageName));
            InputStream in= null;
            try {
                in = MainActivity.this.getAssets().open(mData.get(position).getImageShotPath()+".jpg");
            } catch (IOException e) {
                e.printStackTrace();
            }
            Bitmap bmp= BitmapFactory.decodeStream(in);
            holder.img.setImageBitmap(bmp);
            holder.title.setText((String) mData.get(position).getvTitle());
            holder.desc.setText((String) mData.get(position).getDesc());

            return convertView;
        }
    }

    // Implement XListView.IXListViewListener
    @Override
    public void onRefresh() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(Tag,"onRefresh()...");
                mData.clear();
                geneItems();
                mAdapter = new VedioAdapter(MainActivity.this);
                mListView.setAdapter(mAdapter);
                onLoad();
            }
        }, 2500);
    }

    @Override
    public void onLoadMore() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(Tag,"onLoadMore()...");
                geneItems();
                mAdapter.notifyDataSetChanged();
                onLoad();
            }
        }, 2500);
    }
    // End XListView.IXListViewListener
}
