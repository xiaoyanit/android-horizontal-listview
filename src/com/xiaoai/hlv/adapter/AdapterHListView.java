package com.xiaoai.hlv.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class AdapterHListView extends BaseAdapter {

	protected int lastPosition;
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		this.lastPosition = position;
		// TODO Auto-generated method stub
		return null;
	}

	public int getLastPosition() {
		return this.lastPosition;
	}
}
