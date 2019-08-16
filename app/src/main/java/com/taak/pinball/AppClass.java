package com.taak.pinball;

import android.app.Application;

import net.jhoobin.jhub.CharkhoneSdkApp;

public class AppClass extends Application {

    @Override
    public void onCreate(){
        super.onCreate();
        CharkhoneSdkApp.initSdk(this,getSource());
    }

    private String[] getSource() {
        return getResources().getStringArray(R.array.secrets);
    }


}
