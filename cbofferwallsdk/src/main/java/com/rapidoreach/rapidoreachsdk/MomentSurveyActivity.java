package com.rapidoreach.rapidoreachsdk;

import android.os.Bundle;
import android.webkit.WebView;

import com.rapidoreach.rapidoreachsdk.RapidoReach;
import com.rapidoreach.rapidoreachsdk.RewardCenterActivity;

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
            if ((url.getHost().equals(RapidoReach.APIUrl)) || (url.getHost().equals("staging."+RapidoReach.APIUrl))) {
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
