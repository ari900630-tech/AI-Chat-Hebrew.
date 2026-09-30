package com.ari.fddf

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
 private lateinit var input: EditText
 private lateinit var chat: LinearLayout
 private lateinit var scroll: ScrollView
 private lateinit var sendButton: TextView
 private val backendUrl = "https://fddf.vercel.app/api/chat"

 private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
 private fun bg(c:Int,r:Float)=GradientDrawable().apply{setColor(c);cornerRadius=r}
 private fun strokeBg(fill:Int,stroke:Int,width:Int,r:Float)=GradientDrawable().apply{setColor(fill);setStroke(dp(width),stroke);cornerRadius=r}

 override fun onCreate(b:Bundle?){
  super.onCreate(b)
  window.statusBarColor=Color.rgb(11,16,32)
  window.navigationBarColor=Color.rgb(247,248,252)

  val root=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   setBackgroundColor(Color.rgb(247,248,252))
   layoutDirection=View.LAYOUT_DIRECTION_RTL
  }

  val header=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL
   gravity=Gravity.CENTER_VERTICAL
   setPadding(dp(18),dp(16),dp(18),dp(16))
   setBackgroundColor(Color.rgb(11,16,32))
   layoutDirection=View.LAYOUT_DIRECTION_RTL
  }

  val logo=TextView(this).apply{
   text="✦"
   textSize=25f
   gravity=Gravity.CENTER
   setTextColor(Color.WHITE)
   background=bg(Color.rgb(88,72,220),dp(15).toFloat())
  }
  header.addView(logo,LinearLayout.LayoutParams(dp(48),dp(48)))

  val titles=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   gravity=Gravity.RIGHT
   setPadding(dp(12),0,0,0)
  }
  titles.addView(TextView(this).apply{
   text="AI Chat"
   textSize=21f
   typeface=Typeface.DEFAULT_BOLD
   setTextColor(Color.WHITE)
   gravity=Gravity.RIGHT
  })
  titles.addView(TextView(this).apply{
   text="העוזר החכם שלך"
   textSize=13f
   setTextColor(Color.rgb(177,185,205))
   setPadding(0,dp(3),0,0)
   gravity=Gravity.RIGHT
  })
  header.addView(titles,LinearLayout.LayoutParams(0,-2,1f))

  val online=TextView(this).apply{
   text="● מחובר"
   textSize=12f
   typeface=Typeface.DEFAULT_BOLD
   setTextColor(Color.rgb(134,239,172))
   gravity=Gravity.CENTER
   setPadding(dp(10),dp(7),dp(10),dp(7))
   background=bg(Color.rgb(22,35,48),dp(15).toFloat())
  }
  header.addView(online,LinearLayout.LayoutParams(-2,dp(34)))

  root.addView(header)

  scroll=ScrollView(this).apply{
   setFillViewport(true)
   setPadding(dp(16),dp(14),dp(16),dp(8))
   overScrollMode=View.OVER_SCROLL_IF_CONTENT_SCROLLS
  }
  chat=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   layoutDirection=View.LAYOUT_DIRECTION_RTL
  }
  scroll.addView(chat)
  root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))

  val bottom=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL
   gravity=Gravity.CENTER_VERTICAL
   setPadding(dp(14),dp(10),dp(14),dp(12))
   setBackgroundColor(Color.WHITE)
   elevation=dp(8).toFloat()
   layoutDirection=View.LAYOUT_DIRECTION_RTL
  }

  input=EditText(this).apply{
   hint="כתוב הודעה ל-AI..."
   textSize=16f
   minLines=1
   maxLines=4
   gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
   textDirection=View.TEXT_DIRECTION_RTL
   imeOptions=EditorInfo.IME_ACTION_SEND
   setTextColor(Color.rgb(25,29,40))
   setHintTextColor(Color.rgb(130,136,150))
   setPadding(dp(18),0,dp(18),0)
   background=strokeBg(Color.rgb(247,248,251),Color.rgb(226,229,237),1,dp(20).toFloat())
  }
  bottom.addView(input,LinearLayout.LayoutParams(0,dp(54),1f))

  sendButton=TextView(this).apply{
   text="➤"
   textSize=21f
   typeface=Typeface.DEFAULT_BOLD
   gravity=Gravity.CENTER
   setTextColor(Color.WHITE)
   background=bg(Color.rgb(88,72,220),dp(18).toFloat())
   elevation=dp(3).toFloat()
  }
  val sl=LinearLayout.LayoutParams(dp(54),dp(54))
  sl.setMargins(dp(8),0,0,0)
  bottom.addView(sendButton,sl)
  root.addView(bottom)

  setContentView(root)

  sendButton.setOnClickListener{sendMessage()}
  input.setOnEditorActionListener{_,action,_->if(action==EditorInfo.IME_ACTION_SEND){sendMessage();true}else false}

  showWelcome()
 }

 private fun showWelcome(){
  val card=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   gravity=Gravity.RIGHT
   setPadding(dp(20),dp(18),dp(20),dp(18))
   background=bg(Color.WHITE,dp(20).toFloat())
   elevation=dp(2).toFloat()
   layoutDirection=View.LAYOUT_DIRECTION_RTL
  }
  card.addView(TextView(this).apply{
   text="שלום, נעים להכיר"
   textSize=22f
   typeface=Typeface.DEFAULT_BOLD
   setTextColor(Color.rgb(24,28,40))
   gravity=Gravity.RIGHT
  })
  card.addView(TextView(this).apply{
   text="אני כאן כדי לעזור לך. שאל שאלה, בקש רעיון או התחל שיחה חדשה."
   textSize=15f
   setTextColor(Color.rgb(93,99,114))
   gravity=Gravity.RIGHT
   setPadding(0,dp(7),0,dp(14))
  })
  val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.RIGHT;layoutDirection=View.LAYOUT_DIRECTION_RTL}
  addSuggestion(row,"כתוב לי רעיון")
  addSuggestion(row,"עזור לי לנסח")
  card.addView(row)
  val lp=LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(12));chat.addView(card,lp)
 }

 private fun addSuggestion(row:LinearLayout,label:String){
  val chip=TextView(this).apply{
   text=label
   textSize=13f
   gravity=Gravity.CENTER
   setTextColor(Color.rgb(74,62,180))
   setPadding(dp(12),0,dp(12),0)
   background=strokeBg(Color.rgb(250,249,255),Color.rgb(218,213,249),1,dp(16).toFloat())
   setOnClickListener{input.setText(label);input.setSelection(input.length());input.requestFocus()}
  }
  val lp=LinearLayout.LayoutParams(-2,dp(38));lp.setMargins(0,0,dp(7),0);row.addView(chip,lp)
 }

 private fun addMessage(t:String,user:Boolean){
  val bubble=TextView(this).apply{
   text=t
   textSize=16f
   setTextColor(if(user)Color.WHITE else Color.rgb(38,43,55))
   setPadding(dp(16),dp(12),dp(16),dp(12))
   gravity=Gravity.RIGHT
   textDirection=View.TEXT_DIRECTION_RTL
   background=bg(if(user)Color.rgb(88,72,220) else Color.WHITE,dp(18).toFloat())
   if(!user)elevation=dp(1).toFloat()
  }
  val wrap=LinearLayout(this).apply{
   gravity=if(user)Gravity.END else Gravity.START
   setPadding(0,dp(4),0,dp(4))
  }
  val width=(resources.displayMetrics.widthPixels*.82f).toInt()
  wrap.addView(bubble,LinearLayout.LayoutParams(width,-2))
  chat.addView(wrap)
  scroll.post{scroll.fullScroll(View.FOCUS_DOWN)}
 }

 private fun sendMessage(){
  val m=input.text.toString().trim()
  if(m.isEmpty() || !sendButton.isEnabled)return
  addMessage(m,true)
  input.text.clear()
  sendButton.isEnabled=false
  sendButton.alpha=.55f
  addMessage("חושב…",false)
  val loading=chat.childCount-1
  thread{
   try{
    val body=JSONObject().put("message",m)
    val c=URL(backendUrl).openConnection() as HttpURLConnection
    c.connectTimeout=15000
    c.readTimeout=30000
    c.requestMethod="POST"
    c.setRequestProperty("Content-Type","application/json")
    c.doOutput=true
    c.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
    val s=(if(c.responseCode in 200..299)c.inputStream else c.errorStream).bufferedReader().readText()
    if(c.responseCode !in 200..299)throw Exception("http")
    val answer=JSONObject(s).optString("reply","לא התקבלה תשובה.")
    runOnUiThread{
     if(loading<chat.childCount)chat.removeViewAt(loading)
     addMessage(answer,false)
     sendButton.isEnabled=true
     sendButton.alpha=1f
    }
   }catch(e:Exception){
    runOnUiThread{
     if(loading<chat.childCount)chat.removeViewAt(loading)
     addMessage("לא ניתן להתחבר כרגע לשרת הבינה. נסה שוב בעוד רגע.",false)
     sendButton.isEnabled=true
     sendButton.alpha=1f
    }
   }
  }
 }
}