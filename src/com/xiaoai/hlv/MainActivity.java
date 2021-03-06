package com.xiaoai.hlv;

import com.xiaoai.hlv.adapter.AdapterMain;
import com.xiaoai.hlv.widget.HorizontalListView;

import android.os.Bundle;
import android.app.Activity;

public class MainActivity extends Activity {

	private HorizontalListView hlv;
	
	private AdapterMain mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		hlv = (HorizontalListView) findViewById(R.id.hlv);
		mAdapter = new AdapterMain(this);
		hlv.setAdapter(mAdapter);
	}

}
