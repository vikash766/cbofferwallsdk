package com.example.cbofferwallsdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import static com.example.cbofferwallsdk.CbOfferwall.SDK_VERSION;

/**
 * Created by thammond on 12/23/15.
 */
public class AppuserConnection {
    public static final String TAG = "CbOfferwall";

    byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrl(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    private String getUrlPrefix() {
        return  "https://theoremreach.com/api/sdk/v1/";
    }

    public String getAppuserId() throws JSONException {
        String jsonString = "";
        try {
            String url;

            String urlString = getUrlPrefix() + "appusers";

            CbOfferwall tr = CbOfferwall.getInstance();
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(urlString + "?gps_id=").append(tr.getGpsId());
            urlBuilder.append("&api_key=").append(tr.getApiKey());
            urlBuilder.append("&user_id=").append(tr.getUserID());
            urlBuilder.append("&carrier=").append(tr.getCarrier());
            urlBuilder.append("&os_version=").append(tr.getOsVersion());
            urlBuilder.append("&app_device=").append(tr.getAppDevice());
            urlBuilder.append("&connection_type=").append(tr.getConnectionType());
            urlBuilder.append("&platform=").append("android");
            urlBuilder.append("&sdk_version=").append(SDK_VERSION);

            String cc = Locale.getDefault().getLanguage();

            if (!cc.equals("")) {
                urlBuilder.append("&language=" + cc);
            }

            if (CbOfferwall.getInstance().resetProfiler) {
                urlBuilder.append("&reset_profiler=").append("true");
            }

            url = urlBuilder.toString();

            URL urlOutput = new URL(url);
            URI uri = null;
            try {
                uri = new URI(urlOutput.getProtocol(), urlOutput.getUserInfo(), urlOutput.getHost(), urlOutput.getPort(), urlOutput.getPath(), urlOutput.getQuery(), urlOutput.getRef());
            } catch (Exception e) {

            }
            urlOutput = uri.toURL();

            jsonString = getUrl(urlOutput.toString());

            try {
                JSONObject object = new JSONObject(jsonString);

                String appuserId = object.getString("id");
                boolean surveyAvailable = object.getBoolean("survey_available");
                boolean isProfiled = object.getBoolean("profiled");
                int momentsPollingFrequency = object.getInt("moments_polling_frequency");

                CbOfferwall.getInstance().setAppuserId(appuserId);
                CbOfferwall.getInstance().setSurveyAvailable(surveyAvailable);
                CbOfferwall.getInstance().setMomentsSurveyPollingFrequency(momentsPollingFrequency);

                CbOfferwall.getInstance().setIsProfiled(isProfiled);

                Log.d(TAG, "appuserId: " +  CbOfferwall.getInstance().getAppuserId());

            } catch (JSONException e) {
                e.printStackTrace();
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return jsonString;
    }

    public String getMomentSurveyEntryURL() throws JSONException {
        String jsonString = "";
        try {
            String url;

            String urlString = getUrlPrefix() + "appusers/" + CbOfferwall.getInstance().getAppuserId() + "/moments";

            CbOfferwall tr = CbOfferwall.getInstance();
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(urlString + "?gps_id=").append(tr.getGpsId());
            urlBuilder.append("&api_key=").append(tr.getApiKey());
            urlBuilder.append("&user_id=").append(tr.getUserID());

            if (CbOfferwall.getInstance().resetProfiler) {
                urlBuilder.append("&reset_profiler=").append("true");
            }

            url = urlBuilder.toString();

            jsonString = getUrl(url);

            Log.d(TAG, "getMomentSurveyEntryURL: " + jsonString);

            try {
                JSONObject object = new JSONObject(jsonString);

                String surveyEntryURL = object.getString("entry_url");
                int surveyLength = object.getInt("loi");

                if (surveyEntryURL != null && surveyEntryURL.length() > 1 && surveyLength > 0 && surveyLength < 31) {
                    CbOfferwall.getInstance().setMomentSurveyLength(surveyLength);

                    CbOfferwall.getInstance().setMomentEntryURL(surveyEntryURL);

                }


            } catch (JSONException e) {
                Log.d(TAG, e.toString());
            }

        } catch (IOException ioe) {
            Log.d(TAG, ioe.toString());
        }

        return jsonString;
    }

    public Drawable drawableFromUrl(String url) throws IOException {
        Bitmap x;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.connect();
            InputStream input = connection.getInputStream();

            x = BitmapFactory.decodeStream(input);
            return new BitmapDrawable(CbOfferwall.getInstance().getParentContext().getResources(), x);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getMd5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String md5 = number.toString(16);

            while (md5.length() < 32)
                md5 = "0" + md5;

            return md5;
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public String checkAppuserRewards() throws JSONException {
        String jsonString = "";
        try {
            String url;

            String rewardUrl = "https://theoremreach.com/api/sdk/v2/appusers/" + CbOfferwall.getInstance().getAppuserId() + "/appuser_rewards?api_key=" + CbOfferwall.getInstance().getApiKey();

            String encryptedString = this.getMd5Hash(rewardUrl + "12fb172e94cfcb20dd65c315336b919f");
            String urlString = rewardUrl + "&enc=" + encryptedString;

            url = Uri.parse(urlString).buildUpon().build().toString();

            jsonString = getUrl(url);

            try {
                JSONObject object = new JSONObject(jsonString);
                int pending_coins = 0;
                pending_coins = object.getInt("total_rewards");
                String rewardIds = object.getString("appuser_reward_ids");

                if (pending_coins > 0 && (CbOfferwall.getInstance().getRewardIds() != null && !CbOfferwall.getInstance().getRewardIds().equals(rewardIds))) {
                    CbOfferwall.getInstance().setRewardIds(rewardIds);
                    CbOfferwall.getInstance().awardContent(pending_coins);
                }

            } catch (JSONException e) {
                Log.d(TAG, e.toString());
            }

        } catch (IOException ioe) {
            Log.d(TAG, ioe.toString());
        }

        return jsonString;
    }

    public void grantUserReward() {
        try {

            String urlString = getUrlPrefix() + "appuser_rewards/confirmed";

            String input = "{\"api_key\": \"" + CbOfferwall.getInstance().getApiKey() + "\",\"appuser_reward_ids\": " + "\"" + CbOfferwall.getInstance().getRewardIds() + "\"" + "}";

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            while ((output = br.readLine()) != null) {

            }

            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createSurveySession() {
        try {
            String appuserId = CbOfferwall.getInstance().getAppuserId();

            String urlString = getUrlPrefix() + "appusers/" + appuserId + "/start_new_appuser_session";

            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            String input = "{\"api_key\": \"" + CbOfferwall.getInstance().getApiKey() + "\"" + "}";

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            while ((output = br.readLine()) != null) {

            }

            conn.disconnect();

        } catch (MalformedURLException e) {

            e.printStackTrace();

        } catch (IOException e) {

        }
    }
}