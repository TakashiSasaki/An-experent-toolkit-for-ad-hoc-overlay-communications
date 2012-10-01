package jp.ac.ehime_u.cite.remotecamera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	protected Context context;
	private SurfaceHolder holder;
	protected Camera camera;
	
	CameraPreview(Context context) {
		super(context);
		this.context = context;
		holder = getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("TEST", "surfaceCreated");
		if (camera == null) {
			try {
				camera = Camera.open();
			} catch (RuntimeException e) {
				((Activity)context).finish();
				Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
		if (camera != null) {
			camera.setPreviewCallback(new PreviewCallback() {
				@Override
				public void onPreviewFrame(byte[] data, Camera camera) {
					Log.d("TEST", "onPreviewFrame: preview: data=" + data);
				}
			});
			camera.setOneShotPreviewCallback(new PreviewCallback() {
				@Override
				public void onPreviewFrame(byte[] data, Camera camera) {
					Log.d("TEST", "onPreviewFrame: short preview: data=" + data);
				}
			});
			camera.setErrorCallback(new ErrorCallback() {
				@Override
				public void onError(int error, Camera camera) {
					Log.d("TEST", "onError: error=" + error);
				}
			});
		}
		try {
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			camera.release();
			camera = null;
			((Activity)context).finish();
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d("TEST", "surfaceChanged");
		if (camera == null) {
			((Activity)context).finish();
		} else {
			camera.stopPreview();
			setPictureFormat(format);
			setPreviewSize(width, height);
			setPictureSize(AutoFocus.order_size_x,AutoFocus.order_size_y);
			camera.startPreview();
		}
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("TEST", "surfaceDestroyed");
		if (camera != null) {
			camera.stopPreview();
//			camera.release();
//			camera = null;
		}
	}
	
	protected void setPictureFormat(int format) {
		try {
			Camera.Parameters params = camera.getParameters();
			List<Integer> supported = params.getSupportedPictureFormats();
			if (supported != null) {
				for (int f : supported) {
					if (f == format) {
						params.setPreviewFormat(format);
						camera.setParameters(params);
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void setPreviewSize(int width, int height) {
		Camera.Parameters params = camera.getParameters();
		List<Camera.Size> supported = params.getSupportedPreviewSizes();
		if (supported != null) {
			for (Camera.Size size : supported) {
				if (size.width <= width && size.height <= height) {
					params.setPreviewSize(size.width, size.height);
					camera.setParameters(params);
					break;
				}
			}
		}
	}
	
	// サポートサイズから要求値に最も近い値を設定 <- API.LEVEL8(Android2.2～)
	protected void setPictureSize(int x, int y) {
		Camera.Parameters params = camera.getParameters();
		List<Camera.Size> supported = params.getSupportedPictureSizes();
		if (supported != null) {
			
			int best_size_x = 0;
			int best_size_y = 0;
			int best_size_gosa = Integer.MAX_VALUE;
			int gosa;
			
			for (Camera.Size size : supported) {	Log.d("dede","size:"+size.width+","+size.height);
				// 誤差を取得(単純に差の絶対値を取得)
				gosa = Math.abs(size.width-x) + Math.abs(size.height-y);
				
				// 誤差が最も少ないサイズを記録
				if(gosa < best_size_gosa){
					best_size_x = size.width;
					best_size_y = size.height;
					best_size_gosa = gosa;
				}
			}
			Log.d("dede","bestsize:"+best_size_x+","+best_size_y+",gosa:"+best_size_gosa);
			params.setPictureSize(best_size_x, best_size_y);
			camera.setParameters(params);
		}
	}
	
	protected void setAntibanding(String antibanding) {
		Camera.Parameters params = camera.getParameters();
		List<String> supported = params.getSupportedAntibanding();
		if (supported != null) {
			for (String ab : supported) {
				if (ab.equals(antibanding)) {
					params.setAntibanding(antibanding);
					camera.setParameters(params);
					break;
				}
			}
		}
	}
	
	protected void setColorEffect(String effect) {
		Camera.Parameters params = camera.getParameters();
		List<String> supported = params.getSupportedColorEffects();
		if (supported != null) {
			for (String e : supported) {
				if (e.equals(effect)) {
					params.setColorEffect(effect);
					camera.setParameters(params);
					break;
				}
			}
		}
	}
	
	protected void setFlashMode(String flash_mode) {
		Camera.Parameters params = camera.getParameters();
		List<String> supported = params.getSupportedFlashModes();
		if (supported != null) {
			for (String fm : supported) {
				if (fm.equals(flash_mode)) {
					params.setFlashMode(flash_mode);
					camera.setParameters(params);
					break;
				}
			}
		}
	}
	
	protected void setFocusMode(String focus_mode) {
		Camera.Parameters params = camera.getParameters();
		List<String> supported = params.getSupportedFocusModes();
		if (supported != null) {
			for (String fm : supported) {
				if (fm.equals(focus_mode)) {
					params.setFocusMode(focus_mode);
					camera.setParameters(params);
					break;
				}
			}
		}
	}
	
	protected void setSceneMode(String scene_mode) {
		Camera.Parameters params = camera.getParameters();
		List<String> supported = params.getSupportedSceneModes();
		if (supported != null) {
			for (String sm : supported) {
				if (sm.equals(scene_mode)) {
					params.setSceneMode(scene_mode);
					camera.setParameters(params);
					break;
				}
			}
		}
	}
	
	protected void setWhiteBalance(String white_balance) {
		Camera.Parameters params = camera.getParameters();
		List<String> supported = params.getSupportedWhiteBalance();
		if (supported != null) {
			for (String wb : supported) {
				if (wb.equals(white_balance)) {
					params.setWhiteBalance(white_balance);
					camera.setParameters(params);
					break;
				}
			}
		}
	}
}

