package edu.asu.cubic.app;

import edu.asu.capstone.R;
import android.content.Intent;
import android.os.Bundle;
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

public class MainActivity extends ActionBarActivity {
	public final static String EXTRA_SELECTION = "edu.asu.capstone.SELECTION";
			
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

    public void initiateApplication(View view) {
        RadioButton rbtnActionUnits = (RadioButton) findViewById(R.id.rbtnActionUnits);
        RadioButton rbtnEmotions = (RadioButton) findViewById(R.id.rbtnEmotions);
        
        boolean checked = true;
        String selection = "";
        
        if (rbtnActionUnits.isChecked()) {
        	selection = rbtnActionUnits.getText().toString();
        } else if (rbtnEmotions.isChecked()) {
        	selection = rbtnEmotions.getText().toString();
        }
        else {
        	checked = false;
        	//Somehow alert the user that they need to select one.
        	Toast.makeText(this, "Please select an option.", Toast.LENGTH_SHORT).show();
        }
        
        if (checked) {
        	Intent intent = new Intent(this, RunningActivity.class);
	        intent.putExtra(EXTRA_SELECTION, selection);
	        startActivity(intent);
        }
    }
}
