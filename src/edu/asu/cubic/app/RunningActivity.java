package edu.asu.cubic.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class RunningActivity extends FragmentActivity {

	
	Fragment runningFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_running);
		
		// Create and commit the running fragment.
		FragmentManager fm = getSupportFragmentManager();
		runningFragment = fm.findFragmentById(R.id.runningContainer);
		
		if (runningFragment == null) {
			runningFragment = new RunningFragment();
			fm.beginTransaction()
				.add(R.id.runningContainer, runningFragment)
				.commit();
		}
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
	
	public void stopThread(View v) throws InterruptedException {
		((RunningFragment) runningFragment).startend();
		Thread.sleep(2000);
		finish();
	}

}
