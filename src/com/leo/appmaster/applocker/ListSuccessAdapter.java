package com.leo.appmaster.applocker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.leo.appmaster.R;
import com.leo.appmaster.engine.AppLoadEngine;
import com.leo.appmaster.model.AppInfo;
import com.leo.appmaster.ui.MaterialRippleLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qili on 15-10-10.
 */
public class ListSuccessAdapter extends BaseAdapter {
    private List<AppInfo> mList;
    private String mFlag;
    private Context mContext;
    private LayoutInflater layoutInflater;

    public ListSuccessAdapter(Context mContext) {
        this.mContext = mContext;
        mList = new ArrayList<AppInfo>();
        layoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int i) {
        return mList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ListLockItem itemView = (ListLockItem) view;
        if (itemView == null) {
            itemView = (ListLockItem) layoutInflater.inflate(R.layout.item_success_lock_app, null);
        } else {
            itemView = (ListLockItem) view;
        }

        AppInfo info = mList.get(i);
        itemView.setFlag("recomment_activity");
        itemView.setIcon(info.icon);
        itemView.setTitle(info.label);
//        if (info.topPos > -1) {
        itemView.setDesc(info, info.isLocked);
//        } else {
//            itemView.setDesc(info);
//        }

        itemView.setLockView(info.isLocked);

        itemView.setInfo(info);
        return itemView;
    }


    public void setData(ArrayList<AppInfo> resault) {
        mList = resault;

        notifyDataSetChanged();
    }

    public void setFlag(String fromDefaultRecommentActivity) {
        mFlag = fromDefaultRecommentActivity;
    }

}