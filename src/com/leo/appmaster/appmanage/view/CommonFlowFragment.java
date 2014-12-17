package com.leo.appmaster.appmanage.view;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.leo.appmaster.R;
import com.leo.appmaster.appmanage.AppListActivity;
import com.leo.appmaster.backup.AppBackupRestoreManager;
import com.leo.appmaster.backup.AppBackupRestoreManager.AppBackupDataListener;
import com.leo.appmaster.model.AppItemInfo;
import com.leo.appmaster.model.BaseInfo;
import com.leo.appmaster.model.BusinessItemInfo;
import com.leo.appmaster.ui.LockImageView;
import com.leo.imageloader.DisplayImageOptions;
import com.leo.imageloader.ImageLoader;
import com.leo.imageloader.core.ImageScaleType;

public class CommonFlowFragment extends BaseFolderFragment implements
		OnItemClickListener {

	private GridView mContentGrid;
	private View mRecommendLayout, mHolder;
	private GridView mRecommendGrid;

	private List<AppItemInfo> mContentData;
	private List<BusinessItemInfo> mRecommendData;
	public LayoutInflater mInflater;

	private ContentAdapter mContentAdapter;
	private RecommendAdapter mRecommendAdapter;
	private DisplayImageOptions mDisplayerOption;
	private ImageLoader mImageLoader;

	@Override
	protected int layoutResourceId() {
		return R.layout.fragment_folder_base;
	}

	@Override
	protected void onInitUI() {
		mImageLoader = ImageLoader.getInstance();
		mDisplayerOption = new DisplayImageOptions.Builder()
				.imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
				.showImageOnLoading(R.drawable.default_icon)
				.showImageOnFail(R.drawable.default_icon).cacheInMemory(true)
				.cacheOnDisk(true).considerExifParams(true).build();

		mInflater = LayoutInflater.from(mActivity);
		mHolder = findViewById(R.id.holder);
		mHolder.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				((AppListActivity) mActivity).getFolderLayer().closeFloder();
			}
		});
		// mIconLayout = (RelativeLayout) findViewById(R.id.iv_app_icon_layout);
		mContentGrid = (GridView) findViewById(R.id.content_gridview);
		mRecommendLayout = findViewById(R.id.recommend_layout);
		mRecommendGrid = (GridView) findViewById(R.id.recommend_gridview);

		mContentGrid.setOnItemClickListener(this);
		mRecommendGrid.setOnItemClickListener(this);
	}

	public void updateData(List<AppItemInfo> contentData,
			List<BusinessItemInfo> reccommendData) {

		mContentData = contentData;
		mRecommendData = reccommendData;

		mContentAdapter = new ContentAdapter(contentData);
		if(mContentGrid != null) {
		      mContentGrid.setAdapter(mContentAdapter);

		        if (mRecommendData != null && !mRecommendData.isEmpty()) {
		            mRecommendLayout.setVisibility(View.VISIBLE);
		            mRecommendAdapter = new RecommendAdapter(reccommendData);
		            mRecommendGrid.setAdapter(mRecommendAdapter);
		        } else {
		            mRecommendLayout.setVisibility(View.INVISIBLE);
		        }
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	private class ContentAdapter extends BaseAdapter {
		List<AppItemInfo> mList;

		public ContentAdapter(List<AppItemInfo> list) {
			super();
			mList = list;
		}

		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public Object getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.app_item, null);
			}

			LockImageView imageView = (LockImageView) convertView
					.findViewById(R.id.iv_app_icon);
			TextView textView = (TextView) convertView
					.findViewById(R.id.tv_app_name);
			BaseInfo info = mList.get(position);

			imageView.setImageDrawable(info.icon);
			textView.setText(info.label);
			convertView.setTag(info);
			return convertView;
		}
	}

	private class RecommendAdapter extends BaseAdapter {
		List<BusinessItemInfo> mList;

		public RecommendAdapter(List<BusinessItemInfo> list) {
			super();
			mList = list;
		}

		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public Object getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.app_item, null);
			}

			LockImageView imageView = (LockImageView) convertView
					.findViewById(R.id.iv_app_icon);
			TextView textView = (TextView) convertView
					.findViewById(R.id.tv_app_name);
			BusinessItemInfo info = mList.get(position);

			mImageLoader
					.displayImage(info.iconUrl, imageView, mDisplayerOption);

			textView.setText(info.label);
			convertView.setTag(info);
			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		((AppListActivity) mActivity).handleItemClick(view, mType);
	}
}
