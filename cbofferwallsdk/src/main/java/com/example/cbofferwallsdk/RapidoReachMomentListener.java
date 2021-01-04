package com.example.cbofferwallsdk;

/**
 * Created by thammond on 1/27/18.
 */

public abstract interface RapidoReachMomentListener {
    public abstract void onMomentSurveyOpened();
    public abstract void onMomentSurveyClosed();

    public abstract void onMomentSurveyReceived(int surveyLength);

    public abstract void onMomentSurveyCompleted();
    public abstract void onMomentSurveyNotEligible();
}
