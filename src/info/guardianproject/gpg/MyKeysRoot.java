package info.guardianproject.gpg;

import java.util.HashMap;
import java.util.Map;

import info.guardianproject.gpg.screens.ViewMyKeysActivity;
import info.guardianproject.gpg.utils.Constants;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MyKeysRoot extends Fragment implements Constants {
	public Map<String, Object> properties;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		properties = new HashMap<String, Object>();
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		
	}
	
	@Override
	public View onCreateView(LayoutInflater li, ViewGroup container, Bundle savedInstanceState) {
		View view = li.inflate(R.layout.my_keys_group, container, false);
		Log.d(LOG, "root container: " + container.getId());
		
		// switch by bundle!
		swapFragment(new ViewMyKeysActivity());
		
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
	}

	private void swapFragment(Fragment newFragment) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(R.id.groupRoot, newFragment);
		ft.addToBackStack(null);
		ft.commit();
	}

}
