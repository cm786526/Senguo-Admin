package cc.senguo.SenguoAdmin;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import cn.jpush.android.api.JPushInterface;

public class PushReceive extends BroadcastReceiver {

	private static final String TAG = "TAG";
	 String url;
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Bundle bundle = null;
		if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
	        bundle = intent.getExtras();
	        url= bundle.getString(JPushInterface.EXTRA_ALERT);
		}
	        Log.d(TAG, "onReceive - " + intent.getAction());
	        if (JPushInterface.ACTION_REGISTRATION_ID.equals(intent.getAction())) {
	        	 Log.d(TAG,JPushInterface.ACTION_REGISTRATION_ID );
	        }else if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
	        	 bundle = intent.getExtras();
	 	        url= bundle.getString(JPushInterface.EXTRA_MESSAGE);
	 	        SharedPreferences mySharedPreferences= context.getSharedPreferences("localdata", Activity.MODE_PRIVATE); 
	 	        SharedPreferences.Editor editor = mySharedPreferences.edit(); 
	 	        editor.putString("URL", url); 
	 	        editor.commit();
	            System.out.println("收到了自定义消息。消息内容是：" + bundle.getString(JPushInterface.EXTRA_MESSAGE));
	            // 自定义消息不会展示在通知栏，完全要开发者写代码去处理
	        } else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
	            System.out.println("收到了通知");
	            // 在这里可以做些统计，或者做些其他工作
	        } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {
	        	ActivityCollector.finishAll();
	        	SharedPreferences mySharedPreferences= context.getSharedPreferences("localdata", Activity.MODE_PRIVATE); 
	        	String myurl = mySharedPreferences.getString("URL", "");
	            System.out.println(myurl);
	            // 在这里可以自己写代码去定义用户点击后的行为
	            Intent i = new Intent(context, MainActivity.class);  //自定义打开的界面
	            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            i.putExtra("url",myurl);
	            i.putExtra("activity_flag", 1);
	 
	            context.startActivity(i);
	        } else {
	            Log.d(TAG, "Unhandled intent - " + intent.getAction());
	  }
	}
}
