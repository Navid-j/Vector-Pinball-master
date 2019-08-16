package com.taak.pinball;

import android.content.Intent;
import android.nfc.Tag;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.billingclient.util.IabHelper;
import com.android.billingclient.util.IabResult;
import com.android.billingclient.util.Inventory;
import com.android.billingclient.util.MarketIntentFactorySDK;
import com.android.billingclient.util.Purchase;
import com.android.billingclient.util.SkuDetails;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
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
                        Toast.makeText(SplashActivity.this,"Error while consuming: " + iabResult , Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(SplashActivity.this,"Error Purchaseing: "+ iabResult,Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Log.d(TAG, "Purchase successful.");
                    Intent intent = new Intent(SplashActivity.this,BouncyActivity.class);
                    startActivity(intent);
                }
            };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Log.d(TAG,"Creating IAB helper");
        mHelper = new IabHelper(this,base64EncodedPublicKey,new MarketIntentFactorySDK(true));

        Log.d(TAG,"Starting Setup");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult iabResult) {
                Log.d(TAG,"Setup Finished");
                if (!iabResult.isSuccess()) {
                    return;
                }
                if (mHelper == null) return;

                try {
                    mHelper.queryInventoryAsync(mqueryInventoryFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }
            }

                });

    }

    private IabHelper.QueryInventoryFinishedListener mqueryInventoryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult iabResult, Inventory inventory) {
            Log.d(TAG,"Query inventory finished.");

            if (mHelper == null) return;
            if (iabResult.isFailure()){
                Log.d(TAG,"failed to query inventory: " + inventory);
                goToIntro();
                return;
            }
            Log.d(TAG,"query inventory was successful");
            Log.d(TAG,"initial inventory query finished. enabling main UI");

            Purchase purchase = inventory.getPurchase("pinballDaily");
            if (purchase == null){
                Intent intent = new Intent(SplashActivity.this , IntroActivityNew.class);
                startActivity(intent);
            }else {
                Intent intent = new Intent(SplashActivity.this , BouncyActivity.class);
                startActivity(intent);
            }

        }
    };

    private void goToIntro() {
        Intent intent = new Intent(SplashActivity.this , IntroActivityNew.class);
        startActivity(intent);
        finish();
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
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destroying helper");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }
}
