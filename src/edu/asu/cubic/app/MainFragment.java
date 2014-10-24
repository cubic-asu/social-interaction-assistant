package edu.asu.cubic.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import edu.asu.cubic.app.R;

public class MainFragment extends Fragment {
	public final static String EXTRA_SELECTION = "edu.asu.capstone.SELECTION";
	private Button mSelectButton;
	private RadioButton mRbtnActionUnits;
	private RadioButton mRbtnEmotions;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		View mainView = inflater.inflate(R.layout.fragment_main, parent, false);
		
		mSelectButton = (Button) mainView.findViewById(R.id.btnSelect);
        mRbtnActionUnits = (RadioButton) mainView.findViewById(R.id.rbtnActionUnits);
        mRbtnEmotions = (RadioButton) mainView.findViewById(R.id.rbtnEmotions);
        
        mSelectButton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		 boolean checked = true;
        	     String selection = "";
        	        
        	     if (mRbtnActionUnits.isChecked()) {
        	        selection = mRbtnActionUnits.getText().toString();
        	     } else if (mRbtnEmotions.isChecked()) {
        	        selection = mRbtnEmotions.getText().toString();
        	     } else {
        	        checked = false;
        	      	// Alert the user that they need to select one of the two options.
        	       	Toast.makeText(getActivity(), "Please select an option.", Toast.LENGTH_SHORT).show();
        	     }
      
        	     if (checked) {
        	        Intent intent = new Intent(getActivity(), RunningActivity.class);
        		    intent.putExtra(EXTRA_SELECTION, selection);
        	        startActivity(intent);
                }
        	}
        });
		
		return mainView;
	}
}