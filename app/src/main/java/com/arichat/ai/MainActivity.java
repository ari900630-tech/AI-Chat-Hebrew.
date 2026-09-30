package com.arichat.ai;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    LinearLayout messages;
    EditText input, keyInput;
    ScrollView scroll;
    final android.content.SharedPreferences prefs;

    MainActivity() { prefs = null; }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        final android.content.SharedPreferences p = getSharedPreferences("settings",0);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.WHITE);

        TextView title=new TextView(this); title.setText("AI Chat"); title.setTextSize(22); title.setTextColor(Color.rgb(25,25,35)); title.setGravity(Gravity.CENTER_VERTICAL); title.setPadding(24,18,24,18);
        root.addView(title,new LinearLayout.LayoutParams(-1,70));

        scroll=new ScrollView(this); messages=new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL); messages.setPadding(18,12,18,12); scroll.addView(messages);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout bottom=new LinearLayout(this); bottom.setPadding(12,8,12,12); bottom.setGravity(Gravity.CENTER_VERTICAL);
        input=new EditText(this); input.setHint("כתוב הודעה..."); input.setSingleLine(false); input.setMaxLines(4);
        bottom.addView(input,new LinearLayout.LayoutParams(0,-2,1));
        Button send=new Button(this); send.setText("שלח"); bottom.addView(send,new LinearLayout.LayoutParams(-2,-2));
        Button settings=new Button(this); settings.setText("⚙"); bottom.addView(settings,new LinearLayout.LayoutParams(-2,-2));
        root.addView(bottom);
        setContentView(root);

        send.setOnClickListener(v -> sendMessage(p));
        settings.setOnClickListener(v -> showSettings(p));
        addBubble("שלום! אני מוכן לעזור. שמור את מפתח ה-API בהגדרות כדי להתחיל.", false);
    }

    void addBubble(String text, boolean user) {
        TextView t=new TextView(this); t.setText(text); t.setTextSize(16); t.setTextColor(Color.DKGRAY); t.setPadding(20,14,20,14);
        t.setBackgroundColor(user?Color.rgb(232,238,255):Color.rgb(245,245,248));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,8,0,8); messages.addView(t,lp);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    void sendMessage(android.content.SharedPreferences p) {
        String msg=input.getText().toString().trim(); if(msg.isEmpty()) return;
        String key=p.getString("api_key","");
        if(key.isEmpty()){ showSettings(p); return; }
        addBubble(msg,true); input.setText(""); ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);
        addBubble("חושב...",false);
        new Thread(() -> {
            try {
                URL u=new URL("https://api.groq.com/openai/v1/chat/completions");
                HttpURLConnection c=(HttpURLConnection)u.openConnection(); c.setRequestMethod("POST"); c.setRequestProperty("Authorization","Bearer "+key); c.setRequestProperty("Content-Type","application/json"); c.setDoOutput(true);
                JSONObject body=new JSONObject(); body.put("model","llama-3.3-70b-versatile"); body.put("temperature",0.7);
                JSONArray a=new JSONArray(); JSONObject m=new JSONObject(); m.put("role","user"); m.put("content",msg); a.put(m); body.put("messages",a);
                OutputStream os=c.getOutputStream(); os.write(body.toString().getBytes(StandardCharsets.UTF_8)); os.close();
                InputStream is=c.getResponseCode()>=400?c.getErrorStream():c.getInputStream(); BufferedReader br=new BufferedReader(new InputStreamReader(is)); StringBuilder sb=new StringBuilder(); String line; while((line=br.readLine())!=null)sb.append(line);
                JSONObject out=new JSONObject(sb.toString()); String answer=out.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                runOnUiThread(() -> { if(messages.getChildCount()>0) messages.removeViewAt(messages.getChildCount()-1); addBubble(answer,false); });
            } catch(Exception e) { runOnUiThread(() -> { if(messages.getChildCount()>0) messages.removeViewAt(messages.getChildCount()-1); addBubble("שגיאה: "+e.getMessage(),false); }); }
        }).start();
    }

    void showSettings(android.content.SharedPreferences p) {
        final EditText e=new EditText(this); e.setHint("gsk_..."); e.setInputType(129); e.setText(p.getString("api_key",""));
        new AlertDialog.Builder(this).setTitle("מפתח API").setMessage("המפתח נשמר רק במכשיר. לא מכניסים אותו לקוד או ל-GitHub.").setView(e)
        .setPositiveButton("שמור",(d,w)->p.edit().putString("api_key",e.getText().toString().trim()).apply()).setNegativeButton("ביטול",null).show();
    }
}
