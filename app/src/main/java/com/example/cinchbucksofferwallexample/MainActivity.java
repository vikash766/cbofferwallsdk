package com.example.cinchbucksofferwallexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.cbofferwallsdk.CbOfferwall;
import com.example.cbofferwallsdk.CbOfferwallRewardListener;
import com.example.cbofferwallsdk.CbOfferwallSurveyAvailableListener;
import com.example.cbofferwallsdk.CbOfferwallSurveyListener;

public class MainActivity extends AppCompatActivity implements CbOfferwallRewardListener, CbOfferwallSurveyListener, CbOfferwallSurveyAvailableListener  {

    private final String TAG = "CbOfferwall";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //initialize CbOfferwall
        CbOfferwall.initWithApiKeyAndUserIdAndActivityContext("40cdb7704cacbaeb4c4e491f4ece", "ANDROID_TEST_ID", this);

        //customize navigation header
        CbOfferwall.getInstance().setNavigationBarText("Demo App");
        CbOfferwall.getInstance().setNavigationBarColor("#211548");
        CbOfferwall.getInstance().setNavigationBarTextColor("#FFFFFF");

        //set reward and survey status listeners
        CbOfferwall.getInstance().setCbOfferwallRewardListener(this);
        CbOfferwall.getInstance().setCbOfferwallSurveyListener(this);
        CbOfferwall.getInstance().setCbOfferwallSurveyAvailableListener(this);

        Button btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CbOfferwall.getInstance().isSurveyAvailable()) {
                    // for second placement to earn Gems as well as Tokens
//                    CbOfferwall.getInstance().showRewardCenter("66cb0225-3af3-4d63-8920-7a7a9e43abb2");
                    CbOfferwall.getInstance().showRewardCenter();
                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        CbOfferwall.getInstance().onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CbOfferwall.getInstance().onPause();
    }

    @Override
    public void onReward(int i) {
        Log.d(TAG, "onReward: " + i);

    }

    @Override
    public void onRewardCenterClosed() {
        Log.d(TAG, "onRewardCenterClosed");
    }

    @Override
    public void onRewardCenterOpened() {
        Log.d(TAG, "onRewardCenterOpened");
    }

    @Override
    public void cbofferwallSurveyAvailable(boolean surveyAvailable) {
        Log.d(TAG, "CbOfferwall Survey Available: " + surveyAvailable);

    }
}