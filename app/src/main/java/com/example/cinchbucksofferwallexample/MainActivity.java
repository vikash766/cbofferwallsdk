package com.example.cinchbucksofferwallexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.rapidoreach.rapidoreachsdk.RapidoReach;
import com.rapidoreach.rapidoreachsdk.RapidoReachRewardListener;
import com.rapidoreach.rapidoreachsdk.RapidoReachSurveyAvailableListener;
import com.rapidoreach.rapidoreachsdk.RapidoReachSurveyListener;

public class MainActivity extends AppCompatActivity implements RapidoReachRewardListener, RapidoReachSurveyListener, RapidoReachSurveyAvailableListener  {

    private final String TAG = "RapidoReach";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //initialize RapidoReach
        RapidoReach.initWithApiKeyAndUserIdAndActivityContext("d5ece53df8ac97409298325fec81f3f7", "ANDROID_TEST_ID", this);

        //customize navigation header
        RapidoReach.getInstance().setNavigationBarText("Demo App");
        RapidoReach.getInstance().setNavigationBarColor("#211548");
        RapidoReach.getInstance().setNavigationBarTextColor("#FFFFFF");

        //set reward and survey status listeners
        RapidoReach.getInstance().setRapidoReachRewardListener(this);
        RapidoReach.getInstance().setRapidoReachSurveyListener(this);
        RapidoReach.getInstance().setRapidoReachSurveyAvailableListener(this);

        Button btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Button is clicked");
                if (RapidoReach.getInstance().isSurveyAvailable()) {
                    // for second placement to earn Gems as well as Tokens
//                    RapidoReach.getInstance().showRewardCenter("66cb0225-3af3-4d63-8920-7a7a9e43abb2");
                    RapidoReach.getInstance().showRewardCenter();
                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        RapidoReach.getInstance().onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        RapidoReach.getInstance().onPause();
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
    public void rapidoReachSurveyAvailable(boolean surveyAvailable) {
        Log.d(TAG, "RapidoReach Survey Available: " + surveyAvailable);

    }
}