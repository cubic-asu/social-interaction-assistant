package edu.asu.cubic.app;

import edu.asu.cubic.app.R;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

/* MainActivity is the home screen activity at which point
 * the user can select between "emotions" or "action units"
 * and proceed to RunningActivity.
 * It starts the main menu, allowing user to select between 
 *	two options before starting the face detection segment.
 */
public class MainActivity extends ActionBarActivity {
	public final static String EXTRA_SELECTION = "edu.asu.capstone.SELECTION";

	// Initialize the activity and set the appropriate content view.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }
    
    // Vibrate testing once each radio button is clicked
    public void onRbtnClick(View view){
    	 // Get instance of Vibrator from current Context
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
    	// Start without a delay
    	// Each element then alternates between vibrate, sleep, vibrate, sleep...
    	long[] pattern = {0, 100, 1000, 300, 200, 100, 500, 200, 100};

    	// Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        
        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.rbtnEmotions:
                if (checked)
                	// The '-1' here means to vibrate once
                	// '0' would make the pattern vibrate indefinitely
                	v.vibrate(pattern, -1);
                break;
            case R.id.rbtnActionUnits:
                if (checked)
                	// Vibrate for 1000 milliseconds
                    v.vibrate(1000);
                break;
        }
    }
    public void initiateApplication(View view) {
        RadioButton rbtnActionUnits = (RadioButton) findViewById(R.id.rbtnActionUnits);
        RadioButton rbtnEmotions = (RadioButton) findViewById(R.id.rbtnEmotions);
        // Get instance of Vibrator from current Context
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
         

        boolean checked = true;
        String selection = "";
        
        if (rbtnActionUnits.isChecked()) {
        	selection = rbtnActionUnits.getText().toString();
        } else if (rbtnEmotions.isChecked()) {
        	selection = rbtnEmotions.getText().toString();
        }
        else {
        	checked = false;
        	// Alert the user that they need to select one of the two options.
        	Toast.makeText(this, "Please select an option.", Toast.LENGTH_SHORT).show();
        }
        
        // Once button is clicked, start face detection activity
        // vibrate for 300 milliseconds once button is pressed
        if (checked) {
        	// Vibrate for 300 milliseconds
            v.vibrate(300);
        	Intent intent = new Intent(this, RunningActivity.class);
	        intent.putExtra(EXTRA_SELECTION, selection);
	        startActivity(intent);
        }
    }
}
