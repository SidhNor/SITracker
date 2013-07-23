package com.andrada.sitracker.fragment;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by ggodonoga on 23/07/13.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class StockPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int res = getActivity().getResources().getIdentifier(
                getArguments().getString("resource"), "xml", getActivity().getPackageName());
        addPreferencesFromResource(res);
    }
}
