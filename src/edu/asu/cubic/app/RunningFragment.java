package edu.asu.cubic.app;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

public class RunningFragment extends Fragment implements CvCameraViewListener2 {
	public String selection;
	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	private ConnectThread connectThread;
	private ConnectedThread connectedThread;
	
	// Create a BroadcastReceiver for ACTION_FOUND
	//was commented out
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            // Get the BluetoothDevice object from the Intent
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            // Add the name and address to an array adapter to show in a ListView
	            Log.i(TAG, device.getName() + "\n" + device.getAddress());
	            if (device.getName() != null && (device.getName().equals("SAIPAVAN-VAIO") || device.getName().equals("THUNDERCAT"))) 
	            {
	            	connect(device);
	            }
	        }
	    }
	};
	//was commented out
	
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
		
		// Register the BroadcastReceiver
		//was commented out
		
    	IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    	getActivity().registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    	
    	if (adapter.isDiscovering())
    		adapter.cancelDiscovery();
    	
    	adapter.startDiscovery();
    	//was commented out
    	
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
		mOpenCvCameraView = (CameraBridgeViewBase) getActivity().findViewById(R.id.opencv_camera_view);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.enableView();
		ViewGroup.LayoutParams lpms = mOpenCvCameraView.getLayoutParams();
		lpms.height = 960;
		lpms.width = 640;
		mOpenCvCameraView.setLayoutParams(lpms);
		Log.i(TAG, "getMinimumHeight: " +mOpenCvCameraView.getMeasuredHeight() + " getMinimumWidth: " +mOpenCvCameraView.getMeasuredWidth());
	}
	
	// The final call you receive before your activity is destroyed
    public void onDestroy() {
        super.onDestroy();
        //was commented out
        try {
    		getActivity().unregisterReceiver(mReceiver);
    	} catch (IllegalArgumentException e) {
    		
    	}
    	adapter.cancelDiscovery();
    	if (connectThread != null)
    		connectThread.cancel();
    	if (connectedThread != null)
    		connectedThread.cancel();
    	//was commented out
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
		
		return runningView;
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
    
    /** Create a File for saving an image or video */
    @SuppressLint("SimpleDateFormat") private  File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this. 
    	Log.i(TAG, "Environment.getExternalStorageState(): "+Environment.getExternalStorageState());
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + File.separator + "DCIM" + File.separator +"Camera");
        
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        } 
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
        File mediaFile;
            String mImageName="MI_"+ timeStamp +".png";
            mediaFile = new File(mediaStorageDir.toString() + File.separator + mImageName);  
        return mediaFile;
    }
    
    private void storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        } 
        try {
        	//pictureFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(pictureFile);
            //image.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
			image.compress(Bitmap.CompressFormat.PNG, 0, baos);     
			byte[] b = baos.toByteArray();
            fos.flush();
            fos.write(b);
            fos.close();
            Log.i(TAG, "Successfully wrote image to file: " + pictureFile.getAbsolutePath() + " "+ image.getHeight() + " , "+image.getWidth());
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }  
    }

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
		
        if (mJavaDetector != null) {
            // Detect Faces
            mJavaDetector.detectMultiScale(temp, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
            new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }

		if(faces != null) {
    		Rect[] facesArray = faces.toArray();
    		if(facesArray.length != 0) {
    			long currCaptureTime = System.currentTimeMillis();
    			//Track where the faces are and vibrate accordingly.
    			double left = (mRgbaT.width())/3;
    			double right = (mRgbaT.width() * 2)/3;
    			//todo find biggest face
    			double faceCenter = facesArray[0].tl().x + (facesArray[0].br().x - facesArray[0].tl().x)/2;
    			if ((currCaptureTime-prevCaptureTime)>DELAY*1000) {
	    			if (faceCenter > left && faceCenter < right) {
	    				Log.i(TAG, "\n\nCENTER\n\n");
	    				v.vibrate(fast, -1);
	    			} else {
	    				Log.i(TAG, "\n\nNOT CENTER\n\n");
	    				v.vibrate(slow, -1);
	    			}
    			}
    			
    			// If Faces are detected then add face boundaries
    			String message = "************* Face Detected ************* NumFaces : " + facesArray.length;
    			
    			// Check if the time between current and previous captures is more than the delay
    			if((currCaptureTime-prevCaptureTime)>DELAY*1000) {
	    			// Write the image to file
    				Bitmap bit = Bitmap.createBitmap(mRgbaT.cols(), mRgbaT.rows(), Bitmap.Config.ARGB_8888);
					Utils.matToBitmap(mRgbaT, bit);
					
					Log.i(TAG, "Sending image");
					storeImage(bit);
    				//this part was commented out
					if (connectedThread != null && connectedThread.connected) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						bit.compress(Bitmap.CompressFormat.PNG, 0, baos);     
						byte[] b = baos.toByteArray();
						connectedThread.write(b);
						Log.i(TAG, "Total bytes written: " + b.length);
						Log.i(TAG, "end of file bytes: "+ "end of file length".getBytes().length);
						connectedThread.write("end of file".getBytes());
						
		    			//message += "\n Writing to " + uriTarget;
    				}
					//commented out
	    			prevCaptureTime= currCaptureTime;
    			}
    			
    			for (int i = 0; i < facesArray.length; i++) {
    				//todo write only biggest face
    				
    				Core.rectangle(mRgbaT, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
    				message += " facesArray[i].tl(): "+facesArray[i].tl()+" facesArray[i].br(): "+facesArray[i].br();
    			}
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
	
	private void connect(BluetoothDevice device) {
    	connectThread = new ConnectThread(device);
    	connectThread.start();
    }
    
    private void connected(BluetoothSocket socket) {
    	connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }
	
    private class ConnectThread extends Thread {
    	private final BluetoothSocket socket;
    	private final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    	
    	public ConnectThread(BluetoothDevice device) {
    		BluetoothSocket tmp = null;
     
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
            } catch (IOException e) {
            }
            
            socket = tmp;
    	}
    	
    	public void run() {
    		// Cancel discovery because it will slow down the connection
    		adapter.cancelDiscovery();
     
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                socket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    socket.close();
                } catch (IOException closeException) { }
                return;
            }
     
            // Reset the ConnectThread because we're done
            synchronized (getActivity()) {
                connectThread = null;
            }

            connected(socket);
    	}
    	
    	public void cancel() {
            try {
                socket.close();
            } catch (IOException e) { }
        }

    }
    
    private class ConnectedThread extends Thread {
    	private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public boolean connected;
     
        public ConnectedThread(BluetoothSocket socket) {
        	connected = false;
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
     
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
     
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
     
        public void run() {
        	connected = true;
            byte[] buffer = new byte[1024];  // buffer store for the stream
            // Keep listening to the InputStream until an exception occurs
            while (true) {
            	try {
                    mmInStream.read(buffer);
                } catch (IOException e) { 
                	break;
                }
            }
        }
        
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }

    }
}
