package com.xiaoai.hlv.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.xiaoai.hlv.R;

public class AdapterMain extends BaseAdapter {

	private Context mContext;

	public AdapterMain(Context context) {
		this.mContext = context;
	}

	@Override
	public int getCount() {
		return 10;
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item, null);
		}
		TextView img = (TextView) convertView.findViewById(R.id.img);
		img.setText("" + position);
		return convertView;
	}

}
