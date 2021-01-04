package com.rapidoreach.rapidoreachsdk;

/**
 * Created by sudarshan on 15/07/2020.
 */

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.InvalidParameterException;
import java.util.Timer;
import java.util.TimerTask;

public class RapidoReach {
    private static RapidoReach _instance;
    private RapidoReachRewardListener _rewardListener;
    private RapidoReachSurveyListener _surveyListener;
    private RapidoReachSurveyAvailableListener _surveyAvailableListener;
    private RapidoReachMomentListener _momentListener;
    private String _apiKey = "";
    private String _userId = "";
    private String _surveyUrl = "";
    private WeakReference<Activity> _parentContext;
    private String _googleAdvertiserId;
    private String _unityObjectName;
    private String _carrier;
    private String _osVersion;
    private String _appDevice;
    private String _connectionType;
    private String _appuserId = "";
    private int _awardedContent = 0;
    private boolean surveyAvailable = false;
    private int momentSurveyLength = 999999;
    private String momentEntryURL = "";
    private boolean isProfiled = false;
    private int unityEarnedContent = 0;
    public boolean rewardCenterOpen = false;
    public boolean momentSurveyOpen = false;
    public boolean resetProfiler = false;
    private boolean initialized = false;
    private String navigationBarColor = "#211548";
    private String navigationBarTextColor = "#FFFFFF";
    private String navigationBarText = "";
    private String overrideCloseButtonURL;
    private String overrideRefreshButtonURL;
    private Drawable overrideCloseButton;
    private Drawable overrideRefreshButton;
    private int momentsPollingFrequency = -1;
    private boolean unity = false;
    private String placementId = "";

    private boolean momentsEnabled = false;
    private boolean momentsTitleBarEnabled = false;
    private boolean fetchingMomentSurvey = false;
    private boolean momentSurveyAvailable = false;

    private String rewardIds = "";
    public static String SDK_VERSION = BuildConfig.VERSION_NAME;
    private boolean fetchingAppuserId = false;
    private Timer refreshAvailableSurvey;
    private TRTimer refreshAvailableSurveyTask;

    private final String TAG = "RapidoReach";

    public static RapidoReach getInstance()
    {
        if (_instance == null)
        {
            _instance = new RapidoReach();
        }
        return _instance;
    }

    public static RapidoReach initWithApiKeyAndUserIdAndActivityContext(String apiKey, String userId, Activity parentActivity)  {
        getInstance().setup(apiKey, userId, parentActivity);
        getInstance().setNavigationBarText("RapidoReach");

        return getInstance();
    }

    // TODO: is the ThreadPolicy global? Enforce this in a demo app.

    public void setup(String apiKey, String userId, Activity parentActivity) {

        getInstance().setParentActivityContext(parentActivity);
        getInstance().setUserId(userId);
        getInstance().setApiKey(apiKey);
        getInstance().setCarrier(parentActivity);
        getInstance().setConnectionType(parentActivity);
        getInstance().setAppDevice();
        getInstance().setOsVersion();
        getInstance().setGoogleAdvertiserId();
    }

    public void showMomentSurvey(){
        if (!confirmConnectivity()) return;

        if (!RapidoReach.getInstance().momentSurveyAvailable) return;

        RapidoReach.getInstance().momentSurveyAvailable = false;

        momentSurveyOpen = true;

        onMomentSurveyOpened();

        setDisplaySettings();
        generateSurveyEntryURL();
        if (this._parentContext != null && this._parentContext.get() != null) {
            Intent intent = new Intent(this._parentContext.get(), MomentSurveyActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            this._parentContext.get().startActivity(intent);
        }
    }

    private boolean confirmConnectivity() {
        if (RapidoReach.getInstance().getAppuserId() == null || RapidoReach.getInstance()._googleAdvertiserId == null) {
            RapidoReach.getInstance().setGoogleAdvertiserId();
            return false;
        }

        if (!checkConnectionStatus()) return false;

        return true;
    }

    public void setPlacementId(String placementId) {
        this.placementId = placementId;
    }

    public void showRewardCenter(String placementId) {
        this.setPlacementId(placementId);

        showRewardCenter();
    }

    public void showRewardCenter()
    {
        if (!confirmConnectivity()) return;

        rewardCenterOpen = true;
        onRewardCenterOpened();
        startCreateSurveySessionTask();

        setDisplaySettings();
        generateRewardCenterURL();
        if (this._parentContext != null && this._parentContext.get() != null) {
            Intent intent = new Intent(this._parentContext.get(), RewardCenterActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            this._parentContext.get().startActivity(intent);
        }
    }

    public void onResume(Activity activity) {
        RapidoReach.getInstance()._parentContext = new WeakReference<Activity>(activity);
        if (RapidoReach.getInstance().getAppuserId() == null || RapidoReach.getInstance()._googleAdvertiserId == null) {
            RapidoReach.getInstance().setGoogleAdvertiserId();
        }

        RapidoReach.getInstance().startMomentsTimer();
    }

    private void startMomentsTimer() {
        if (null != refreshAvailableSurvey) return;

        if (RapidoReach.getInstance().isMomentsEnabled() && RapidoReach.getInstance().momentsPollingFrequency >= 1) {
            refreshAvailableSurvey = new Timer();
            refreshAvailableSurveyTask = new TRTimer();
            refreshAvailableSurvey.schedule(refreshAvailableSurveyTask, 0, RapidoReach.getInstance().getMomentsPollingFrequency());
        }
    }

    public void onPause() {
        RapidoReach.getInstance()._parentContext = null;
        momentSurveyAvailable = false;

        if (null != refreshAvailableSurvey) {
            refreshAvailableSurvey.cancel();
            refreshAvailableSurveyTask = null;
            refreshAvailableSurvey = null;
        }
    }

    class  TRTimer extends TimerTask {
        @Override
        public void run() {
            if (RapidoReach.getInstance().momentsPollingFrequency < 1) {
                return;
            }

            if (RapidoReach.getInstance().getAppuserId() != null && RapidoReach.getInstance()._googleAdvertiserId!= null) {
                fetchSurveyEntryURL();
            }
        }
    }

    // TODO: setup unregister listener that sets _parentContext to null. Implement onStart/onStop? Or maybe onDestroy if it's always called

    // TODO: do we actually need the AdvertisingIdClientInfo for Unity or can we use the standard Google?

    // TODO: create a new IntentService class instead of AsyncTasks?

    // TODO: include OKHTTP library? http://square.github.io/okhttp/. RetroFit - http://square.github.io/retrofit/. RetroFit is build for REST, de-serializeation, etc.

    // TODO: look into RefWatcher to ensure we aren't holding onto references inappropriately. https://github.com/square/leakcanary/blob/master/leakcanary-watcher/src/main/java/com/squareup/leakcanary/RefWatcher.java

    // TODO: create new class for all of the static AsyncTasks called like "BackgroundWork"

    void setGoogleAdvertiserId() {
        if (fetchingAppuserId) return;

        fetchingAppuserId = true;

        if (isKindleFire()) {
            String advertisingID = "";
            boolean limitAdTracking = false;

            try {
                ContentResolver cr = RapidoReach.getInstance().getParentContext().getContentResolver();

                // get user's tracking preference
                limitAdTracking = (Secure.getInt(cr, "limit_ad_tracking") == 0) ? false : true;

                if (limitAdTracking) return;

                // get advertising
                advertisingID = Secure.getString(cr, "advertising_id");
                RapidoReach.getInstance().startGetAppuserTask();
            } catch (SettingNotFoundException ex) {
                // not supported
            }

            RapidoReach.getInstance()._googleAdvertiserId = advertisingID;
            fetchingAppuserId = false;
        } else {
            new GpsIdTask().execute();
        }
    }

    public void showUnityRewardCenter(final Activity context)
    {
        if (!checkConnectionStatus()) return;

        getInstance()._parentContext = new WeakReference<Activity>(context);

        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    AdvertisingIdClientInfo.AdInfo adInfo = AdvertisingIdClientInfo.getAdvertisingIdInfo(context);
                    RapidoReach.getInstance()._googleAdvertiserId = adInfo.getId();
                    RapidoReach.this.showRewardCenter();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void showUnityMomentSurvey(final Activity context)
    {
        if (!checkConnectionStatus()) return;

        getInstance()._parentContext = new WeakReference<Activity>(context);

        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    AdvertisingIdClientInfo.AdInfo adInfo = AdvertisingIdClientInfo.getAdvertisingIdInfo(context);
                    RapidoReach.getInstance()._googleAdvertiserId = adInfo.getId();
                    RapidoReach.this.showMomentSurvey();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // TODO: update _parentContext to like parentContextReference or whatever it references

    private boolean checkConnectionStatus() {
        if (this._parentContext != null && this._parentContext.get() != null) {
            setConnectionType(this._parentContext.get());

            if (this._connectionType.contains("unknown")) {
                return false;
            } else {
                return true;
            }
        }

        return false;
    }

    private void setDisplaySettings() throws InvalidParameterException {
        if (this._parentContext != null && this._parentContext.get() != null) {
            RapidoReach.getInstance().setConnectionType(RapidoReach.getInstance().getParentContext());
        }

        String wifi = RapidoReach.getInstance().getConnectionType();
        if (wifi == "unknown") {
            return;
        }

        if (RapidoReach.getInstance().getGpsId() == null) {
            if (this._parentContext != null && this._parentContext.get() != null) {
                String android_id = Settings.Secure.getString(_parentContext.get().getContentResolver(),
                        Settings.Secure.ANDROID_ID);
                RapidoReach.getInstance().setGpsId(android_id);
            }
        }

    }

    private void generateRewardCenterURL() {
        RapidoReach tr = RapidoReach.getInstance();
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(getUrlPrefix() + "sdk/v2/appuser_entry?gps_id=").append(tr.getGpsId());
        urlBuilder.append("&api_key=").append(tr.getApiKey());
        urlBuilder.append("&user_id=").append(tr.getUserID());

        if (RapidoReach.getInstance().resetProfiler) {
            urlBuilder.append("&reset_profiler=").append("true");
        }

        if (RapidoReach.getInstance().placementId != null && RapidoReach.getInstance().placementId.length() > 1) {
            urlBuilder.append("&placement_id=").append(RapidoReach.getInstance().placementId);
        }

        this._surveyUrl = urlBuilder.toString();
        Log.d(TAG, "generateRewardCenterURL "+this._surveyUrl);
    }

    private void generateSurveyEntryURL() {
        this._surveyUrl = RapidoReach.getInstance().getMomentEntryURL();
    }

    public void unityEarnedReward(int quantity) {
        unityEarnedContent = quantity;
    }

    public void unityAwardNotification() {
        if (RapidoReach.getInstance()._unityObjectName != null && RapidoReach.getInstance().getUnityEarnedContent() > 0) {

            String quantity = RapidoReach.getInstance().getUnityEarnedContent() + "";
            com.unity3d.player.UnityPlayer.UnitySendMessage(RapidoReach.getInstance()._unityObjectName, "OnReward", quantity);

            RapidoReach.getInstance().setUnityEarnedContent(0);

            startUpdatePendingCoinsTask();
        }
    }

    public void awardContent(int quantity) {
        try {
            if (quantity > 0) {
                if (RapidoReach.getInstance()._rewardListener != null) {
                    RapidoReach.getInstance().getRewardListener().onReward(quantity);

                    startUpdatePendingCoinsTask();
                }

                if (RapidoReach.getInstance()._unityObjectName != null) {
                    RapidoReach.getInstance().unityEarnedReward(quantity);

                    if (RapidoReach.getInstance()._parentContext != null && RapidoReach.getInstance()._parentContext.get() != null) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RapidoReach.getInstance().getParentContext());

                        if (!(prefs.getBoolean("theoremReachActive", false))) {
                            unityAwardNotification();
                        }
                    }
                }
            }

        } catch (Exception e) {

        }
    }

    private void startGetAppuserTask() {
        new GetAppuserId().execute();
    }

    private static class GetAppuserId extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {

                new AppuserConnection().getAppuserId();

                RapidoReach.getInstance().fetchingAppuserId = false;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (RapidoReach.getInstance()._appuserId != null && RapidoReach.getInstance()._appuserId.length() > 1) {
                if (RapidoReach.getInstance()._parentContext != null && RapidoReach.getInstance()._parentContext.get() != null) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RapidoReach.getInstance().getParentContext());
                    Log.d("RapidoReach", "putString theoremReachAppuserId");
                    prefs.edit().putString("theoremReachAppuserId", RapidoReach.getInstance()._appuserId).apply();
                }

                if (!RapidoReach.getInstance().initialized) {
                    RapidoReach.getInstance().checkForEarnedContent();
                    RapidoReach.getInstance().initialized = true;
                }

                if (RapidoReach.getInstance().isMomentsEnabled() && !RapidoReach.getInstance().fetchingMomentSurvey && (RapidoReach.getInstance().momentsPollingFrequency >= 1) && RapidoReach.getInstance().getMomentEntryURL() == "") {
                    RapidoReach.getInstance().startMomentsTimer();
                }
            }
        }
    }

    private void checkForEarnedContent() {
        if (RapidoReach.getInstance().getAppuserId() == null || RapidoReach.getInstance()._appuserId.length() < 1 || RapidoReach.getInstance()._googleAdvertiserId == null) {
            RapidoReach.getInstance().setGoogleAdvertiserId();
            return;
        }
        startCheckForRewardsTask();
    }

    private void startCheckForRewardsTask() {
        new CheckAppuserRewards().execute();
    }

    private static class CheckAppuserRewards extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {

                new AppuserConnection().checkAppuserRewards();

                RapidoReach.getInstance().fetchingAppuserId = false;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    void fetchSurveyEntryURL() {
        if (RapidoReach.getInstance().momentsPollingFrequency < 1) {
            return;
        }

        if (RapidoReach.getInstance().getAppuserId() == null || RapidoReach.getInstance()._googleAdvertiserId == null) {
            RapidoReach.getInstance().setGoogleAdvertiserId();
            return;
        }
        new FetchSurveyEntryURL().execute();
    }

    private static class FetchSurveyEntryURL extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                if (RapidoReach.getInstance().fetchingMomentSurvey) return null;

                RapidoReach.getInstance().momentSurveyAvailable = false;
                RapidoReach.getInstance().setMomentEntryURL("");

                RapidoReach.getInstance().fetchingMomentSurvey = true;
                new AppuserConnection().getMomentSurveyEntryURL();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            RapidoReach.getInstance().fetchingMomentSurvey = false;
            if (RapidoReach.getInstance().getMomentSurveyLength() > 0 && RapidoReach.getInstance().getMomentSurveyLength() < 31 && RapidoReach.getInstance().getMomentEntryURL() != null && RapidoReach.getInstance().getMomentEntryURL().length() > 10) {
                int length = RapidoReach.getInstance().getMomentSurveyLength();
                RapidoReach.getInstance().onMomentSurveyReceived(length);
                RapidoReach.getInstance().momentSurveyAvailable = true;
            }
        }
    }

    void fetchCloseButtonFromURL() {
        if (RapidoReach.getInstance().overrideCloseButtonURL == null) return;

        new FetchCloseButtonFromURL().execute();
    }

    private static class FetchCloseButtonFromURL extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {

                RapidoReach.getInstance().overrideCloseButton = new AppuserConnection().drawableFromUrl(RapidoReach.getInstance().overrideCloseButtonURL);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    void fetchRefreshButtonFromURL() {
        if (RapidoReach.getInstance().overrideRefreshButtonURL == null) return;

        new FetchRefreshButtonFromURL().execute();
    }

    private static class FetchRefreshButtonFromURL extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {

                RapidoReach.getInstance().overrideRefreshButton = new AppuserConnection().drawableFromUrl(RapidoReach.getInstance().overrideRefreshButtonURL);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static class GpsIdTask extends AsyncTask<Void, Void, String> {
        @Override protected String doInBackground(Void... params) {
            AdvertisingIdClient.Info idInfo = null;
            try {
                if (RapidoReach.getInstance()._parentContext != null && RapidoReach.getInstance()._parentContext.get() != null) {
                    idInfo = AdvertisingIdClient.getAdvertisingIdInfo(RapidoReach.getInstance().getParentContext());
                }
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String advertId = null;
            try{
                advertId = idInfo.getId();
            }catch (NullPointerException e){
                e.printStackTrace();
            }

            return advertId;
        }
        @Override
        protected void onPostExecute(String advertId) {

            if (advertId != null) {
                RapidoReach.getInstance()._googleAdvertiserId = advertId;
                RapidoReach.getInstance().startGetAppuserTask();
            } else {
                RapidoReach.getInstance().fetchingAppuserId = false;
            }
        }
    }

    private void startUpdatePendingCoinsTask() {
        new UpdatePendingCoins().execute();
    }

    private static class UpdatePendingCoins extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                new AppuserConnection().grantUserReward();
            } catch (Exception e) {
                e.printStackTrace();

            }
            return null;
        }
    }

    private void startCreateSurveySessionTask() {
        new CreateSurveySession().execute();
    }

    private static class CreateSurveySession extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                new AppuserConnection().createSurveySession();

            } catch (Exception e) {
                e.printStackTrace();

            }
            return null;
        }
    }

    public void resetProfiler(boolean resetProfiler) {
        RapidoReach.getInstance().resetProfiler = resetProfiler;
    }

    public void enableMoments(boolean momentEnabled) {

        RapidoReach.getInstance().momentsEnabled = momentEnabled;
        if (RapidoReach.getInstance().getAppuserId() != null && RapidoReach.getInstance().getAppuserId().length() > 0 && RapidoReach.getInstance().isSurveyAvailable() && (RapidoReach.getInstance().momentsPollingFrequency >= 1)) {
            fetchSurveyEntryURL();
        }
    }

    public void enableTitleBarInMoments(boolean enabled) {

        RapidoReach.getInstance().momentsTitleBarEnabled = enabled;
    }

    public void setRapidoReachRewardListener(RapidoReachRewardListener object)
    {
        this._rewardListener = object;
    }

    public void setRapidoReachSurveyListener(RapidoReachSurveyListener object)
    {
        this._surveyListener = object;
    }

    public void setRapidoReachSurveyAvailableListener(RapidoReachSurveyAvailableListener object)
    {
        this._surveyAvailableListener = object;
    }

    public void setRapidoReachMomentListener(RapidoReachMomentListener object)
    {
        this._momentListener = object;
    }

    private RapidoReachRewardListener getRewardListener() {
        return _rewardListener;
    }

    private RapidoReachSurveyListener getSurveyListener() {
        return _surveyListener;
    }

    private RapidoReachMomentListener getMomentListener() {
        return _momentListener;
    }

    private RapidoReachSurveyAvailableListener getSurveyAvailableListener() {
        return _surveyAvailableListener;
    }

    public void onRewardCenterOpened() {
        if (RapidoReach.getInstance()._surveyListener != null) {
            RapidoReach.getInstance().getSurveyListener().onRewardCenterOpened();
        }

        if (RapidoReach.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(RapidoReach.getInstance()._unityObjectName, "OnRewardCenterOpened", "");
        }
    }

    public void onMomentSurveyOpened() {
        if (RapidoReach.getInstance()._momentListener != null) {
            RapidoReach.getInstance().getMomentListener().onMomentSurveyOpened();
        }

        if (RapidoReach.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(RapidoReach.getInstance()._unityObjectName, "OnMomentSurveyOpened", "");
        }
    }

    public void onMomentSurveyClosed() {
        momentSurveyOpen = false;
        if (RapidoReach.getInstance()._momentListener != null) {
            RapidoReach.getInstance().getMomentListener().onMomentSurveyClosed();
        }

        if (RapidoReach.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(RapidoReach.getInstance()._unityObjectName, "OnMomentSurveyClosed", "");
        }
    }

    public void onMomentSurveyReceived(int surveyLength) {
        if (RapidoReach.getInstance()._momentListener != null) {
            RapidoReach.getInstance().getMomentListener().onMomentSurveyReceived(surveyLength);
        }

        if (RapidoReach.getInstance()._unityObjectName != null) {
            String length = surveyLength + "";
            com.unity3d.player.UnityPlayer.UnitySendMessage(RapidoReach.getInstance()._unityObjectName, "OnMomentSurveyReceived", length);
        }
    }

    public void onMomentSurveyCompleted() {
        if (RapidoReach.getInstance()._momentListener != null) {
            RapidoReach.getInstance().getMomentListener().onMomentSurveyCompleted();
        }

        if (RapidoReach.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(RapidoReach.getInstance()._unityObjectName, "OnMomentSurveyCompleted", "");
        }
    }
    public void onMomentSurveyNotEligible() {
        if (RapidoReach.getInstance()._momentListener != null) {
            RapidoReach.getInstance().getMomentListener().onMomentSurveyNotEligible();
        }

        if (RapidoReach.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(RapidoReach.getInstance()._unityObjectName, "OnMomentSurveyNotEligible", "");
        }
    }

    public void onRewardCenterClosed() {
        RapidoReach.getInstance().surveyAvailable = false;

        RapidoReach.getInstance().placementId = "";

        checkForEarnedContent();

        if (RapidoReach.getInstance()._surveyListener != null) {
            RapidoReach.getInstance().getSurveyListener().onRewardCenterClosed();
        }

        if (RapidoReach.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(RapidoReach.getInstance()._unityObjectName, "OnRewardCenterClosed", "");
        }

        startGetAppuserTask();
    }

    public String getConnectionType() {
        return _connectionType;
    }

    private String getMomentEntryURL() {
        return momentEntryURL;
    }

    public void setConnectionType(Activity context) {
        this._connectionType = "connectionType";
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            @SuppressWarnings("deprecation") NetworkInfo info = cm.getActiveNetworkInfo();
            //noinspection deprecation
            if (info == null || !info.isConnected()) {
                this._connectionType = "unknown"; //not connected
                return;
            }
            if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                this._connectionType = "WIFI";
                return;
            }
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                int networkType = info.getSubtype();
                switch (networkType) {
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                        this._connectionType = "2G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                        this._connectionType = "2G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                        this._connectionType = "2G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                        this._connectionType = "2G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_IDEN: //api<8 : replace by 11
                        this._connectionType = "2G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                        this._connectionType = "3G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        this._connectionType = "3G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                        this._connectionType = "3G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                        this._connectionType = "3G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                        this._connectionType = "3G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                        this._connectionType = "3G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_EVDO_B: //api<9 : replace by 14
                        this._connectionType = "3G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_EHRPD:  //api<11 : replace by 12
                        this._connectionType = "3G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_HSPAP:  //api<13 : replace by 15
                        this._connectionType = "3G";
                        return;
                    case TelephonyManager.NETWORK_TYPE_LTE:    //api<11 : replace by 13
                        this._connectionType = "4G";
                        return;
                    default:
                        this._connectionType = "unknown";
                        return;
                }
            } else {
                this._connectionType = "unknown";
                return;
            }
        } catch (Exception e) {
            this._connectionType = "unknown";
            return;
        }
    }

    public void setAppDevice() {
        this._appDevice = Build.BRAND + " " + Build.MODEL;
    }

    public void setCarrier(Activity context) {
        try {
            TelephonyManager telephonyManager = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
            if (telephonyManager.getSimOperatorName() != null) {
                this._carrier = telephonyManager.getSimOperatorName();
            } else {
                this._carrier = "";
            }
        } catch (Exception e) {
            this._carrier = "";
        }
    }

    public void setOsVersion() {
        this._osVersion = String.valueOf(Build.VERSION.SDK_INT);
    }

    public Activity getParentContext() {
        return _parentContext.get();
    }

    public void setAppuserId(String appuserId)
    {
        this._appuserId = appuserId;
    }

    public String getAppuserId() {
        Log.d(TAG, "Within getAppUserUserId");
        if (this._appuserId == null || this._appuserId.length() < 1) {
            if (RapidoReach.getInstance()._parentContext != null && RapidoReach.getInstance()._parentContext.get() != null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RapidoReach.getInstance().getParentContext());
                Log.d(TAG, "App user Id is null ");
                return prefs.getString("theoremReachAppuserId", null);
            }
        }
        Log.d(TAG, "appUserId is "+this._appuserId);
        return this._appuserId;
    }

    public int getUnityEarnedContent() {
        return RapidoReach.getInstance().unityEarnedContent;
    }

    private void setUnityEarnedContent(int content) {
        RapidoReach.getInstance().unityEarnedContent = content;
    }

    protected void setRewardIds(String rewardIds) {
        RapidoReach.getInstance().rewardIds = rewardIds;
    }

    public String getRewardIds() {
        return RapidoReach.getInstance().rewardIds;
    }

    public void setMomentSurveyLength(int minLength) {
        RapidoReach.getInstance().momentSurveyLength = minLength;
    }

    public void setMomentEntryURL(String momentEntryURL) {
        RapidoReach.getInstance().momentEntryURL = momentEntryURL;
    }

    public void setSurveyAvailable(boolean surveyAvailable) {
        RapidoReach.getInstance().surveyAvailable = surveyAvailable;

        if (RapidoReach.getInstance()._surveyAvailableListener != null) {
            RapidoReach.getInstance().getSurveyAvailableListener().rapidoReachSurveyAvailable(surveyAvailable);
        }

        if (RapidoReach.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(RapidoReach.getInstance()._unityObjectName, "RapidoReachSurveyAvailable", "" + surveyAvailable);
        }
    }

    public void setIsProfiled(boolean isProfiled) {
        RapidoReach.getInstance().isProfiled = isProfiled;
    }

    private void setGpsId(String android_id)
    {
        if (this._googleAdvertiserId == null) {
            this._googleAdvertiserId = android_id;
        }
    }

    public void setNavigationBarColor(String colorHexCode) {
        this.navigationBarColor = colorHexCode;
    }

    public String getNavigationBarColor() {
        return this.navigationBarColor;
    }

    public void setNavigationBarTextColor(String colorHexCode) {
        this.navigationBarTextColor = colorHexCode;
    }

    public String getNavigationBarTextColor() {
        return this.navigationBarTextColor;
    }

    public void setNavigationBarText(String navigationBarText) {
        this.navigationBarText = navigationBarText;
    }

    public String getNavigationBarText() {
        return this.navigationBarText;
    }

    public void setOverrideCloseButtonURL(String overrideCloseButtonURL) {
        this.overrideCloseButtonURL = overrideCloseButtonURL;
        fetchCloseButtonFromURL();
    }

    public Drawable getOverrideCloseButton() {
        return this.overrideCloseButton;
    }

    public String getOverrideCloseButtonURL() {
        return this.overrideCloseButtonURL;
    }

    public void setOverrideRefreshButtonURL(String overrideRefreshButtonURL) {
        this.overrideRefreshButtonURL = overrideRefreshButtonURL;
        fetchRefreshButtonFromURL();
    }

    public Drawable getOverrideRefreshButton() {
        return this.overrideRefreshButton;
    }

    public String getOverrideRefreshButtonURL() {
        return this.overrideRefreshButtonURL;
    }

    public void setApiKey(String key)
    {
        this._apiKey = key;
    }

    public void setParentActivityContext(Activity key)
    {
        this._parentContext = new WeakReference<Activity>(key);
    }

    public void setUserId(String key)
    {
        this._userId = key;
    }

    public String getSurveyUrl()
    {
        return this._surveyUrl;
    }

    public String getUserID()
    {
        return this._userId;
    }

    public String getCarrier()
    {
        return this._carrier;
    }

    public String getOsVersion()
    {
        return this._osVersion;
    }

    public String getAppDevice()
    {
        return this._appDevice;
    }

    private String getUrlPrefix() {
        return  "https://cbofferwall-srv2.kondgekar.com/";
    }

    public void setUnityObjectName(String objectName) {
        RapidoReach.getInstance()._unityObjectName = objectName;
    }

    public String getApiKey() {
        return _apiKey;
    }

    public String getGpsId() {
        return _googleAdvertiserId;
    }

    public boolean isSurveyAvailable() {
        Log.d(TAG, "Within isSurvyeAvaiable");
        if (RapidoReach.getInstance().getAppuserId() == null || RapidoReach.getInstance()._googleAdvertiserId == null) {
            RapidoReach.getInstance().setGoogleAdvertiserId();
            return false;
        }

        return RapidoReach.getInstance().surveyAvailable;
    }

    private int getMomentSurveyLength() {
        return RapidoReach.getInstance().momentSurveyLength;
    }

    public boolean isProfiled() {
        return RapidoReach.getInstance().isProfiled;
    }

    public boolean isMomentsEnabled() {
        return RapidoReach.getInstance().momentsEnabled;
    }

    public boolean isMomentsTitleBarEnabled() {
        return RapidoReach.getInstance().momentsTitleBarEnabled;
    }

    public void setMomentsSurveyPollingFrequency(int momentsPollingFrequency) {
        RapidoReach.getInstance().momentsPollingFrequency = momentsPollingFrequency;
    }

    private int getMomentsPollingFrequency() {
        return RapidoReach.getInstance().momentsPollingFrequency * 60 * 1000;
    }

    public boolean isSurveyAvailable(int maxLength) {
        if (RapidoReach.getInstance().isSurveyAvailable() && RapidoReach.getInstance().momentSurveyAvailable) {
            if (RapidoReach.getInstance().getMomentSurveyLength() <= maxLength) {
                return true;
            }
        }

        return false;
    }

    public static boolean unityIsSurveyAvailable() {

        if (RapidoReach.getInstance().isSurveyAvailable()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean unityIsProfiled() {

        if (RapidoReach.getInstance().isProfiled()) {
            return true;
        } else {
            return false;
        }
    }

    public void setUnityGame(boolean unity) {
        RapidoReach.getInstance().unity = unity;
    }

    public boolean isUnity() {
        return RapidoReach.getInstance().unity;
    }

    public static boolean isKindleFire() {
        return android.os.Build.MANUFACTURER.equals("Amazon") && (android.os.Build.MODEL.equals("Kindle Fire") || android.os.Build.MODEL.startsWith("KF"));
    }

}
