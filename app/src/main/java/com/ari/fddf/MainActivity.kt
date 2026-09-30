package com.ari.fddf
import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
class MainActivity:Activity(){private lateinit var key:EditText;private lateinit var input:EditText;private lateinit var chat:LinearLayout
 override fun onCreate(b:Bundle?){super.onCreate(b);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,12)};root.addView(TextView(this).apply{text="AI Chat";textSize=28f;gravity=Gravity.CENTER;padding(0,0,0,16)});key=EditText(this).apply{hint="מפתח API של Groq";inputType=0x81};root.addView(key);val save=Button(this).apply{text="שמור מפתח"};root.addView(save);chat=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};root.addView(ScrollView(this).apply{addView(chat)},LinearLayout.LayoutParams(-1,0,1f));input=EditText(this).apply{hint="כתוב הודעה...";minLines=2};val send=Button(this).apply{text="שלח"};val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};row.addView(input,LinearLayout.LayoutParams(0,-2,1f));row.addView(send);root.addView(row);setContentView(root);key.setText(getPreferences(0).getString("api",""));save.setOnClickListener{getPreferences(0).edit().putString("api",key.text.toString().trim()).apply();toast("המפתח נשמר")};send.setOnClickListener{sendMessage()}}
 private fun sendMessage(){val k=key.text.toString().trim();val m=input.text.toString().trim();if(k.isEmpty()){toast("הזן מפתח API");return};if(m.isEmpty())return;add("אתה: $m");input.text.clear();add("AI: ...");thread{try{val body=JSONObject().put("model","llama-3.1-8b-instant").put("messages",JSONArray().put(JSONObject().put("role","user").put("content",m)));val c=URL("https://api.groq.com/openai/v1/chat/completions").openConnection() as HttpURLConnection;c.requestMethod="POST";c.setRequestProperty("Authorization","Bearer $k");c.setRequestProperty("Content-Type","application/json");c.doOutput=true;c.outputStream.use{it.write(body.toString().toByteArray())};val s=(if(c.responseCode in 200..299)c.inputStream else c.errorStream).bufferedReader().readText();if(c.responseCode !in 200..299)throw Exception(s);val a=JSONObject(s).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");runOnUiThread{chat.removeViewAt(chat.childCount-1);add("AI: $a")}}catch(e:Exception){runOnUiThread{chat.removeViewAt(chat.childCount-1);add("שגיאה: ${e.message}")}}}}
 private fun add(s:String){chat.addView(TextView(this).apply{text=s;textSize=16f;setTextColor(Color.DKGRAY);setPadding(12,10,12,10)}};private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()}
