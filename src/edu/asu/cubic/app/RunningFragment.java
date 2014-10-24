package edu.asu.cubic.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class RunningFragment extends Fragment implements CvCameraViewListener2 {
	public String selection;
	private Button mStopButton;
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
    private View runningView;
    
    public RunningFragment() {
    	Log.i(TAG, "Instantiated new " + this.getClass());
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "********** In onCreate method *********");
		if (OpenCVLoader.initDebug()) {
			mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		} else {
			Log.d("CVerror","OpenCV library Init failure");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, getActivity(), mLoaderCallback);
		}
		
		Intent intent = getActivity().getIntent();
		selection = intent.getStringExtra(MainActivity.EXTRA_SELECTION);
	}
	
    // Initialize the activity and set the appropriate content view.
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		runningView = inflater.inflate(R.layout.fragment_running, parent, false);
		Log.i(TAG, "********** In onCreateView method in RunningFragment *********");
		
		TextView textView = (TextView) runningView.findViewById(R.id.progressLabel);
		String newText = "The application is now running. The " + selection + " are being captured.";
		textView.setText(newText);
		
		mStopButton = (Button) runningView.findViewById(R.id.btnStop);
		
		mStopButton.setOnClickListener(new OnClickListener() {
	        	@Override
	        	public void onClick(View v) {
	        	        Intent intent = new Intent(getActivity(), MainActivity.class);
	        	        startActivity(intent);
	        	}
		});
		
		return runningView;
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mOpenCvCameraView= null;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		// Create the OpenCV Camera View
		mOpenCvCameraView = (CameraBridgeViewBase) runningView.findViewById(R.id.opencv_camera_view);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.enableView();
	}
	
    public void onDestroy() {
        super.onDestroy();
    }
    
	public void stopThread(View v) {
		//mOpenCvCameraView.disableView();
		getActivity().finish();
	}
	
	// This method is called when OpenCV is initiated.
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(getActivity()) {
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
                        File cascadeDir = getActivity().getDir("cascade", Context.MODE_PRIVATE);
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

    //	This method is invoked when delivery of the frame needs to be done
    @Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
    	long[] slow = {0, 100, 1000};
    	long[] fast = {0, 100, 200, 100, 200, 100, 200};
    	
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
    			//Track where the faces are and vibrate accordingly.
    			double left = (mRgbaT.width())/3;
    			double right = (mRgbaT.width() * 2)/3;
    			double faceCenter;
    			boolean center = false;

    			for (int i = 0; i < facesArray.length; i++) {
    				faceCenter = facesArray[i].tl().x + (facesArray[i].br().x - facesArray[i].tl().x)/2;
    				if (faceCenter > left && faceCenter < right) {
        				center = true;
    				}
    			}
    			if (center) {
    				Log.i(TAG, "\n\nCENTER\n\n");
    				v.vibrate(fast, -1);
    			} else {
    				Log.i(TAG, "\n\nNOT CENTER\n\n");
    				v.vibrate(slow, -1);
    			}
    			
    			// If Faces are detected then add face boundaries
    			String message = "************* Face Detected ************* NumFaces : " + facesArray.length;
    			for (int i = 0; i < facesArray.length; i++) {
    				Core.rectangle(temp, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
    				Core.rectangle(mRgbaT, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
    				message += " facesArray[i].tl(): "+facesArray[i].tl()+" facesArray[i].br(): "+facesArray[i].br();
    			}
    			// Write the image to file
    			Uri uriTarget = getActivity().getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, new ContentValues());
    			message += "\n Writing to " + uriTarget;
    			Log.i(TAG, message);
    		}
    	}
		return mRgbaT;
	}

	public void onCameraViewStarted(int width, int height) {
		Log.i(TAG,"*********** Calling Mat() *********");
		mGray = new Mat();
		mRgba = new Mat();
	}

	public void onCameraViewStopped() {
		mGray.release();
		mRgba.release();
	}
}
