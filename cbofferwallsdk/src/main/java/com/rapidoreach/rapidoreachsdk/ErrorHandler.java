package com.rapidoreach.rapidoreachsdk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;

class ReplyObject {
    public JSONArray Errors = new JSONArray();
    public String ErrorCode = "";
    public JSONArray Info = new JSONArray();
    public JSONArray Data = new JSONArray();
    public ReplyObject(){}
    public ReplyObject(JSONObject object){
        try {
            ErrorCode = object.getString("ErrorCode");
            Data = object.getJSONArray("Data");
            Errors = object.getJSONArray("Errors");
            Info = object.getJSONArray("Info");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public JSONObject toJSONObject() {
        JSONObject jo = new JSONObject();
        try {
            jo.put("ErrorCode", ErrorCode);
            jo.put("Info", Info);
            jo.put("Errors", Errors);
            jo.put("Data", Data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }
}

public class ErrorHandler {
    public static void handle(JSONObject object){
        Message message = RapidoReach.getInstance().mHandler.obtainMessage(1, (Object) object);
        message.sendToTarget();
    }
    public static void handleError(ReplyObject creply){
        if(creply.ErrorCode.equals("SUCCESS")){
            return;
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(RapidoReach.getInstance().getParentContext());
        // set title
        alertDialogBuilder.setTitle(creply.ErrorCode);
        String error = "";
        for (int i = 0; i < creply.Errors.length(); i++){
            try {
                error += creply.Errors.getString(i)+"\n";
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // set dialog message
        alertDialogBuilder
                .setMessage(error)
                .setCancelable(false)
                .setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close
                        // current activity
                        //MainActivity.this.finish();
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }
}


