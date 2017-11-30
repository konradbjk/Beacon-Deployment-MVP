package com.example.admin.deploymentmvp;

import android.app.Application;
import android.util.Log;

import com.kontakt.sdk.android.common.KontaktSDK;

public class App extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    KontaktSDK.initialize(this);
    if (KontaktSDK.isInitialized()) {
      Log.d("My Activity","Kontakt SDK initialized.");
    } else {
      Log.e("My Activity", "Kontakt SDK failed to initialize.");
    }

  }
}
