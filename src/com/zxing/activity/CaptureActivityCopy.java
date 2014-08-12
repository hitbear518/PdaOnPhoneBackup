package com.zxing.activity;

import java.util.Vector;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.widget.Button;

import com.google.zxing.BarcodeFormat;
import com.zsxj.pdaonphone.R;
import com.zxing.camera.CameraManager;
import com.zxing.decoding.CaptureActivityHandler;
import com.zxing.decoding.InactivityTimer;
import com.zxing.view.ViewfinderView;

public class CaptureActivityCopy extends Activity implements SurfaceHolder.Callback {
	
	private CaptureActivityHandler mHandler;
	private ViewfinderView mViewFinderView;
	private boolean mHasSurface;
	private Vector<BarcodeFormat> mDecodeFormats;
	private String mCharacterSet;
	private InactivityTimer mInactivityTimer;
	private MediaPlayer mMediaPlayer;
	private boolean mPlayBeep;
	private boolean mVibrate;
	private Button mCancelScanButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera);
		
		CameraManager.init(getApplication());
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

}
