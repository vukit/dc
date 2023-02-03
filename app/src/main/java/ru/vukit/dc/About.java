package ru.vukit.dc;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

@Keep
public class About extends Fragment {

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(getResources().getString(R.string.action_about));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String version;
        PackageInfo packageinfo;
        View view = inflater.inflate(R.layout.about, container, false);
        (view.findViewById(R.id.instruction_url)).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.action_instruction_url)))));
        (view.findViewById(R.id.privacy_policy_url)).setOnClickListener((v) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.action_privacy_policy_url)))));
        (view.findViewById(R.id.license_url)).setOnClickListener((v) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.action_license_url)))));
        (view.findViewById(R.id.all_projects_url)).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.action_all_projects_url)))));
        try {
            packageinfo = requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(), 0);
            version = getString(R.string.app_version) + " " + packageinfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "";
        }
        ((TextView) view.findViewById(R.id.app_version)).setText(version);
        (view.findViewById(R.id.vendorName)).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.action_vendor_url)))));
        return view;
    }

}
