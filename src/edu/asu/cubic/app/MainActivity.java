package edu.asu.cubic.app;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {
	public final static String EXTRA_SELECTION = "edu.asu.capstone.SELECTION";
	private BluetoothAdapter mBA;
	private BluetoothSocket mBTSocket;
	private Set<BluetoothDevice> mPairedDevices;
	private OutputStream mOutStream = null;
	private String address = "00:FF:CD:69:AC:09";
	// MAC Address of Brandan's computer 00-FF-CD-69-AC-09
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	//private ListView mListView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Initialize Bluetooth Adapter.
		mBA = BluetoothAdapter.getDefaultAdapter();
		checkBTState();
		
		mPairedDevices = mBA.getBondedDevices();
		ArrayList<String> list = new ArrayList<String>();
		for (BluetoothDevice bt : mPairedDevices) {
			list.add(bt.getAddress());
		}
		
		// Choose a device to use. For testing purposes pick the first.
		address = list.get(0);
		Toast.makeText(getApplicationContext(), address, Toast.LENGTH_SHORT).show();
		BluetoothDevice device = mBA.getRemoteDevice(address);
		
		try {
			mBTSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
		} catch (IOException e) {
			AlertBox("Fatal Error", "In onCreate() and socket create failed: " + e.getMessage() + ".");
		}
		
		mBA.cancelDiscovery();
		
		// Establish the connection. This is a blocking process.
		try {
			mBTSocket.connect();
		} catch (IOException e) {
			try {
				mBTSocket.close();
			} catch (IOException e2) {
				AlertBox("Fatal Error", "In onCreate() and unable to close socket during connection failure" + e2.getMessage() + ".");
			}
		}
		
		// Create a data stream so we can talk to the server.
		try {
			mOutStream = mBTSocket.getOutputStream();
		} catch (IOException e) {
			AlertBox("Fatal Error", "In onCreate() and output stream creation failed: " + e.getMessage() + ".");
		}
		
		// Test sending a message
		String message = "Hello from our app!";
		byte[] msgBuffer = message.getBytes();
		try {
			//boolean i = true;
			//while (i) {
				//mOutStream = mBTSocket.getOutputStream();
				//Thread.sleep(1000);
			//Uri uriTarget = this.getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, new ContentValues());
			mOutStream.write(msgBuffer);
			//mOutStream.flush();
			//}
		} catch (IOException e) {
			String msg = "In onCreate() and an exception occurred during write: " + e.getMessage();
			msg = msg + ".\n\nThe MAC address recorded is: " + address + ".\n\nCheck that the SPP UUID is: " + MY_UUID.toString();
			
			AlertBox("Fatal Error", msg);
		}
		
		// Create and commit the main fragment.
		FragmentManager fm = getSupportFragmentManager();
		Fragment mainFragment = fm.findFragmentById(R.id.fragmentContainer);
		
		if (mainFragment == null) {
			mainFragment = new MainFragment();
			fm.beginTransaction()
				.add(R.id.fragmentContainer, mainFragment)
				.commit();
		}
	}
	
	public void cancel() {
		try {
			mBTSocket.close();
		} catch (IOException e) {
			
		}
	}
	
	private void checkBTState() {
		if (!mBA.isEnabled()) {
			Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(turnOn, 0);
		}
	}
	
	public void AlertBox( String title, String message ){
	    new AlertDialog.Builder(this)
	    .setTitle( title )
	    .setMessage( message + " Press OK to exit." )
	    .setPositiveButton("OK", new OnClickListener() {
	        public void onClick(DialogInterface arg0, int arg1) {
	          finish();
	        }
	    }).show();
	  }
	
	/*public void list(View view) {
		mPairedDevices = mBA.getBondedDevices();

		ArrayList list = new ArrayList();
		for (BluetoothDevice bt: mPairedDevices) {
			list.add(bt.getName());
		}
		Toast.makeText(getApplicationContext(), "Showing Paired Devices", Toast.LENGTH_SHORT).show();
		final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
		//lv.setAdapter(adapter);
	}*/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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
}
