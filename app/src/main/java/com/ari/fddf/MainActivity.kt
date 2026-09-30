package com.ari.fddf

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
 private lateinit var key:EditText; private lateinit var input:EditText; private lateinit var chat:LinearLayout; private lateinit var scroll:ScrollView
 private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
 private fun bg(c:Int,r:Float)=GradientDrawable().apply{setColor(c);cornerRadius=r}
 override fun onCreate(b:Bundle?){
  super.onCreate(b); window.statusBarColor=Color.rgb(18,18,24); window.navigationBarColor=Color.rgb(18,18,24)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(245,246,250))}
  val header=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(18),dp(20),dp(18));setBackgroundColor(Color.rgb(18,18,24))}
  header.addView(TextView(this).apply{text="AI Chat";textSize=26f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)})
  header.addView(TextView(this).apply{text="העוזר החכם שלך";textSize=14f;setTextColor(Color.rgb(190,190,205));setPadding(0,dp(3),0,0)})
  root.addView(header)
  val settings=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(8))}
  settings.addView(TextView(this).apply{text="חיבור ל-Groq";textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(45,45,55))})
  val kr=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(7),0,0)}
  key=EditText(this).apply{hint="הדבק כאן מפתח API";textSize=15f;singleLine=true;inputType=0x81;setPadding(dp(14),0,dp(14),0);background=bg(Color.WHITE,dp(16).toFloat())}
  kr.addView(key,LinearLayout.LayoutParams(0,dp(52),1f))
  val save=Button(this).apply{text="שמור";textSize=14f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);background=bg(Color.rgb(91,76,200),dp(16).toFloat())}
  val sl=LinearLayout.LayoutParams(dp(82),dp(52));sl.setMargins(dp(8),0,0,0);kr.addView(save,sl);settings.addView(kr);root.addView(settings)
  scroll=ScrollView(this).apply{setFillViewport(true);setPadding(dp(14),dp(4),dp(14),dp(8))}
  chat=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.BOTTOM};scroll.addView(chat);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  val bottom=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(8),dp(12),dp(10));setBackgroundColor(Color.WHITE)}
  input=EditText(this).apply{hint="כתוב הודעה...";textSize=16f;minLines=1;maxLines=4;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(4),dp(16),dp(4));background=bg(Color.rgb(244,244,248),dp(22).toFloat())}
  bottom.addView(input,LinearLayout.LayoutParams(0,dp(54),1f))
  val send=Button(this).apply{text="➤";textSize=21f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);background=bg(Color.rgb(91,76,200),dp(27).toFloat())}
  val tl=LinearLayout.LayoutParams(dp(54),dp(54));tl.setMargins(dp(8),0,0,0);bottom.addView(send,tl);root.addView(bottom);setContentView(root)
  key.setText(getPreferences(0).getString("api",""))
  save.setOnClickListener{getPreferences(0).edit().putString("api",key.text.toString().trim()).apply();Toast.makeText(this,"המפתח נשמר",Toast.LENGTH_SHORT).show()}
  send.setOnClickListener{sendMessage()}
 }
 private fun addMessage(t:String,user:Boolean){
  val bubble=TextView(this).apply{text=t;textSize=16f;setTextColor(if(user)Color.WHITE else Color.rgb(40,40,48));setPadding(dp(15),dp(11),dp(15),dp(11));background=bg(if(user)Color.rgb(91,76,200)else Color.WHITE,dp(18).toFloat())}
  val wrap=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=if(user)Gravity.END else Gravity.START;setPadding(0,dp(5),0,dp(5))}
  wrap.addView(bubble,LinearLayout.LayoutParams((resources.displayMetrics.widthPixels*.82f).toInt(),-2));chat.addView(wrap);scroll.post{scroll.fullScroll(View.FOCUS_DOWN)}
 }
 private fun sendMessage(){
  val k=key.text.toString().trim();val m=input.text.toString().trim()
  if(k.isEmpty()){Toast.makeText(this,"הזן מפתח API",Toast.LENGTH_SHORT).show();return};if(m.isEmpty())return
  addMessage(m,true);input.text.clear();addMessage("חושב...",false);val loading=chat.childCount-1
  thread{try{
   val body=JSONObject().put("model","llama-3.1-8b-instant").put("messages",JSONArray().put(JSONObject().put("role","user").put("content",m)))
   val c=URL("https://api.groq.com/openai/v1/chat/completions").openConnection() as HttpURLConnection
   c.requestMethod="POST";c.setRequestProperty("Authorization","Bearer "+k);c.setRequestProperty("Content-Type","application/json");c.doOutput=true;c.outputStream.use{it.write(body.toString().toByteArray())}
   val s=(if(c.responseCode in 200..299)c.inputStream else c.errorStream).bufferedReader().readText();if(c.responseCode !in 200..299)throw Exception(s)
   val a=JSONObject(s).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
   runOnUiThread{if(loading<chat.childCount)chat.removeViewAt(loading);addMessage(a,false)}
  }catch(e:Exception){runOnUiThread{if(loading<chat.childCount)chat.removeViewAt(loading);addMessage("שגיאה: "+e.message,false)}}}
}