package com.arichat.ai;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String PREFS = "settings";
    private static final String API_KEY = "api_key";
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";
    private LinearLayout messages;
    private EditText input;
    private ScrollView scroll;
    private SharedPreferences prefs;
    private Button send;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        TextView title = new TextView(this);
        title.setText("AI Chat");
        title.setTextSize(22);
        title.setTextColor(Color.rgb(25,25,35));
        title.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        title.setPadding(24,18,24,18);
        root.addView(title, new LinearLayout.LayoutParams(-1,70));
        scroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(18,12,18,12);
        scroll.addView(messages);
        root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bottom = new LinearLayout(this);
        bottom.setPadding(12,8,12,12);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        input = new EditText(this);
        input.setHint("כתוב הודעה...");
        input.setSingleLine(false);
        input.setMaxLines(4);
        input.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        bottom.addView(input, new LinearLayout.LayoutParams(0,-2,1));
        send = new Button(this);
        send.setText("שלח");
        bottom.addView(send, new LinearLayout.LayoutParams(-2,-2));
        Button settings = new Button(this);
        settings.setText("⚙");
        bottom.addView(settings, new LinearLayout.LayoutParams(-2,-2));
        root.addView(bottom);
        setContentView(root);
        send.setOnClickListener(v -> sendMessage());
        settings.setOnClickListener(v -> showSettings());
        addBubble("שלום! אני מוכן לעזור. שמור את מפתח ה-API בהגדרות כדי להתחיל.", false);
    }

    private void addBubble(String text, boolean user) {
        TextView bubble = new TextView(this);
        bubble.setText(text == null ? "" : text);
        bubble.setTextSize(16);
        bubble.setTextColor(Color.DKGRAY);
        bubble.setGravity(Gravity.RIGHT);
        bubble.setPadding(20,14,20,14);
        bubble.setBackgroundColor(user ? Color.rgb(232,238,255) : Color.rgb(245,245,248));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,8,0,8);
        messages.addView(bubble,lp);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void sendMessage() {
        String msg = input.getText().toString().trim();
        if (msg.isEmpty()) return;
        String key = prefs.getString(API_KEY,"").trim();
        if (key.isEmpty()) { showSettings(); return; }
        addBubble(msg,true);
        input.setText("");
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);
        send.setEnabled(false);
        addBubble("חושב...",false);
        new Thread(() -> {
            String answer;
            try { answer = requestGroq(key,msg); }
            catch (Exception e) { answer = "שגיאה בחיבור לשרת: " + (e.getMessage()==null ? "שגיאה לא ידועה" : e.getMessage()); }
            final String finalAnswer = answer;
            runOnUiThread(() -> {
                if (messages.getChildCount()>0) messages.removeViewAt(messages.getChildCount()-1);
                addBubble(finalAnswer,false);
                send.setEnabled(true);
            });
        }).start();
    }

    private String requestGroq(String key,String msg) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(ENDPOINT).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        c.setRequestProperty("Authorization","Bearer "+key);
        c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
        c.setDoOutput(true);
        JSONObject body = new JSONObject();
        body.put("model",MODEL);
        body.put("temperature",0.7);
        JSONArray arr = new JSONArray();
        JSONObject m = new JSONObject();
        m.put("role","user");
        m.put("content",msg);
        arr.put(m);
        body.put("messages",arr);
        try (OutputStream os=c.getOutputStream()) { os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
        int code=c.getResponseCode();
        InputStream stream=code>=400 ? c.getErrorStream() : c.getInputStream();
        if(stream==null) throw new Exception("HTTP "+code);
        StringBuilder response=new StringBuilder();
        try(BufferedReader br=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))) { String line; while((line=br.readLine())!=null) response.append(line); }
        c.disconnect();
        JSONObject json=new JSONObject(response.toString());
        if(code>=400) { JSONObject err=json.optJSONObject("error"); throw new Exception(err==null ? "HTTP "+code : err.optString("message","HTTP "+code)); }
        JSONArray choices=json.optJSONArray("choices");
        if(choices==null || choices.length()==0) throw new Exception("לא התקבלה תשובה מהשרת");
        return choices.getJSONObject(0).getJSONObject("message").optString("content","לא התקבלה תשובה");
    }

    private void showSettings() {
        final EditText keyEdit=new EditText(this);
        keyEdit.setHint("gsk_...");
        keyEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyEdit.setText(prefs.getString(API_KEY,""));
        new AlertDialog.Builder(this).setTitle("מפתח API").setMessage("המפתח נשמר רק במכשיר. אל תכניס מפתח API לקוד או ל-GitHub.").setView(keyEdit)
            .setPositiveButton("שמור",(d,w)->prefs.edit().putString(API_KEY,keyEdit.getText().toString().trim()).apply())
            .setNegativeButton("ביטול",null).show();
    }
}