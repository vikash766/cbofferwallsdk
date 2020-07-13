package com.example.cbofferwallsdk;

import android.os.Bundle;
import android.webkit.WebView;

import com.example.cbofferwallsdk.CbOfferwall;
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
        if (CbOfferwall.getInstance().isMomentsTitleBarEnabled()) {
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

            if ((url.getHost().equals("theoremreach.com")) || (url.getHost().equals("staging.theoremreach.com"))) {
                if (path.contains("moments_result/success")) {
                    if (!finished) {
                        CbOfferwall.getInstance().onMomentSurveyCompleted();

                        closeRewardCenter();
                        return;
                    }
                } else if (path.contains("moments_result/term")) {
                    if (!finished) {
                        CbOfferwall.getInstance().onMomentSurveyNotEligible();

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
            CbOfferwall.getInstance().onMomentSurveyClosed();
        }
        finished = true;
        CbOfferwall.getInstance().momentSurveyOpen = false;
        finish();
    }


}
