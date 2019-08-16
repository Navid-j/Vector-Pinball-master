package com.taak.pinball;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.billingclient.util.IabHelper;
import com.android.billingclient.util.IabResult;
import com.android.billingclient.util.Inventory;
import com.android.billingclient.util.MarketIntentFactorySDK;
import com.android.billingclient.util.Purchase;
import com.android.billingclient.util.SkuDetails;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

public class IntroActivityNew extends AppIntro {


    private static final String TAG = "IntroActivity";
    private final String base64EncodedPublicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCXdSOFSWMEdO/qe0e2EovvrD1jkWyGRv0otDA0apqZXYhOaVsznA3FNNpZYCA++e73Z00oEDxEy0x7zLG7O2n09tedt1Q+pC9hZEYQxyPe3f2Pr/CDj1v6JwL6fSbrCA6QOxYTTghMStaYVn1gyzQfe1jO9tA7bkobvC2ZHu1dJQIDAQAB";

    private IabHelper mHelper;

    final static int  RC_REQUEST = 1001;

    private IabHelper.OnConsumeFinishedListener monConsumeFinishedListener = new
            IabHelper.OnConsumeFinishedListener() {
                @Override
                public void onConsumeFinished(Purchase purchase, IabResult iabResult) {
                    Log.d(TAG,"Consumption finished. Purchase: " + purchase + ", result: " +
                            iabResult);
                    if (mHelper == null) return;

                    if (iabResult.isSuccess()){
                        Log.d(TAG, "Consumption successful. Provisioning.");
                    }else {
                        Toast.makeText(IntroActivityNew.this,"Error while consuming: " + iabResult , Toast.LENGTH_SHORT).show();
                    }
                    Log.d(TAG, "End consumption flow");
                }
            };

    private IabHelper.OnIabPurchaseFinishedListener mIabPurchaseFinishedListener = new
            IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult iabResult, Purchase purchase) {
                    Log.d(TAG,"Purchase finished: " + iabResult + ", purchase: " + purchase);

                    if (mHelper == null) return;
                    if (iabResult.isFailure()){
//                        Toast.makeText(IntroActivityNew.this,"Error Purchaseing: "+ iabResult,Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Log.d(TAG, "Purchase successful.");
                    Intent intent = new Intent(IntroActivityNew.this,BouncyActivity.class);
                    startActivity(intent);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_intro);


        Log.d(TAG,"Creating IAB helper");
        mHelper = new IabHelper(this,base64EncodedPublicKey,new MarketIntentFactorySDK(true));


        addSlide(AppIntroFragment.newInstance(getResources().getText(R.string.intro1_title),
                getResources().getText(R.string.intro1_text),
                R.drawable.icon,
                ContextCompat.getColor(getApplicationContext(),R.color.intro_RedBg)));
        addSlide(AppIntroFragment.newInstance("",
                getResources().getText(R.string.intro2_text),
                R.drawable.wal2,
                ContextCompat.getColor(getApplicationContext(),R.color.intro_BlueBg)));

        showSkipButton(false);
        setBarColor(getResources().getColor(R.color.intro_bar));
        setSeparatorColor(getResources().getColor(R.color.intro_bar));
        setNavBarColor(R.color.intro_bar);

        setDoneText(getResources().getText(R.string.btn_buy));
        doneButton.setBackgroundResource(R.color.intro_doneBtn);


                Log.d(TAG,"Starting Setup");
                mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                    @Override
                    public void onIabSetupFinished(IabResult iabResult) {
                        Log.d(TAG,"Setup Finished");
                        if (!iabResult.isSuccess()) {
                            return;
                        }
                        if (mHelper == null) return;
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destroying helper");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG,"onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        if (!mHelper.handleActivityResult(requestCode, resultCode, data)){
            super.onActivityResult(requestCode,resultCode,data);
        }else {
            Log.d(TAG,"onActivityResult handled by IABUtil.");

        }
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        String payload = "";
        try {
            mHelper.launchSubscriptionPurchaseFlow(IntroActivityNew.this,
                    "pinballDaily",
                    RC_REQUEST,
                    mIabPurchaseFinishedListener,
                    payload);
        }catch (IabHelper.IabAsyncInProgressException e){
            Toast.makeText(IntroActivityNew.this,"Error launching purchase flow. Another async operation" +
                    "in progress",Toast.LENGTH_SHORT).show();
        }

    }
}