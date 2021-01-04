package com.example.cbofferwallsdk;

import android.os.Bundle;
import android.webkit.WebView;

import com.example.cbofferwallsdk.RapidoReach;
import com.example.cbofferwallsdk.RewardCenterActivity;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by thammond on 1/26/18.
 */

public class MomentSurveyActivity extends RewardCenterActivity {
    private boolean finished = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (RapidoReach.getInstance().isMomentsTitleBarEnabled()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
            getSupportActionBar().hide();
        }
    }

    @Override
    protected void setNavigationButtonsState() {
        super.setNavigationButtonsState();

        WebView view = this._webView;
        URL url = null;
        try {
            url = new URL(view.getUrl());
        } catch (MalformedURLException localMalformedURLException) {

        }

        if (url != null) {
            String path = url.getPath();

            if ((url.getHost().equals("cbofferwall-srv2.kondgekar.com")) || (url.getHost().equals("staging.cbofferwall-srv2.kondgekar.com"))) {
                if (path.contains("moments_result/success")) {
                    if (!finished) {
                        RapidoReach.getInstance().onMomentSurveyCompleted();

                        closeRewardCenter();
                        return;
                    }
                } else if (path.contains("moments_result/term")) {
                    if (!finished) {
                        RapidoReach.getInstance().onMomentSurveyNotEligible();

                        closeRewardCenter();
                        return;
                    }
                } else if (path.contains("moments_result/quit")) {
                    if (!finished) {

                        closeRewardCenter();
                        return;
                    }
                }
            }
        } else {
            closeRewardCenter();
        }
    }

    @Override
    protected void closeRewardCenter() {
        if (!finished) {
            RapidoReach.getInstance().onMomentSurveyClosed();
        }
        finished = true;
        RapidoReach.getInstance().momentSurveyOpen = false;
        finish();
    }


}
