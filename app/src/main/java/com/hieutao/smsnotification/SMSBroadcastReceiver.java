package com.hieutao.smsnotification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.google.gson.Gson;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SMSBroadcastReceiver extends BroadcastReceiver {

    Bundle bundle;
    SmsMessage currentSMS;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            bundle = intent.getExtras();
            if (bundle != null) {
                Object[] objects = (Object[]) bundle.get("pdus");
                if (objects != null) {
                    StringBuilder fullMessage = new StringBuilder();
                    String senderNo = null;
                    for (Object obj : objects) {
                        currentSMS = getIncomingMessage(obj, bundle);
                        senderNo = currentSMS.getOriginatingAddress();
                        String message = currentSMS.getDisplayMessageBody();
                        fullMessage.append(message);
                    }
                    String uniqueId = SharedPreferencesManager.getUniqueId(context);
//                    String[] arrayString = {"VPBank", "VietinBank", "Vietcombank", "MBBANK"};
//                    ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(arrayString));
                    boolean found = true;
                    if (uniqueId != null && found) {
                        String slot = detectSim(bundle);

                        Map<String, String> params = new HashMap<String, String>();

                        params.put("uniqueId", uniqueId);
                        params.put("sim", slot);
                        params.put("name", senderNo);
                        params.put("content", fullMessage.toString());

                        post(context, params);

                        //Toast.makeText(context, "sender: " + senderNo + "\nMessage: " + message, Toast.LENGTH_SHORT).show();
                    }

                    this.abortBroadcast();
                }
            }
        }
    }

    private SmsMessage getIncomingMessage(Object aObject, Bundle bundle) {
        String format = bundle.getString("format");
        return SmsMessage.createFromPdu((byte[]) aObject, format);
    }

    // https://stackoverflow.com/questions/35968766/how-to-figure-out-which-sim-received-sms-in-dual-sim-android-device
    private String detectSim(Bundle bundle) {
        int slot = -1;
        Set<String> keySet = bundle.keySet();
        for (String key : keySet) {
            switch (key) {
                case "phone":
                    slot = bundle.getInt("phone", -1);
                    break;
                case "slot":
                    slot = bundle.getInt("slot", -1);
                    break;
                case "simId":
                    slot = bundle.getInt("simId", -1);
                    break;
                case "simSlot":
                    slot = bundle.getInt("simSlot", -1);
                    break;
                case "slot_id":
                    slot = bundle.getInt("slot_id", -1);
                    break;
                case "simnum":
                    slot = bundle.getInt("simnum", -1);
                    break;
                case "slotId":
                    slot = bundle.getInt("slotId", -1);
                    break;
                case "slotIdx":
                    slot = bundle.getInt("slotIdx", -1);
                    break;
                default:
                    if (key.toLowerCase().contains("slot") | key.toLowerCase().contains("sim")) {
                        String value = bundle.getString(key, "-1");
                        if (value.equals("0") | value.equals("1") | value.equals("2")) {
                            slot = bundle.getInt(key, -1);
                        }
                    }
            }
        }

        return String.valueOf(slot);
    }

    private void post(Context context, Map<String, String> params) {
        new Thread(() -> {
            try {
//                OkHttpClient client = new OkHttpClient();
//                String url = "http://localhost:8080/api/order/payment/receive";
                String uniqueId = params.get("uniqueId");
                String slot = params.get("slot");
                String name = params.get("name");
                String content = params.get("content");
//                Gson gson = new Gson();
                String jsonData = "{\r\n    \"uniqueId\": \"" + uniqueId
                        + "\",\r\n    \"slot\": \"" + slot
                        + "\",\r\n    \"name\": \"" + name
                        + "\",\r\n    \"content\": \"" + content + "\"\r\n}";
//                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
//                RequestBody requestBody = RequestBody.create(jsonData, mediaType);
//                Request request = new Request.Builder()
//                        .url(url)
//                        .post(requestBody)
//                        .build();
//                Response response = client.newCall(request).execute();

                OkHttpClient client = new OkHttpClient().newBuilder().build();
                MediaType mediaType = MediaType.parse("application/json");
                RequestBody body = RequestBody.create(mediaType, "{\r\n    \"uniqueId\": \"abc\",\r\n    \"slot\": \"1\",\r\n    \"name\": \"08772\",\r\n    \"content\": \"hihih\"\r\n}");
                Request request = new Request.Builder()
                        .url("http://192.168.1.40:8080/api/order/payment/receive")
                        .method("POST", body)
                        .addHeader("Content-Type", "application/json")
                        .build();
                Response response = client.newCall(request).execute();
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
