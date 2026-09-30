package com.arichat.ai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.content.Context
import android.text.InputType
import org.json.JSONArray
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
 private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }
 private val groqUrl = "https://api.groq.com/openai/v1/chat/completions"
 private val currentMessages = mutableListOf<Pair<Boolean,String>>()
 private var currentChatId = System.currentTimeMillis()

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

  val header=FrameLayout(this).apply{
   setPadding(dp(14),dp(10),dp(14),dp(10))
   setBackgroundColor(Color.rgb(11,16,32))
  }

  val newChat=TextView(this).apply{
   text="＋"
   textSize=28f
   gravity=Gravity.CENTER
   setTextColor(Color.WHITE)
   background=bg(Color.rgb(35,45,66),dp(15).toFloat())
   contentDescription="שיחה חדשה"
   setOnClickListener{startNewChat()}
  }
  val newLp=FrameLayout.LayoutParams(dp(48),dp(48),Gravity.START or Gravity.CENTER_VERTICAL)
  header.addView(newChat,newLp)

  val menu=TextView(this).apply{
   text="☰"
   textSize=24f
   gravity=Gravity.CENTER
   setTextColor(Color.WHITE)
   background=bg(Color.rgb(35,45,66),dp(15).toFloat())
   contentDescription="תפריט שיחות"
   setOnClickListener{showMenu(it)}
  }
  val menuLp=FrameLayout.LayoutParams(dp(48),dp(48),Gravity.END or Gravity.CENTER_VERTICAL)
  header.addView(menu,menuLp)

  val center=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL
   gravity=Gravity.CENTER
   layoutDirection=View.LAYOUT_DIRECTION_RTL
  }
  val logo=TextView(this).apply{
   text="✦"
   textSize=24f
   gravity=Gravity.CENTER
   setTextColor(Color.WHITE)
   background=bg(Color.rgb(88,72,220),dp(15).toFloat())
  }
  center.addView(logo,LinearLayout.LayoutParams(dp(44),dp(44)))
  val titles=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   gravity=Gravity.RIGHT
   setPadding(dp(10),0,0,0)
  }
  titles.addView(TextView(this).apply{
   text="AI Chat"
   textSize=20f
   typeface=Typeface.DEFAULT_BOLD
   setTextColor(Color.WHITE)
   gravity=Gravity.RIGHT
  })
  titles.addView(TextView(this).apply{
   text="העוזר החכם שלך"
   textSize=12f
   setTextColor(Color.rgb(177,185,205))
   setPadding(0,dp(2),0,0)
   gravity=Gravity.RIGHT
  })
  center.addView(titles,LinearLayout.LayoutParams(-2,-2))
  val centerLp=FrameLayout.LayoutParams(-2,-2,Gravity.CENTER)
  header.addView(center,centerLp)

  val online=TextView(this).apply{
   text="●"
   textSize=12f
   typeface=Typeface.DEFAULT_BOLD
   setTextColor(Color.rgb(134,239,172))
   gravity=Gravity.CENTER
   background=bg(Color.rgb(22,35,48),dp(15).toFloat())
  }
  val onlineLp=FrameLayout.LayoutParams(dp(30),dp(30),Gravity.CENTER_VERTICAL or Gravity.END)
  onlineLp.rightMargin=dp(58)
  header.addView(online,onlineLp)

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

 private fun addMessage(t:String,user:Boolean,record:Boolean=true){
  if(record)currentMessages.add(user to t)
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

 private fun historyArray():JSONArray=JSONArray(prefs.getString("chat_history","[]") ?: "[]")

 private fun saveCurrentChat(){
  if(currentMessages.isEmpty())return
  val arr=historyArray()
  val obj=JSONObject().put("id",currentChatId)
  val first=currentMessages.firstOrNull{it.first}?.second ?: "שיחה חדשה"
  obj.put("title",if(first.length>38)first.take(38)+"…" else first)
  val msgs=JSONArray()
  currentMessages.forEach{(user,text)->msgs.put(JSONObject().put("user",user).put("text",text))}
  obj.put("messages",msgs)
  var found=false
  for(i in 0 until arr.length()){
   if(arr.optJSONObject(i)?.optLong("id")==currentChatId){arr.put(i,obj);found=true;break}
  }
  if(!found)arr.put(obj)
  while(arr.length()>30)arr.remove(0)
  prefs.edit().putString("chat_history",arr.toString()).apply()
 }

 private fun startNewChat(){
  saveCurrentChat()
  currentChatId=System.currentTimeMillis()
  currentMessages.clear()
  chat.removeAllViews()
  input.text.clear()
  showWelcome()
 }

 private fun loadChat(obj:JSONObject){
  saveCurrentChat()
  currentChatId=obj.optLong("id",System.currentTimeMillis())
  currentMessages.clear()
  chat.removeAllViews()
  val msgs=obj.optJSONArray("messages") ?: JSONArray()
  for(i in 0 until msgs.length()){
   val m=msgs.optJSONObject(i) ?: continue
   addMessage(m.optString("text"),m.optBoolean("user"),true)
  }
  if(msgs.length()==0)showWelcome()
 }

 private fun showMenu(anchor:View){
  val popup=PopupWindow(this)
  val box=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   setPadding(dp(8),dp(8),dp(8),dp(8))
   background=bg(Color.WHITE,dp(16).toFloat())
   elevation=dp(12).toFloat()
   layoutDirection=View.LAYOUT_DIRECTION_RTL
  }
  fun item(label:String,action:()->Unit){
   val v=TextView(this).apply{
    text=label
    textSize=15f
    gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
    setTextColor(Color.rgb(35,39,50))
    setPadding(dp(16),0,dp(16),0)
    setOnClickListener{popup.dismiss();action()}
   }
   box.addView(v,LinearLayout.LayoutParams(dp(260),dp(50)))
  }
  item("＋  שיחה חדשה"){startNewChat()}
  item("🔑  מפתח API"){showApiKeyDialog()}
  item("ℹ  אודות"){
   AlertDialog.Builder(this).setTitle("AI Chat").setMessage("עוזר AI בעברית. גרסה 1.0.23.").setPositiveButton("סגור",null).show()
  }

  val label=TextView(this).apply{
   text="השיחות שלך"
   textSize=13f
   typeface=Typeface.DEFAULT_BOLD
   setTextColor(Color.rgb(100,105,120))
   gravity=Gravity.RIGHT
   setPadding(dp(16),dp(12),dp(16),dp(6))
  }
  box.addView(label,LinearLayout.LayoutParams(dp(260),dp(40)))

  val arr=historyArray()
  if(arr.length()==0){
   box.addView(TextView(this).apply{
    text="אין עדיין שיחות שמורות"
    textSize=14f
    setTextColor(Color.rgb(130,136,150))
    gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
    setPadding(dp(16),0,dp(16),0)
   },LinearLayout.LayoutParams(dp(260),dp(44)))
  }else{
   for(i in arr.length()-1 downTo 0){
    val obj=arr.optJSONObject(i) ?: continue
    val title=obj.optString("title","שיחה")
    item("💬  "+title){loadChat(obj)}
   }
  }

  popup.contentView=box
  popup.isFocusable=true
  popup.isOutsideTouchable=true
  popup.setBackgroundDrawable(bg(Color.WHITE,dp(16).toFloat()))
  popup.width=dp(280)
  popup.height=WindowManager.LayoutParams.WRAP_CONTENT
  popup.showAsDropDown(anchor,dp(-220),dp(4),Gravity.END)
 }

 private fun showApiKeyDialog(){
  val field=EditText(this).apply{
   hint="הדבק כאן את מפתח Groq"
   inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
   setText(prefs.getString("groq_key",""))
  }
  val box=LinearLayout(this).apply{setPadding(dp(22),0,dp(22),0);addView(field,LinearLayout.LayoutParams(-1,dp(56)))}
  AlertDialog.Builder(this).setTitle("מפתח API").setMessage("המפתח נשמר במכשיר ומשמש את האפליקציה לשליחת השיחות ל-Groq.").setView(box)
   .setNegativeButton("ביטול",null)
   .setPositiveButton("שמירה"){_,_->prefs.edit().putString("groq_key",field.text.toString().trim()).apply()}
   .show()
 }

 private fun sendMessage(){
  val m=input.text.toString().trim()
  if(m.isEmpty() || !sendButton.isEnabled)return
  addMessage(m,true)
  input.text.clear()
  sendButton.isEnabled=false
  sendButton.alpha=.55f
  addMessage("חושב…",false,false)
  val loading=chat.childCount-1
  thread{
   try{
    val key=prefs.getString("groq_key","")?.trim().orEmpty()
    val useDirect=key.isNotEmpty()
    val body=if(useDirect)
      JSONObject().put("model","llama-3.1-8b-instant").put("messages",JSONArray().put(JSONObject().put("role","user").put("content",m)))
    else JSONObject().put("message",m)
    val c=URL(if(useDirect)groqUrl else backendUrl).openConnection() as HttpURLConnection
    c.connectTimeout=15000
    c.readTimeout=30000
    c.requestMethod="POST"
    c.setRequestProperty("Content-Type","application/json")
    if(useDirect)c.setRequestProperty("Authorization","Bearer $key")
    c.doOutput=true
    c.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
    val code=c.responseCode
    val stream=if(code in 200..299)c.inputStream else c.errorStream
    val response=stream?.bufferedReader()?.readText().orEmpty()
    if(code !in 200..299){
     val apiMsg=try{JSONObject(response).optJSONObject("error")?.optString("message","")}catch(_:Exception){""}
     val safe=when(code){
      401->"מפתח ה-Groq לא תקין או שפג תוקפו."
      403->"הגישה ל-Groq נדחתה עבור המפתח."
      429->"הגעת למגבלת הבקשות של Groq. נסה שוב מאוחר יותר."
      400->if(apiMsg.isNotBlank())"Groq דחה את הבקשה: $apiMsg" else "Groq דחה את הבקשה."
      else->"Groq החזיר שגיאה $code."
     }
     throw Exception(safe)
    }
    val answer=if(useDirect) JSONObject(response).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content","לא התקבלה תשובה.") ?: "לא התקבלה תשובה." else JSONObject(response).optString("reply","לא התקבלה תשובה.")
    runOnUiThread{
     if(loading<chat.childCount)chat.removeViewAt(loading)
     addMessage(answer,false,true)
     saveCurrentChat()
     sendButton.isEnabled=true
     sendButton.alpha=1f
    }
   }catch(e:Exception){
    runOnUiThread{
     if(loading<chat.childCount)chat.removeViewAt(loading)
     addMessage(e.message ?: "אירעה שגיאה בחיבור ל-AI.",false,true)
     sendButton.isEnabled=true
     sendButton.alpha=1f
    }
   }
  }
 }
}