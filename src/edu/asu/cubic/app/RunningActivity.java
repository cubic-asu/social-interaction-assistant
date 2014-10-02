package edu.asu.cubic.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import edu.asu.cubic.app.R;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

/*
 * RunningActivity is the activity in which photos are being taken at a set interval
 * and are sent to the processing device. OpenCV is used for face detection to ensure
 * there is a face within the frame before taking a picture.
 */
public class RunningActivity extends ActionBarActivity implements CvCameraViewListener2{
	public String selection;
	// Variables are needed for OpenCV Camera API
	private static final String    TAG                 = "Capstone::CameraActivity";
	private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
	private Mat                    mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;
    private int                    mAbsoluteFaceSize   = 0;
    private static CameraBridgeViewBase   mOpenCvCameraView;
    // Variables needed for timer.
    private static long DELAY = 1; // in seconds
    private long prevCaptureTime= 0;
    public RunningActivity() {
    	Log.i(TAG, "Instantiated new " + this.getClass());
    }
    
    // Initialize the activity and set the appropriate content view.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "********** In onCreate method *********");
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mLoaderCallback);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_running);
        
		Intent intent = getIntent();
		selection = intent.getStringExtra(MainActivity.EXTRA_SELECTION);
		
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		//mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_camera_view);
		//mOpenCvCameraView.setCvCameraViewListener(this);
		//startApplication();
	}

	// Called when the activity is no longer visible to the user
	// ie, because another activity has been resumed and is covering this one 
	@Override
	public void onStop() {
		super.onStop();
		mOpenCvCameraView= null;
	}
	
	// Called when the system is about to start resuming a previous activity - 
	//		pauses the cameraView
	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
	}
	
	// Called when the activity will start interacting with the user
	@Override
	public void onResume() {
		super.onResume();
		// Create the OpenCV Camera View
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_camera_view);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.enableView();
	}
	
	// The final call you receive before your activity is destroyed
    public void onDestroy() {
        super.onDestroy();
        //finish();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.running, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_running,
					container, false);
			TextView view = (TextView) rootView.findViewById(R.id.progressLabel);
			RunningActivity activity = (RunningActivity) getActivity();
			String newText = "The application is now running. The " + activity.selection + " are being captured.";
			Log.i(TAG, "In PlaceholderFragment onCreateView");
			view.setText(newText);
			return rootView;
		}
	}
	
	public void stopThread(View v) {
		//mOpenCvCameraView.disableView();
		finish();
	}
	
	// This method is called when OpenCV is initiated.
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
        	Log.i(TAG, "********** In onManagerConnected *********");
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "********** OpenCV loaded successfully *********");
                    
                    // Load native library after(!) OpenCV initialization
                    // System.loadLibrary("detection_based_tracker");

                    try {
                    	
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "******** Failed to load cascade classifier *******");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "******** Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    
                } break;
                default:
                {	Log.e(TAG, "********** OpenCV loading failed *********");
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    
   
	/* 
	 * Implement the following methods from CvCameraViewListener2 class
	 */
    
    //	This method is invoked when delivery of the frame needs to be done
    @Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();// a color image
		mGray = inputFrame.gray();// a grayscale image
		MatOfRect faces = new MatOfRect();
		// Since the image is captured as landscape, turn the images to portrait
		Mat temp = mGray.clone();
		Core.transpose(temp, temp);
        Core.flip(temp, temp, 1);
        Imgproc.resize(temp, temp, mGray.size());
        
        Mat mRgbaT = mRgba.t();
		Core.flip(mRgbaT, mRgbaT, 1);
		Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());
		
		long currCaptureTime = System.currentTimeMillis();
		// Check if the time between current and previous captures is more than the delay
		if((currCaptureTime-prevCaptureTime)>DELAY*1000) {
            if (mJavaDetector != null) {
            	// Detect Faces
            	mJavaDetector.detectMultiScale(temp, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
            			new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
            }
            prevCaptureTime= currCaptureTime;
		}
		
		if(faces != null) {
    		Rect[] facesArray = faces.toArray();
    		if(facesArray.length != 0) {
    			// If Faces are detected then add face boundaries
    			String message = "************* Face Detected ************* NumFaces : " + facesArray.length;
    			for (int i = 0; i < facesArray.length; i++) {
    				Core.rectangle(temp, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
    				Core.rectangle(mRgbaT, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
    				message += " facesArray[i].tl(): "+facesArray[i].tl()+" facesArray[i].br(): "+facesArray[i].br();
    			}
    			// Write the image to file
    			Uri uriTarget = getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, new ContentValues());
    			message += "\n Writing to " + uriTarget;
    			Log.i(TAG, message);
    		}
    	}
		return mRgbaT;
	}

	// implemented abstract method inherited from CvCameraViewListener2
    // This method is invoked when camera preview has started. After this method is invoked the frames 
    // 		will start to be delivered to client via the onCameraFrame() callback.
	public void onCameraViewStarted(int width, int height) {
		Log.i(TAG,"*********** Calling Mat() *********");
		mGray = new Mat();
		mRgba = new Mat();
	}
	
	// implemented abstract method inherited from CvCameraViewListener2
	// This method is invoked when camera preview has been stopped for some reason. No frames will be 
	//		delivered via onCameraFrame() callback after this method is called
	public void onCameraViewStopped() {
		mGray.release();
		mRgba.release();
	}
	
}
