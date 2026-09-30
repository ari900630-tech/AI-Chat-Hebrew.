package com.arichat.ai;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String PREFS = "settings";
    private static final String API_KEY = "api_key";
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_MODEL = "openai/gpt-oss-120b";\n    private static final String MODEL_PREF = "model";\n    private static final String[] MODEL_IDS = {
            "openai/gpt-oss-120b",
            "openai/gpt-oss-20b",
            "qwen/qwen3.8-27b",
            "openai/gpt-oss-safeguard-20b",
            "minimaxai/minimax-m2.7",
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "canopylabs/orpheus-v1-english",
            "canopylabs/orpheus-arabic-saudi"
    };
    private static final String[] MODEL_NAMES = {
            "GPT-OSS 120B — מתקדם",
            "GPT-OSS 20B — מהיר",
            "Qwen 3.8 27B — ראייה וחשיבה",
            "GPT-OSS Safeguard 20B — בטיחות",
            "MiniMax M2.7",
            "Llama 3.3 70B",
            "Llama 3.1 8B",
            "Orpheus English — קול",
            "Orpheus Arabic Saudi — קול"
    };

    private LinearLayout messages;
    private EditText input;
    private ScrollView scroll;
    private SharedPreferences prefs;
    private Button send;
    private TextView status;

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(248,250,252));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
    }

    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);\n        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        return t;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(248,250,252));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);\n        root.setTextDirection(View.TEXT_DIRECTION_RTL);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(10), dp(14), dp(10));
        header.setBackgroundColor(Color.WHITE);

        Button menu = smallButton("☰");
        Button newChat = smallButton("＋");
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(48), dp(48));
        header.addView(menu, hp);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.RIGHT);
        TextView title = text("AI Chat", 21, Color.rgb(20,25,35));
        title.setTypeface(null, Typeface.BOLD);
        boolean connected = !prefs.getString(API_KEY,"").trim().isEmpty();
        status = text(connected ? "● מחובר" : "הוסף מפתח API כדי להתחיל", 12,
                connected ? Color.rgb(25,150,90) : Color.rgb(120,125,135));
        brand.addView(title, new LinearLayout.LayoutParams(-1, dp(28)));
        brand.addView(status, new LinearLayout.LayoutParams(-1, dp(22)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(52), 1);
        bp.setMargins(dp(10),0,dp(10),0);
        header.addView(brand, bp);
        header.addView(newChat, hp);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(72)));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(dp(14), dp(18), dp(14), dp(18));
        scroll.addView(messages);
        root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout composerWrap = new LinearLayout(this);
        composerWrap.setPadding(dp(10),dp(8),dp(10),dp(10));
        composerWrap.setBackgroundColor(Color.WHITE);
        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(6),dp(5),dp(6),dp(5));
        composer.setBackground(bg(Color.rgb(245,247,250), 24));

        send = new Button(this);
        send.setText("➤");
        send.setTextSize(18);
        send.setTextColor(Color.WHITE);
        send.setBackground(bg(Color.rgb(30,110,235), 22));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(48),dp(48));
        composer.addView(send, sp);

        input = new EditText(this);
        input.setHint("כתוב הודעה...");
        input.setTextSize(16);
        input.setSingleLine(false);
        input.setMaxLines(4);
        input.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);\n        input.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);\n        input.setTextDirection(View.TEXT_DIRECTION_RTL);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(dp(12),0,dp(12),0);
        composer.addView(input, new LinearLayout.LayoutParams(0,dp(52),1));
        composerWrap.addView(composer, new LinearLayout.LayoutParams(-1,dp(62)));
        root.addView(composerWrap, new LinearLayout.LayoutParams(-1,dp(80)));

        setContentView(root);
        addWelcome();

        send.setOnClickListener(v -> sendMessage());
        newChat.setOnClickListener(v -> startNewChat());
        menu.setOnClickListener(v -> showMenu(v));
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(20);
        b.setTextColor(Color.rgb(55,65,80));
        b.setBackground(bg(Color.rgb(245,247,250), 18));
        b.setPadding(0,0,0,0);
        return b;
    }

    private void addWelcome() {
        if (messages.getChildCount() > 0) return;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18),dp(18),dp(18),dp(18));
        card.setBackground(bg(Color.WHITE,20));
        TextView h = text("ברוכים הבאים ל־AI Chat",20,Color.rgb(20,25,35));
        h.setTypeface(null, Typeface.BOLD);
        card.addView(h,new LinearLayout.LayoutParams(-1,dp(34)));
        TextView p = text("אני כאן כדי לענות על שאלות ולעזור לך. כתוב הודעה למטה כדי להתחיל.",15,Color.rgb(90,98,110));
        p.setGravity(Gravity.RIGHT | Gravity.TOP);
        card.addView(p,new LinearLayout.LayoutParams(-1,dp(62)));
        messages.addView(card,new LinearLayout.LayoutParams(-1,dp(122)));
    }

    private void addBubble(String value, boolean user) {
        TextView bubble = text(value == null ? "" : value,16,user ? Color.WHITE : Color.rgb(35,40,50));
        bubble.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        bubble.setPadding(dp(16),dp(12),dp(16),dp(12));
        bubble.setBackground(bg(user ? Color.rgb(30,110,235) : Color.WHITE,18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(user ? dp(42) : dp(10),dp(5),user ? dp(10) : dp(42),dp(5));
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
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(input.getWindowToken(),0);
        send.setEnabled(false);
        addBubble("חושב...",false);

        new Thread(() -> {
            String answer;
            try { answer = requestGroq(key,msg); }
            catch (Exception e) {
                answer = "שגיאה בחיבור לשרת: " +
                        (e.getMessage()==null ? "שגיאה לא ידועה" : e.getMessage());
            }
            final String finalAnswer = answer;
            runOnUiThread(() -> {
                if (messages.getChildCount()>0)
                    messages.removeViewAt(messages.getChildCount()-1);
                addBubble(finalAnswer,false);
                send.setEnabled(true);
            });
        }).start();
    }

    private String requestGroq(String key,String msg) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(ENDPOINT).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        c.setRequestProperty("Authorization","Bearer "+key);
        c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
        c.setDoOutput(true);

        JSONObject body=new JSONObject();
        body.put("model",prefs.getString(MODEL_PREF,DEFAULT_MODEL));
        body.put("temperature",0.7);\n        body.put("max_completion_tokens",1024);
        JSONArray arr=new JSONArray();
        JSONObject m=new JSONObject();
        m.put("role","user");
        m.put("content",msg);
        arr.put(m);
        body.put("messages",arr);

        try(OutputStream os=c.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code=c.getResponseCode();
        InputStream stream=code>=400 ? c.getErrorStream() : c.getInputStream();
        if(stream==null) throw new Exception("HTTP "+code);

        StringBuilder response=new StringBuilder();
        try(BufferedReader br=new BufferedReader(
                new InputStreamReader(stream,StandardCharsets.UTF_8))) {
            String line;
            while((line=br.readLine())!=null) response.append(line);
        }
        c.disconnect();

        JSONObject json=new JSONObject(response.toString());
        if(code>=400) {
            JSONObject err=json.optJSONObject("error");
            throw new Exception(err==null ? "HTTP "+code :
                    err.optString("message","HTTP "+code));
        }
        JSONArray choices=json.optJSONArray("choices");
        if(choices==null || choices.length()==0)
            throw new Exception("לא התקבלה תשובה מהשרת");
        return choices.getJSONObject(0).getJSONObject("message")
                .optString("content","לא התקבלה תשובה");
    }

    private void showMenu(View anchor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setTextDirection(View.TEXT_DIRECTION_RTL);
        box.setPadding(dp(20), dp(8), dp(20), dp(8));

        TextView title = text("תפריט", 20, Color.rgb(20,25,35));
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        box.addView(title, new LinearLayout.LayoutParams(-1, dp(58)));

        addMenuItem(box,"שיחה חדשה",v -> {
            ((AlertDialog) v.getTag()).dismiss();
            startNewChat();
        });
        addMenuItem(box,"השיחות שלך",v -> {
            ((AlertDialog) v.getTag()).dismiss();
            showHistory();
        });
        addMenuItem(box,"בחירת מודל",v -> {\n            ((AlertDialog) v.getTag()).dismiss();\n            showModelPicker();\n        });\n        addMenuItem(box,"מפתח API",v -> {
            ((AlertDialog) v.getTag()).dismiss();
            showSettings();
        });
        addMenuItem(box,"אודות",v -> {
            ((AlertDialog) v.getTag()).dismiss();
            showAbout();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(box)
                .create();

        for (int i = 1; i < box.getChildCount(); i++) {
            box.getChildAt(i).setTag(dialog);
        }
        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(dp(310), -2);
            }
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(dp(310), -2);
        }
    }

    private void addMenuItem(LinearLayout box,String label,View.OnClickListener listener) {
        Button b=new Button(this);
        b.setText(label);
        b.setTextSize(15);
        b.setTextColor(Color.rgb(35,40,50));
        b.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(dp(10),0,dp(10),0);
        b.setOnClickListener(listener);
        box.addView(b,new LinearLayout.LayoutParams(-1,dp(52)));
    }

    private void showModelPicker() {
        String current = prefs.getString(MODEL_PREF, DEFAULT_MODEL);
        int checked = 0;
        for (int i = 0; i < MODEL_IDS.length; i++) {
            if (MODEL_IDS[i].equals(current)) { checked = i; break; }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("בחירת מודל")
                .setSingleChoiceItems(MODEL_NAMES, checked, (d, which) -> {
                    prefs.edit().putString(MODEL_PREF, MODEL_IDS[which]).apply();
                    d.dismiss();
                    updateStatus();
                    Toast.makeText(this, "המודל נבחר: " + MODEL_NAMES[which], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ביטול", null)
                .create();
        dialog.setOnShowListener(d -> {
            if (dialog.getListView() != null) {
                dialog.getListView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                dialog.getListView().setTextDirection(View.TEXT_DIRECTION_RTL);
            }
        });
        dialog.show();
    }

    private void startNewChat() {
        messages.removeAllViews();
        addWelcome();
        input.setText("");
        input.requestFocus();
    }

    private void showHistory() {
        new AlertDialog.Builder(this)
                .setTitle("השיחות שלך")
                .setMessage("היסטוריית שיחות זמינה בממשק. התחל שיחה חדשה כדי לפתוח שיחה נקייה.")
                .setPositiveButton("סגור",null)
                .show();
    }

    private void showSettings() {
        final EditText keyEdit=new EditText(this);
        keyEdit.setHint("gsk_...");
        keyEdit.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyEdit.setText(prefs.getString(API_KEY,""));

        AlertDialog dialog=new AlertDialog.Builder(this)
                .setTitle("מפתח API")
                .setMessage("המפתח נשמר רק במכשיר. אל תכניס מפתח API לקוד או ל-GitHub.")
                .setView(keyEdit)
                .setPositiveButton("שמור",(d,w)->{
                    prefs.edit().putString(API_KEY,
                            keyEdit.getText().toString().trim()).apply();
                    updateStatus();
                })
                .setNegativeButton("ביטול",null).create();
        dialog.show();
    }

    private void updateStatus() {
        boolean connected=!prefs.getString(API_KEY,"").trim().isEmpty();
        status.setText(connected ? "● מחובר" : "הוסף מפתח API כדי להתחיל");
        status.setTextColor(connected ?
                Color.rgb(25,150,90) : Color.rgb(120,125,135));
    }
}
