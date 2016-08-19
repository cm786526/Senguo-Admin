package cc.senguo.SenguoAdmin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import cn.jpush.android.api.InstrumentedActivity;
import cn.jpush.android.api.JPushInterface;

import cc.senguo.SenguoAdmin.wxapi.WXEntryActivity;
import com.loopj.android.http.HttpGet;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
@SuppressLint("SetJavaScriptEnabled") public class Senguo_admin extends InstrumentedActivity {
	final static String murl="http://test.senguo.cc/customer/weixin";
	WebView toweb_admin;
	ValueCallback<Uri> valueCallback;
	static String filePath = null;
	static String KEY;
	public static boolean isForeground = false;
	private volatile boolean isCancelled = false;
	private BroadcastReceiver rec_fromGetWeightService = null;
	private String connect_mac=null;
	ProgressDialog progressBar; //进度提示框
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.senguo_admin);
		toweb_admin=(WebView)findViewById(R.id.toweb_admin);
		Intent intent=this.getIntent();
		final String jpush_id=JPushInterface.getRegistrationID(this);
		String url=intent.getStringExtra("URL");
		if(url.indexOf("madmin/order")==-1){
			 url=url+"&jpush_id="+jpush_id;
		}
		toweb_admin.loadUrl(url);  
		//设置可以自由缩放网页
				WebSettings webSettings = toweb_admin.getSettings();    
				
		        //允许使用javascript  
		        webSettings.setJavaScriptEnabled(true);  
		        webSettings.setDefaultTextEncodingName("GBK");//设置字符编码
		        //将WebAppInterface于javascript绑定  
//		        toweb_admin.addJavascriptInterface(new WebAppInterface(this), "Android");
		        toweb_admin.setWebChromeClient(new WebChromeClient(){
		        	@SuppressWarnings("unused")  
		            public void openFileChooser(ValueCallback<Uri> uploadMsg,String acceptType,  
		                    String capture){
		        		   valueCallback = uploadMsg;  
		                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);  
		                    intent.addCategory(Intent.CATEGORY_OPENABLE);  
		                    intent.setType("image/*");  
		                    startActivityForResult(  
		                            Intent.createChooser(intent, "完成操作需要使用"),  
		                            1);  
		                      
		            }  
		             @SuppressWarnings("unused")  
		             public void openFileChooser(ValueCallback<Uri> uploadMsg,  
		                     String acceptType){  
		             valueCallback = uploadMsg;  
		            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);  
		            intent.addCategory(Intent.CATEGORY_OPENABLE);  
		            intent.setType("image/*");  
		            startActivityForResult(  
		                    Intent.createChooser(intent, "完成操作需要使用"),1);  
		            }  
		               
		             @SuppressWarnings("unused")  
		            public void openFileChooser(ValueCallback<Uri> uploadMsg) {  
		                 valueCallback= uploadMsg;  
		                 Intent intent = new Intent(Intent.ACTION_GET_CONTENT);  
		                 intent.addCategory(Intent.CATEGORY_OPENABLE);  
		                 intent.setType("image/*");  
		                 startActivityForResult(  
		                         Intent.createChooser(intent, "完成操作需要使用"),1);  
		             }  
		              
		            @Override  
		            public boolean onJsAlert(WebView view, String url, String message,  
		                    final JsResult result) {  
		                AlertDialog.Builder b2 = new AlertDialog.Builder(  
		                        Senguo_admin.this)  
		                        .setTitle("温馨提示")  
		                        .setMessage(message)  
		                        .setPositiveButton("确认",  
		                                new AlertDialog.OnClickListener() {  
		                                    @Override  
		                                    public void onClick(DialogInterface dialog,  
		                                            int which) {  
		                                        result.confirm();  
		                                    }  
		                                });  
		                b2.setCancelable(false);  
		                b2.create();  
		                b2.show();  
		                return true;  
		            }  
		        });
		        toweb_admin.setWebViewClient(new WebViewClient(){
		        	public boolean shouldOverrideUrlLoading(WebView view, String url)  
		            {   
		                //  重写此方法表明点击网页/madmin/order&jpush_id=0a0d291b411 (127.0.0.1) 1.78ms里面的链接还是在当前的webview里跳转，不跳到浏览器那边  
		        	//  重写此方法表明点击网页里面的链接还是在当前的webview里跳转，不跳到浏览器那边  
		        	    if(url.equals(murl)){
		        	    	Intent intent=new Intent(Senguo_admin.this,WXEntryActivity.class);
		        	    	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  //注意本行的FLAG设置
		        	    	startActivity(intent);
		        	    	finish();
		        	    	return true;
		        	    }else if (url.equals("http://test.senguo.cc/customer/logout")){
		        	    	 view.loadUrl(url+"?user_type=0&jpush_id="+jpush_id);
		                     Intent intent=new Intent(Senguo_admin.this,MainActivity.class);
		                     intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  //注意本行的FLAG设置
		                     startActivity(intent);
		                     finish();
		                     return true;  
		        	    }else{
		        	    	 view.loadUrl(url);
		                     return true;  
		        	    }
		        	    }        
		             }); 
		       
	}
	
	private long exitTime = 0;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){   
	    	if(keyCode == KeyEvent.KEYCODE_BACK && toweb_admin.canGoBack())
		    {
		    	toweb_admin.goBack();
		    	return true;
		    }
	        if((System.currentTimeMillis()-exitTime) > 2000){  
	            Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();                                
	            exitTime = System.currentTimeMillis();   
	        } else {
	            finish();
//	            System.exit(0);
	        }
	        return true;   
	    }
	    return super.onKeyDown(keyCode, event);
	}

	 @Override
     protected void onResume(){
         super.onResume();
         JPushInterface.onResume(this);
     }
     
     @Override
     protected void onPause(){
         super.onPause();
         JPushInterface.onPause(this);
     }
     
     @Override
 	protected void onActivityResult(int requestCode, int resultCode,
 			Intent intent) {
 		if (requestCode == 1) {

 			if (valueCallback == null)
 				return;

 			Uri result = intent == null || resultCode != RESULT_OK ? null
 					: intent.getData();
 			if (result != null) {

 				if ("content".equals(result.getScheme())) {

 					Cursor cursor = this
 							.getContentResolver()
 							.query(result,
 									new String[] { android.provider.MediaStore.Images.ImageColumns.DATA },
 									null, null, null);
 					cursor.moveToFirst();
 					filePath = cursor.getString(0);
 					cursor.close();
 					System.out.println(filePath);

 				} else {
 					System.out.println(filePath);
 					filePath = result.getPath();

 				}
 				uploadImg();
 			} else {

 				valueCallback.onReceiveValue(result);
 				valueCallback = null;
 			}

 		}
 	}

 	private void uploadImg() {
 		new Thread(new Runnable() {
 			@Override
 			public void run() {
 				// 获得七牛上传凭证uploadToken
 				String token = getUploadToken();
 				// 手机SD卡图片存放路
 				String imgPath = filePath;
 				if (token != null) {
 					String data = imgPath;
 					// 图片名称为当前日期+随机数生成
 					String key = getRandomFileName();
 					KEY = "http://7rf3aw.com2.z0.glb.qiniucdn.com/" + key;
 					UploadManager uploadManager = new UploadManager();
 					uploadManager.put(data, key, token,
 							new UpCompletionHandler() {
 								@Override
 								public void complete(String arg0,
 										ResponseInfo info, JSONObject response) {
 									// TODO Auto-generated method stub
 									Log.i("qiniu", info.toString());
 								}
 							}, new UploadOptions(null, null, false,
 									new UpProgressHandler() {
 										public void progress(String key,
 												double percent) {
 											if (percent == 1.0) {
 												Uri myUri = Uri.parse(filePath);
 												valueCallback.onReceiveValue(myUri);
 												toweb_admin.loadUrl("javascript:uploadImgForAndroid('"+KEY+"')");
 												valueCallback = null;
 											}
 										}
 									}, new UpCancellationSignal() {
 										public boolean isCancelled() {
 											return isCancelled;
 										}
 									}));
 				} else {
 					Log.i("fail", "上传失败");
 				}
 			}

 			public String getRandomFileName() {

 				SimpleDateFormat simpleDateFormat;

 				simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

 				Date date = new Date();

 				String str = simpleDateFormat.format(date);

 				Random random = new Random();

 				int rannum = (int) (random.nextDouble() * (99999 - 10000 + 1)) + 10000;// 获取5位随机数

 				return rannum + str;// 当前时间
 			}
 		}).start();
 	}

 	private String getUploadToken() {
 		HttpGet httpRequest = new HttpGet(
 				"http://test123.senguo.cc/fruitzone/shop/apply/addImg");
 		try {
 			// 取得HttpClient对象
 			HttpClient httpclient = new DefaultHttpClient();
 			// 请求HttpClient，取得HttpResponse
 			HttpResponse httpResponse = httpclient.execute(httpRequest);
 			// 请求成功
 			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
 				// 取得返回的字符串
 				String strResult = EntityUtils.toString(httpResponse
 						.getEntity());
 				String TOKEN = "";
 				String now_time = "";
 				try {
 					JSONObject datajson = new JSONObject(strResult);
 					TOKEN = datajson.getString("token");
 					now_time = datajson.getString("key");
 				} catch (JSONException e1) {
 					// TODO Auto-generated catch block
 					e1.printStackTrace();
 				}
 				Log.e("token", TOKEN);
 				Log.e("DATE", now_time);
 				return TOKEN;
 			} else {
 				Log.e("error", "请求错误!");
 				return null;
 			}
 		} catch (Exception e) {
 			Log.i("url response", "false");
 			e.printStackTrace();
 			return null;
 		}
 	}

}
