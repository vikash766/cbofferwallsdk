package com.example.cbofferwallsdk;

/**
 * Created by sudarshan on 15/07/2020.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

public class RewardCenterActivity
        extends AppCompatActivity {
    private int currentId = 1000000;
    protected String _baseUrl;
    protected String _homeButtonURL;
    protected RelativeLayout _progressIndicatorView;
    protected FrameLayout _webViewPlaceholder;
    protected SurveyWebView _webView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        requestWindowFeature(1);
        getWindow().setFlags(1024, 1024);

        RelativeLayout survey_layout = generateSurveyLayout();

        this._baseUrl = CbOfferwall.getInstance().getSurveyUrl();
        this._homeButtonURL = "https://cbofferwall-srv2.kondgekar.com.com/sdk/v1/appuser_abandoned_campaign?id=" + CbOfferwall.getInstance().getAppuserId();
        setOrientationPermission();
        initUI();

        setContentView(survey_layout);

    }

    @Override
    protected void onStart() {
        super.onStart();

        updateSharedPreferences(true);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (CbOfferwall.getInstance().getUnityEarnedContent() > 0) {
            CbOfferwall.getInstance().unityAwardNotification();
        }

        updateSharedPreferences(false);
    }

    private void updateSharedPreferences(boolean status) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("theoremReachActive", status).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (CbOfferwall.getInstance().getOverrideRefreshButtonURL() != null && CbOfferwall.getInstance().getOverrideRefreshButton() != null) {

            Drawable drawable = CbOfferwall.getInstance().getOverrideRefreshButton();

            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Drawable newDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 100, 100, true));

            menu.add(Menu.NONE, 1623333523, Menu.NONE, "Refresh").setIcon(newDrawable)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        } else {
            menu.add(Menu.NONE, 1623333523, Menu.NONE, "Refresh").setIcon(R.drawable.ic_action_refresh_white)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        Drawable drawable = menu.getItem(0).getIcon();
        drawable.mutate();
        drawable.setColorFilter(Color.parseColor(CbOfferwall.getInstance().getNavigationBarTextColor()), PorterDuff.Mode.SRC_ATOP);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (CbOfferwall.getInstance().momentSurveyOpen) {
                    closeRewardCenter();
                    return true;
                }

                WebView view = this._webView;

                URL url = null;
                try {
                    url = new URL(view.getUrl());
                } catch (MalformedURLException localMalformedURLException) {

                }

                if (url != null) {
                    if ((url.getHost().equals("cbofferwall-srv2.kondgekar.com.com")) || (url.getHost().equals("staging.cbofferwall-srv2.kondgekar.com.com"))) {
                        closeRewardCenter();
                        return true;
                    } else {
                        this._webView.loadUrl(this._homeButtonURL);
                    }
                } else {
                    closeRewardCenter();
                }

                return true;

            case 1623333523:
                try {
                    this._webView.loadUrl(this._webView.getUrl());
                } catch (Exception e ) {
                    this._webView.reload();
                }
                return true;

        }

        return (super.onOptionsItemSelected(item));
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    protected void initUI() {
        if (this._webView == null) {
            this._webView = new SurveyWebView(this);
            this._webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            this._webView.getSettings().setJavaScriptEnabled(true);
            this._webView.getSettings().setUseWideViewPort(true);
            this._webView.getSettings().setDomStorageEnabled(true);
            this._webView.setWebChromeClient(new SurveyWebChromeClient());
            this._webView.setWebViewClient(new SurveyWebViewClient());

            this._webView.getSettings().setSupportZoom(true);

            this._webView.loadUrl(this._baseUrl);
        } else {
            if (this._webView.getProgress() >= 100) {
                this._progressIndicatorView.setVisibility(View.INVISIBLE);
            }
            setNavigationButtonsState();
        }
        this._webViewPlaceholder.addView(this._webView);
    }

    public static String getApplicationName(Context context) {
        return context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
    }

    private RelativeLayout generateSurveyLayout() {
        RelativeLayout survey_layout = new RelativeLayout(this);
        survey_layout.setId(this.generateViewId());
        survey_layout.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));

        Toolbar toolbar = new Toolbar(this);
        toolbar.setId(this.generateViewId());
//        LinearLayout.LayoutParams myLayoutParams = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT, 180);

        LinearLayout.LayoutParams myLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        myLayoutParams.weight = 1.0f;
        myLayoutParams.gravity = Gravity.CENTER;

        toolbar.setLayoutParams(myLayoutParams);

        toolbar.setPopupTheme(R.style.LibraryTheme);
        toolbar.setBackgroundColor(Color.parseColor(CbOfferwall.getInstance().getNavigationBarColor()));

        //find Unity alternative
        if (!CbOfferwall.getInstance().isUnity()) {
            toolbar.setContentInsetStartWithNavigation(0);
        }

        survey_layout.addView(toolbar,0);

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Drawable appIcon = getResources().getDrawable(R.drawable.ic_action_remove_white);

        Drawable iconDrawable = appIcon;
        iconDrawable.mutate();
        iconDrawable.setColorFilter(Color.parseColor(CbOfferwall.getInstance().getNavigationBarTextColor()), PorterDuff.Mode.SRC_ATOP);

//        Drawable appIcon = getPackageManager().getApplicationIcon(getApplicationInfo());
//
//        if (appIcon == null) {
//            appIcon = getResources().getDrawable(R.drawable.ic_action_remove_white);
//        } else {
//            Bitmap bitmap = ((BitmapDrawable) appIcon).getBitmap();
//            appIcon = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 160, 160, true));
//        }

        if (CbOfferwall.getInstance().getOverrideCloseButtonURL() != null && CbOfferwall.getInstance().getOverrideCloseButton() != null) {

            Drawable drawable = CbOfferwall.getInstance().getOverrideCloseButton();

            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Drawable newDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 80, 80, true));

            newDrawable.mutate();
            newDrawable.setColorFilter(Color.parseColor(CbOfferwall.getInstance().getNavigationBarTextColor()), PorterDuff.Mode.SRC_ATOP);

            if (!CbOfferwall.getInstance().momentSurveyOpen) {
                getSupportActionBar().setHomeAsUpIndicator(newDrawable);
            }
        } else {
            if (!CbOfferwall.getInstance().momentSurveyOpen) {
                getSupportActionBar().setHomeAsUpIndicator(appIcon);
            }
        }

        TextView title = new TextView(this);
        title.setId(this.generateViewId());
        title.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));
        title.setTextColor(Color.parseColor(CbOfferwall.getInstance().getNavigationBarTextColor()));

        title.setText(CbOfferwall.getInstance().getNavigationBarText());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setMaxLines(1);
        title.setEllipsize(TextUtils.TruncateAt.END);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) title.getLayoutParams();
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//        params.weight = 1.0f;
//        params.gravity = Gravity.CENTER;

//        layoutParams.setMargins(0,0,10,0);
        title.setLayoutParams(layoutParams);
        title.setGravity(Gravity.CENTER);

        toolbar.addView(title);

        FrameLayout webViewPlaceholder = new FrameLayout(this);
        this._webViewPlaceholder = webViewPlaceholder;
        webViewPlaceholder.setId(this.generateViewId());
        webViewPlaceholder.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        RelativeLayout.LayoutParams webViewPlaceholderLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        webViewPlaceholderLayoutParams.addRule(RelativeLayout.BELOW, toolbar.getId());
        webViewPlaceholder.setLayoutParams(webViewPlaceholderLayoutParams);

        survey_layout.addView(webViewPlaceholder);

        RelativeLayout progressView = new RelativeLayout(this);
        this._progressIndicatorView = progressView;
        progressView.setId(this.generateViewId());
        progressView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        RelativeLayout.LayoutParams progressViewLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        progressViewLayoutParams.addRule(RelativeLayout.BELOW, toolbar.getId());
        progressView.setLayoutParams(progressViewLayoutParams);

        progressView.setClickable(true);
        progressView.setBackgroundColor(Color.parseColor("#b0555555"));
        progressView.setVisibility(View.VISIBLE);

        survey_layout.addView(progressView);

        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        RelativeLayout.LayoutParams progressBarParams = (RelativeLayout.LayoutParams) progressBar.getLayoutParams();
        progressBarParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        progressView.addView(progressBar);

        return survey_layout;
    }


    public void onConfigurationChanged(Configuration newConfig) {
        if (this._webView != null) {
            this._webViewPlaceholder.removeView(this._webView);
        }
        super.onConfigurationChanged(newConfig);
        RelativeLayout survey_layout = generateSurveyLayout();
        setContentView(survey_layout);
        initUI();
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        this._webView.saveState(outState);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this._webView.restoreState(savedInstanceState);
    }

    private void setOrientationPermission() {
        WindowManager window_manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = window_manager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int width = metrics.widthPixels;
        if (getResources().getConfiguration().orientation == 2) {
            width = metrics.heightPixels;
        }
        if (width < 760) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    public int generateViewId() {
        AtomicInteger sNextGeneratedId = new AtomicInteger(this.currentId);
        this.currentId += 1;
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    protected void closeRewardCenter() {
        finish();
        CbOfferwall.getInstance().onRewardCenterClosed();
    }

    public void onBackPressed() {

        WebView view = this._webView;

        URL url = null;
        try {
            url = new URL(view.getUrl());
        } catch (MalformedURLException localMalformedURLException) {

        }

        if (url != null) {
            String path = url.getPath();
            if ((url.getHost().equals("cbofferwall-srv2.kondgekar.com.com")) || (url.getHost().equals("staging.cbofferwall-srv2.kondgekar.com.com"))) {
                closeRewardCenter();
                return;
            }
        }

        return;

    }

    protected void setNavigationButtonsState() {
        WebView view = this._webView;
        URL url = null;
        try {
            url = new URL(view.getUrl());
        } catch (MalformedURLException localMalformedURLException) {

        }

        if (url != null) {
            String path = url.getPath();

            if ((url.getHost().equals("cbofferwall-srv2.kondgekar.com.com")) || (url.getHost().equals("staging.cbofferwall-srv2.kondgekar.com.com"))) {
                if (path.contains("pulley/finish")) {
                    closeRewardCenter();
                    return;
                }
            }
        } else {
            this._webView.loadUrl(this._baseUrl);
        }
    }

    // TODO: Create either private SurveyWebChromeClient or external class (best). Make method in RewardCenterctivity

    class SurveyWebChromeClient
            extends WebChromeClient {
        SurveyWebChromeClient() {
        }

        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if ((newProgress >= 100) && (view.getUrl() != null)) {
                RewardCenterActivity.this._progressIndicatorView.setVisibility(View.INVISIBLE);
                RewardCenterActivity.this.setNavigationButtonsState();
            }
        }

        public boolean onCreateWindow (WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
            ((WebView.WebViewTransport) resultMsg.obj).setWebView(view);

            resultMsg.sendToTarget();
            return true;
        }
    }

    class SurveyWebView extends WebView {
        public SurveyWebView(Context context) {
            super(context);
        }

        @Override
        public void goBack() {
            super.goBack();
        }

    }

    class SurveyWebViewClient
            extends WebViewClient {
        SurveyWebViewClient() {
        }

        @Override
        public void onReceivedSslError(final WebView view, final SslErrorHandler handler, SslError error) {
            try {
                final AlertDialog.Builder builder = new AlertDialog.Builder(RewardCenterActivity.this);
                builder.setMessage("Uh oh! The survey has encountered an SSL error while loading. Please click OK to continue to the survey.");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.proceed();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.cancel();
                    }
                });
                final AlertDialog dialog = builder.create();
                dialog.show();
            } catch (Exception e) {
                e.printStackTrace();
                view.loadUrl(CbOfferwall.getInstance().getSurveyUrl());
            }
        }

        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            RewardCenterActivity.this._progressIndicatorView.setVisibility(View.VISIBLE);

            if (url.contains("https://play.google.com/store/apps/") || (url.contains("market://"))) {
                view.loadUrl(CbOfferwall.getInstance().getSurveyUrl());
            }
        }

//        public boolean shouldOverrideUrlLoading(WebView view, String url) {
//
//            if (url.contains("https://play.google.com/store/apps/")) {
//
//                String marketUrl = url.replaceAll("https://play.google.com/store/apps/", "market://");
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(marketUrl)));
//
//            } else if (url.contains("market://")) {
//
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
//
//            }
//
//            return false;
//        }

        public void onPageFinished(WebView view, String url) {

        }
    }

}
