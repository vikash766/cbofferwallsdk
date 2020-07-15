package com.example.cbofferwallsdk;

/**
 * Created by thammond on 12/22/15.
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

public class CbOfferwall {
    private static CbOfferwall _instance;
    private CbOfferwallRewardListener _rewardListener;
    private CbOfferwallSurveyListener _surveyListener;
    private CbOfferwallSurveyAvailableListener _surveyAvailableListener;
    private CbOfferwallMomentListener _momentListener;
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

    private final String TAG = "CbOfferwall";

    public static CbOfferwall getInstance()
    {
        if (_instance == null)
        {
            _instance = new CbOfferwall();
        }
        return _instance;
    }

    public static CbOfferwall initWithApiKeyAndUserIdAndActivityContext(String apiKey, String userId, Activity parentActivity)  {
        getInstance().setup(apiKey, userId, parentActivity);
        getInstance().setNavigationBarText("CbOfferwall");

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

        if (!CbOfferwall.getInstance().momentSurveyAvailable) return;

        CbOfferwall.getInstance().momentSurveyAvailable = false;

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
        if (CbOfferwall.getInstance().getAppuserId() == null || CbOfferwall.getInstance()._googleAdvertiserId == null) {
            CbOfferwall.getInstance().setGoogleAdvertiserId();
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
        CbOfferwall.getInstance()._parentContext = new WeakReference<Activity>(activity);
        if (CbOfferwall.getInstance().getAppuserId() == null || CbOfferwall.getInstance()._googleAdvertiserId == null) {
            CbOfferwall.getInstance().setGoogleAdvertiserId();
        }

        CbOfferwall.getInstance().startMomentsTimer();
    }

    private void startMomentsTimer() {
        if (null != refreshAvailableSurvey) return;

        if (CbOfferwall.getInstance().isMomentsEnabled() && CbOfferwall.getInstance().momentsPollingFrequency >= 1) {
            refreshAvailableSurvey = new Timer();
            refreshAvailableSurveyTask = new TRTimer();
            refreshAvailableSurvey.schedule(refreshAvailableSurveyTask, 0, CbOfferwall.getInstance().getMomentsPollingFrequency());
        }
    }

    public void onPause() {
        CbOfferwall.getInstance()._parentContext = null;
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
            if (CbOfferwall.getInstance().momentsPollingFrequency < 1) {
                return;
            }

            if (CbOfferwall.getInstance().getAppuserId() != null && CbOfferwall.getInstance()._googleAdvertiserId!= null) {
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
                ContentResolver cr = CbOfferwall.getInstance().getParentContext().getContentResolver();

                // get user's tracking preference
                limitAdTracking = (Secure.getInt(cr, "limit_ad_tracking") == 0) ? false : true;

                if (limitAdTracking) return;

                // get advertising
                advertisingID = Secure.getString(cr, "advertising_id");
                CbOfferwall.getInstance().startGetAppuserTask();
            } catch (SettingNotFoundException ex) {
                // not supported
            }

            CbOfferwall.getInstance()._googleAdvertiserId = advertisingID;
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
                    CbOfferwall.getInstance()._googleAdvertiserId = adInfo.getId();
                    CbOfferwall.this.showRewardCenter();
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
                    CbOfferwall.getInstance()._googleAdvertiserId = adInfo.getId();
                    CbOfferwall.this.showMomentSurvey();
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
            CbOfferwall.getInstance().setConnectionType(CbOfferwall.getInstance().getParentContext());
        }

        String wifi = CbOfferwall.getInstance().getConnectionType();
        if (wifi == "unknown") {
            return;
        }

        if (CbOfferwall.getInstance().getGpsId() == null) {
            if (this._parentContext != null && this._parentContext.get() != null) {
                String android_id = Settings.Secure.getString(_parentContext.get().getContentResolver(),
                        Settings.Secure.ANDROID_ID);
                CbOfferwall.getInstance().setGpsId(android_id);
            }
        }

    }

    private void generateRewardCenterURL() {
        CbOfferwall tr = CbOfferwall.getInstance();
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(getUrlPrefix() + "sdk/v2/appuser_entry?gps_id=").append(tr.getGpsId());
        urlBuilder.append("&api_key=").append(tr.getApiKey());
        urlBuilder.append("&user_id=").append(tr.getUserID());

        if (CbOfferwall.getInstance().resetProfiler) {
            urlBuilder.append("&reset_profiler=").append("true");
        }

        if (CbOfferwall.getInstance().placementId != null && CbOfferwall.getInstance().placementId.length() > 1) {
            urlBuilder.append("&placement_id=").append(CbOfferwall.getInstance().placementId);
        }

        this._surveyUrl = urlBuilder.toString();
        Log.d(TAG, "generateRewardCenterURL "+this._surveyUrl);
    }

    private void generateSurveyEntryURL() {
        this._surveyUrl = CbOfferwall.getInstance().getMomentEntryURL();
    }

    public void unityEarnedReward(int quantity) {
        unityEarnedContent = quantity;
    }

    public void unityAwardNotification() {
        if (CbOfferwall.getInstance()._unityObjectName != null && CbOfferwall.getInstance().getUnityEarnedContent() > 0) {

            String quantity = CbOfferwall.getInstance().getUnityEarnedContent() + "";
            com.unity3d.player.UnityPlayer.UnitySendMessage(CbOfferwall.getInstance()._unityObjectName, "OnReward", quantity);

            CbOfferwall.getInstance().setUnityEarnedContent(0);

            startUpdatePendingCoinsTask();
        }
    }

    public void awardContent(int quantity) {
        try {
            if (quantity > 0) {
                if (CbOfferwall.getInstance()._rewardListener != null) {
                    CbOfferwall.getInstance().getRewardListener().onReward(quantity);

                    startUpdatePendingCoinsTask();
                }

                if (CbOfferwall.getInstance()._unityObjectName != null) {
                    CbOfferwall.getInstance().unityEarnedReward(quantity);

                    if (CbOfferwall.getInstance()._parentContext != null && CbOfferwall.getInstance()._parentContext.get() != null) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CbOfferwall.getInstance().getParentContext());

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

                CbOfferwall.getInstance().fetchingAppuserId = false;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (CbOfferwall.getInstance()._appuserId != null && CbOfferwall.getInstance()._appuserId.length() > 1) {
                if (CbOfferwall.getInstance()._parentContext != null && CbOfferwall.getInstance()._parentContext.get() != null) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CbOfferwall.getInstance().getParentContext());
                    Log.d("CbOfferwall", "putString theoremReachAppuserId");
                    prefs.edit().putString("theoremReachAppuserId", CbOfferwall.getInstance()._appuserId).apply();
                }

                if (!CbOfferwall.getInstance().initialized) {
                    CbOfferwall.getInstance().checkForEarnedContent();
                    CbOfferwall.getInstance().initialized = true;
                }

                if (CbOfferwall.getInstance().isMomentsEnabled() && !CbOfferwall.getInstance().fetchingMomentSurvey && (CbOfferwall.getInstance().momentsPollingFrequency >= 1) && CbOfferwall.getInstance().getMomentEntryURL() == "") {
                    CbOfferwall.getInstance().startMomentsTimer();
                }
            }
        }
    }

    private void checkForEarnedContent() {
        if (CbOfferwall.getInstance().getAppuserId() == null || CbOfferwall.getInstance()._appuserId.length() < 1 || CbOfferwall.getInstance()._googleAdvertiserId == null) {
            CbOfferwall.getInstance().setGoogleAdvertiserId();
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

                CbOfferwall.getInstance().fetchingAppuserId = false;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    void fetchSurveyEntryURL() {
        if (CbOfferwall.getInstance().momentsPollingFrequency < 1) {
            return;
        }

        if (CbOfferwall.getInstance().getAppuserId() == null || CbOfferwall.getInstance()._googleAdvertiserId == null) {
            CbOfferwall.getInstance().setGoogleAdvertiserId();
            return;
        }
        new FetchSurveyEntryURL().execute();
    }

    private static class FetchSurveyEntryURL extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                if (CbOfferwall.getInstance().fetchingMomentSurvey) return null;

                CbOfferwall.getInstance().momentSurveyAvailable = false;
                CbOfferwall.getInstance().setMomentEntryURL("");

                CbOfferwall.getInstance().fetchingMomentSurvey = true;
                new AppuserConnection().getMomentSurveyEntryURL();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            CbOfferwall.getInstance().fetchingMomentSurvey = false;
            if (CbOfferwall.getInstance().getMomentSurveyLength() > 0 && CbOfferwall.getInstance().getMomentSurveyLength() < 31 && CbOfferwall.getInstance().getMomentEntryURL() != null && CbOfferwall.getInstance().getMomentEntryURL().length() > 10) {
                int length = CbOfferwall.getInstance().getMomentSurveyLength();
                CbOfferwall.getInstance().onMomentSurveyReceived(length);
                CbOfferwall.getInstance().momentSurveyAvailable = true;
            }
        }
    }

    void fetchCloseButtonFromURL() {
        if (CbOfferwall.getInstance().overrideCloseButtonURL == null) return;

        new FetchCloseButtonFromURL().execute();
    }

    private static class FetchCloseButtonFromURL extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {

                CbOfferwall.getInstance().overrideCloseButton = new AppuserConnection().drawableFromUrl(CbOfferwall.getInstance().overrideCloseButtonURL);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    void fetchRefreshButtonFromURL() {
        if (CbOfferwall.getInstance().overrideRefreshButtonURL == null) return;

        new FetchRefreshButtonFromURL().execute();
    }

    private static class FetchRefreshButtonFromURL extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {

                CbOfferwall.getInstance().overrideRefreshButton = new AppuserConnection().drawableFromUrl(CbOfferwall.getInstance().overrideRefreshButtonURL);

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
                if (CbOfferwall.getInstance()._parentContext != null && CbOfferwall.getInstance()._parentContext.get() != null) {
                    idInfo = AdvertisingIdClient.getAdvertisingIdInfo(CbOfferwall.getInstance().getParentContext());
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
                CbOfferwall.getInstance()._googleAdvertiserId = advertId;
                CbOfferwall.getInstance().startGetAppuserTask();
            } else {
                CbOfferwall.getInstance().fetchingAppuserId = false;
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
        CbOfferwall.getInstance().resetProfiler = resetProfiler;
    }

    public void enableMoments(boolean momentEnabled) {

        CbOfferwall.getInstance().momentsEnabled = momentEnabled;
        if (CbOfferwall.getInstance().getAppuserId() != null && CbOfferwall.getInstance().getAppuserId().length() > 0 && CbOfferwall.getInstance().isSurveyAvailable() && (CbOfferwall.getInstance().momentsPollingFrequency >= 1)) {
            fetchSurveyEntryURL();
        }
    }

    public void enableTitleBarInMoments(boolean enabled) {

        CbOfferwall.getInstance().momentsTitleBarEnabled = enabled;
    }

    public void setCbOfferwallRewardListener(CbOfferwallRewardListener object)
    {
        this._rewardListener = object;
    }

    public void setCbOfferwallSurveyListener(CbOfferwallSurveyListener object)
    {
        this._surveyListener = object;
    }

    public void setCbOfferwallSurveyAvailableListener(CbOfferwallSurveyAvailableListener object)
    {
        this._surveyAvailableListener = object;
    }

    public void setCbOfferwallMomentListener(CbOfferwallMomentListener object)
    {
        this._momentListener = object;
    }

    private CbOfferwallRewardListener getRewardListener() {
        return _rewardListener;
    }

    private CbOfferwallSurveyListener getSurveyListener() {
        return _surveyListener;
    }

    private CbOfferwallMomentListener getMomentListener() {
        return _momentListener;
    }

    private CbOfferwallSurveyAvailableListener getSurveyAvailableListener() {
        return _surveyAvailableListener;
    }

    public void onRewardCenterOpened() {
        if (CbOfferwall.getInstance()._surveyListener != null) {
            CbOfferwall.getInstance().getSurveyListener().onRewardCenterOpened();
        }

        if (CbOfferwall.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(CbOfferwall.getInstance()._unityObjectName, "OnRewardCenterOpened", "");
        }
    }

    public void onMomentSurveyOpened() {
        if (CbOfferwall.getInstance()._momentListener != null) {
            CbOfferwall.getInstance().getMomentListener().onMomentSurveyOpened();
        }

        if (CbOfferwall.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(CbOfferwall.getInstance()._unityObjectName, "OnMomentSurveyOpened", "");
        }
    }

    public void onMomentSurveyClosed() {
        momentSurveyOpen = false;
        if (CbOfferwall.getInstance()._momentListener != null) {
            CbOfferwall.getInstance().getMomentListener().onMomentSurveyClosed();
        }

        if (CbOfferwall.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(CbOfferwall.getInstance()._unityObjectName, "OnMomentSurveyClosed", "");
        }
    }

    public void onMomentSurveyReceived(int surveyLength) {
        if (CbOfferwall.getInstance()._momentListener != null) {
            CbOfferwall.getInstance().getMomentListener().onMomentSurveyReceived(surveyLength);
        }

        if (CbOfferwall.getInstance()._unityObjectName != null) {
            String length = surveyLength + "";
            com.unity3d.player.UnityPlayer.UnitySendMessage(CbOfferwall.getInstance()._unityObjectName, "OnMomentSurveyReceived", length);
        }
    }

    public void onMomentSurveyCompleted() {
        if (CbOfferwall.getInstance()._momentListener != null) {
            CbOfferwall.getInstance().getMomentListener().onMomentSurveyCompleted();
        }

        if (CbOfferwall.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(CbOfferwall.getInstance()._unityObjectName, "OnMomentSurveyCompleted", "");
        }
    }
    public void onMomentSurveyNotEligible() {
        if (CbOfferwall.getInstance()._momentListener != null) {
            CbOfferwall.getInstance().getMomentListener().onMomentSurveyNotEligible();
        }

        if (CbOfferwall.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(CbOfferwall.getInstance()._unityObjectName, "OnMomentSurveyNotEligible", "");
        }
    }

    public void onRewardCenterClosed() {
        CbOfferwall.getInstance().surveyAvailable = false;

        CbOfferwall.getInstance().placementId = "";

        checkForEarnedContent();

        if (CbOfferwall.getInstance()._surveyListener != null) {
            CbOfferwall.getInstance().getSurveyListener().onRewardCenterClosed();
        }

        if (CbOfferwall.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(CbOfferwall.getInstance()._unityObjectName, "OnRewardCenterClosed", "");
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
            if (CbOfferwall.getInstance()._parentContext != null && CbOfferwall.getInstance()._parentContext.get() != null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CbOfferwall.getInstance().getParentContext());
                Log.d(TAG, "App user Id is null ");
                return prefs.getString("theoremReachAppuserId", null);
            }
        }
        Log.d(TAG, "appUserId is "+this._appuserId);
        return this._appuserId;
    }

    public int getUnityEarnedContent() {
        return CbOfferwall.getInstance().unityEarnedContent;
    }

    private void setUnityEarnedContent(int content) {
        CbOfferwall.getInstance().unityEarnedContent = content;
    }

    protected void setRewardIds(String rewardIds) {
        CbOfferwall.getInstance().rewardIds = rewardIds;
    }

    public String getRewardIds() {
        return CbOfferwall.getInstance().rewardIds;
    }

    public void setMomentSurveyLength(int minLength) {
        CbOfferwall.getInstance().momentSurveyLength = minLength;
    }

    public void setMomentEntryURL(String momentEntryURL) {
        CbOfferwall.getInstance().momentEntryURL = momentEntryURL;
    }

    public void setSurveyAvailable(boolean surveyAvailable) {
        CbOfferwall.getInstance().surveyAvailable = surveyAvailable;

        if (CbOfferwall.getInstance()._surveyAvailableListener != null) {
            CbOfferwall.getInstance().getSurveyAvailableListener().cbofferwallSurveyAvailable(surveyAvailable);
        }

        if (CbOfferwall.getInstance()._unityObjectName != null) {
            com.unity3d.player.UnityPlayer.UnitySendMessage(CbOfferwall.getInstance()._unityObjectName, "CbOfferwallSurveyAvailable", "" + surveyAvailable);
        }
    }

    public void setIsProfiled(boolean isProfiled) {
        CbOfferwall.getInstance().isProfiled = isProfiled;
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
        return  "https://theoremreach.com/";
    }

    public void setUnityObjectName(String objectName) {
        CbOfferwall.getInstance()._unityObjectName = objectName;
    }

    public String getApiKey() {
        return _apiKey;
    }

    public String getGpsId() {
        return _googleAdvertiserId;
    }

    public boolean isSurveyAvailable() {
        Log.d(TAG, "Within isSurvyeAvaiable");
        if (CbOfferwall.getInstance().getAppuserId() == null || CbOfferwall.getInstance()._googleAdvertiserId == null) {
            CbOfferwall.getInstance().setGoogleAdvertiserId();
            return false;
        }

        return CbOfferwall.getInstance().surveyAvailable;
    }

    private int getMomentSurveyLength() {
        return CbOfferwall.getInstance().momentSurveyLength;
    }

    public boolean isProfiled() {
        return CbOfferwall.getInstance().isProfiled;
    }

    public boolean isMomentsEnabled() {
        return CbOfferwall.getInstance().momentsEnabled;
    }

    public boolean isMomentsTitleBarEnabled() {
        return CbOfferwall.getInstance().momentsTitleBarEnabled;
    }

    public void setMomentsSurveyPollingFrequency(int momentsPollingFrequency) {
        CbOfferwall.getInstance().momentsPollingFrequency = momentsPollingFrequency;
    }

    private int getMomentsPollingFrequency() {
        return CbOfferwall.getInstance().momentsPollingFrequency * 60 * 1000;
    }

    public boolean isSurveyAvailable(int maxLength) {
        if (CbOfferwall.getInstance().isSurveyAvailable() && CbOfferwall.getInstance().momentSurveyAvailable) {
            if (CbOfferwall.getInstance().getMomentSurveyLength() <= maxLength) {
                return true;
            }
        }

        return false;
    }

    public static boolean unityIsSurveyAvailable() {

        if (CbOfferwall.getInstance().isSurveyAvailable()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean unityIsProfiled() {

        if (CbOfferwall.getInstance().isProfiled()) {
            return true;
        } else {
            return false;
        }
    }

    public void setUnityGame(boolean unity) {
        CbOfferwall.getInstance().unity = unity;
    }

    public boolean isUnity() {
        return CbOfferwall.getInstance().unity;
    }

    public static boolean isKindleFire() {
        return android.os.Build.MANUFACTURER.equals("Amazon") && (android.os.Build.MODEL.equals("Kindle Fire") || android.os.Build.MODEL.startsWith("KF"));
    }

}
