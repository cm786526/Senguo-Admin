package cc.senguo.SenguoAdmin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import CustomView.TasksCompletedView;
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

import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;

@SuppressLint("SetJavaScriptEnabled") public class MainActivity extends InstrumentedActivity {
	private final static String TAG = MainActivity.class.getSimpleName();
	final static String murl="http://test.senguo.cc/customer/weixin";
	final static String qqurl = "http://test.senguo.cc/customer/qq?next=";
	final static String childurl="http://test.senguo.cc/customer/login";
	private static final int REQUEST_CODE = 200;
	private static final int REQUEST_CODE_CM = 300;
	private static final int REQUEST_CODE_SCAN=400;
	WebView toweb;
	ValueCallback<Uri> valueCallback;
	String filePath = null;
	static String KEY;
	static long overtime=0;
	static Thread mythread;
	static ProgressDialog prDialog;
	public static boolean isForeground = false;
	private volatile boolean isCancelled = false;
	private AlertDialog alertDialog;
	final String jpush_id=JPushInterface.getRegistrationID(this);
	private BroadcastReceiver rec_fromGetWeightService = null,rec_fromBluetoothService=null;
	private String connect_mac=null;
	ProgressDialog progressBar; //进度提示框
	boolean is_connected=false;  //蓝牙是否连接
	GetWeightFromBluetooth countService;
	RelativeLayout mLl_parent;
	TasksCompletedView mTasksView;
	private int mTotalProgress;
	private int mCurrentProgress;
	View pop_view; //在当前页面再覆盖的一层layout
	private String mDeviceName;
	private String mDeviceAddress;
	private ExpandableListView mGattServicesList;
	private BluetoothLeService mBluetoothLeService;
	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";
	private boolean  onbind_service=false;
	static Handler myhandler=new Handler(){
		@SuppressWarnings("deprecation")
		public void handleMessage(Message msg){
			switch(msg.what){
			case 0:
				prDialog.dismiss();
				mythread.destroy();
				break;
			case 1:
				int newProgress=(int) (msg.getData().getDouble("percent")*100);
		        if(newProgress==100){  
		            prDialog.dismiss();
		        }  
				break;
			case 2:
				prDialog.dismiss();
				mythread.destroy();
				break;
			}
		}
	};



	 @Override 
	public void  onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		 requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.senguo_main);
		ActivityCollector.addActivity(this); 
		init();
		 ShowTaskCompleteView();
		alertDialog = new AlertDialog.Builder(this).create(); 
        toweb=(WebView)findViewById(R.id.toweb);

		 //设置webview的user-agent
		 String ua = toweb.getSettings().getUserAgentString();
		 toweb.getSettings().setUserAgentString(ua+"senguo:app, senguo:adminapp, senguo:ardadminapp");
		 //注册接收来自于取重的广播
		 this.rec_fromGetWeightService = new MyBroadcastReceiver();
		 IntentFilter filter_fromweight = new IntentFilter();
		 filter_fromweight.addAction("cc.senguo.SenguoAdmin.GetWeightFromBluetooth");// 这里的Action要和Service发送广播时使用的Action一样
		 MainActivity.this.registerReceiver(this.rec_fromGetWeightService, filter_fromweight);  // 动态注册BroadcastReceiver
		 registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());//注册监听蓝牙服务的广播
		 //接收来自于获取蓝牙设备的广播
		 this.rec_fromBluetoothService = new MyBroadcastReceiver();
		 IntentFilter filter_from_bluetooth = new IntentFilter();
		 filter_from_bluetooth.addAction("cc.senguo.SenguoAdmin.BluetoothService");// 这里的Action要和Service发送广播时使用的Action一样
		 MainActivity.this.registerReceiver(this.rec_fromGetWeightService, filter_from_bluetooth);  // 动态注册BroadcastReceiver

        Intent intent=this.getIntent();
		int activity_flag=intent.getIntExtra("activity_flag", -1);
		if(activity_flag==1){     //推送跳转过来
			String url=intent.getStringExtra("url");
			toweb.loadUrl(url);
		}
		else if(activity_flag==2){    // 微信授权跳转过来
			String url=intent.getStringExtra("url")+"&jpush_id="+jpush_id;
			toweb.loadUrl(url);	 
		}
		else if(activity_flag==0){   //启动页跳转过来的
			toweb.loadUrl("http://test.senguo.cc/madmin?jpush_id="+ jpush_id);
//			toweb.loadUrl("http://test.senguo.cc/madmin");
		}

		//设置可以自由缩放网页
		WebSettings webSettings = toweb.getSettings();    
        webSettings.setJavaScriptEnabled(true);  
        webSettings.setDefaultTextEncodingName("GBK");//设置字符编码
        toweb.addJavascriptInterface(new WebAppInterface(this), "SenguoAdmin");
		 //加上下面这段代码可以使网页中的链接不以浏览器的方式打开  
        toweb.setWebChromeClient(new WebChromeClient(){
			@Override
			public void onProgressChanged(WebView view, int progress) {
				if (progress < mTotalProgress) {
					if(mTasksView!=null){
						mTasksView.setProgress(progress);
						try {
							Thread.sleep(100);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			// Android > 4.1.1 调用这个方法
        	@SuppressWarnings("unused")  
            public void openFileChooser(ValueCallback<Uri> uploadMsg,String acceptType,  
                    String capture){
					valueCallback = uploadMsg;
					Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
					intent.addCategory(Intent.CATEGORY_OPENABLE);
					intent.setType("image/*");
					startActivityForResult(Intent.createChooser(intent, null),
							1);

            }  
        	// 3.0 + 调用这个方法
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
             
             // Android < 3.0 调用这个方法 
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
                        MainActivity.this)  
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
        toweb.setWebViewClient(new WebViewClient(){
        	@Override
        	public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
				ShowTaskCompleteView();

                //  重写此方法表明点击网页里面的链接还是在当前的webview里跳转，不跳到浏览器那边  
        	    if(url.equals(murl)){
        	    	Intent intent=new Intent(MainActivity.this,WXEntryActivity.class);
        	    	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  //注意本行的FLAG设置
        	    	startActivity(intent);
        	    	finish();
        	    	return true;
        	    }else if (url.equals("http://test.senguo.cc/customer/logout")){
        	    	view.loadUrl(url+"?user_type=0&jpush_id="+jpush_id);
                    return true;  
       	    }else if(url.equals("http://test.senguo.cc/customer/login")){
       	    	toweb.loadUrl("http://test.senguo.cc/madmin?jpush_id="+jpush_id);
    	    	CookieSyncManager.createInstance(MainActivity.this);  
    	    	CookieSyncManager.getInstance().startSync();  
    	    	CookieManager.getInstance().removeSessionCookie();
       	    	return true;
       	    }else if(url.equals("http://test.senguo.cc/customer/qq?next=/madmin?jpush_id=0802773f7aa")){
				Intent intent=new Intent(MainActivity.this,Main2Activity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  //注意本行的FLAG设置
				intent.putExtra("url",url);
				startActivity(intent);
//				finish();
				return true;
			}else if(url.equals("http://test.senguo.cc/madmin/scanorder")) {
					Intent intent = new Intent(MainActivity.this,CaptureActivity.class);
					startActivityForResult(intent,REQUEST_CODE);
					return true;
				}else if(url.indexOf("tel:")!=-1){
					Intent intent = new Intent(Intent.ACTION_DIAL);
					intent.setData(Uri.parse(url));
					startActivity(intent);
					return true;

			}else {

//					System.out.println(url);
        	    	view.loadUrl(url);
                    return true;  
        	    }
            }
        	@Override
        	public void onPageFinished(WebView view, String url) {

				if(pop_view!=null){
					mLl_parent.removeView(mTasksView);
					mLl_parent.removeView(pop_view);
				}
                super.onPageFinished(view, url);
            }

			@SuppressWarnings("deprecation")
			@Override
        	public void onReceivedError(WebView view, int errorCode,  
                    String description, final String failingUrl) {
                alertDialog.setTitle("页面加载出错");  
                alertDialog.setMessage("网络连接不正常,请检查网络..");  
                alertDialog.setButton("刷新", new DialogInterface.OnClickListener(){  
                    @Override  
                    public void onClick(DialogInterface dialog, int which) {  
                        // TODO Auto-generated method stub
                    	toweb.loadUrl(failingUrl);
                    }  
                });  
                alertDialog.show(); 
                super.onReceivedError(view, errorCode, description, failingUrl);
            }  
             });
	}
	// 初始化 JPush。如果已经初始化，但没有登录成功，则执行重新登录。
		private void init(){
			 JPushInterface.init(getApplicationContext());
		}

		public void onReceivedError(WebView view, int errorCode,  
                 String description, String failingUrl) {  
             Toast.makeText(MainActivity.this, "网页加载出错！", Toast.LENGTH_LONG);  
               
             alertDialog.setTitle("ERROR");  
             alertDialog.setMessage(description);  
             alertDialog.setButton("OK", new DialogInterface.OnClickListener(){  
                 @Override  
                 public void onClick(DialogInterface dialog, int which) {  
                     // TODO Auto-generated method stub  
                 }  
             });  
             alertDialog.show();  
         }  

		@Override
	     protected void onResume(){
	         super.onResume();
			registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	         //JPushInterface.onResume(this);
	     }
	     
	     @Override
	     protected void onPause(){
	         super.onPause();
			 unregisterReceiver(mGattUpdateReceiver);
	         //JPushInterface.onPause(this);
	     }
	     private long exitTime = 0;
	     @Override
	     public void onDestroy(){
	         super.onDestroy();
			 if(onbind_service){
				 unbindService(mServiceConnection);
				 mBluetoothLeService=null;
			 }
	         System.exit(0);  // 出现空白页的地方，找了哥哥我好久啊啊啊啊啊
	         //do something
	     }
	     @Override
	 	public boolean onKeyDown(int keyCode, KeyEvent event) {
	 		if (keyCode == KeyEvent.KEYCODE_BACK && toweb.canGoBack()) {
				String current_url=toweb.getUrl();
	 			if (current_url.equals("http://test.senguo.cc/customer/login?next=%2Fmadmin%3Fjpush_id%3D"+jpush_id)) {
	 				 new AlertDialog.Builder(MainActivity.this).setTitle("退出提示")//设置对话框标题  		  
	 			     .setMessage("你确定退出森果吗！")//设置显示的内容  
	 			     .setPositiveButton("确定",new DialogInterface.OnClickListener() {//添加确定按钮  
	 			         @Override  
	 			         public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件  	  
	 			             // TODO Auto-generated method stub
							 Intent intent=new Intent(MainActivity.this,BluetoothService.class);
							 stopService(intent);
	 			             finish();  
	 			         }  
	 			     }).setNegativeButton("取消",new DialogInterface.OnClickListener() {//添加返回按钮  
	 			         @Override  
	 			         public void onClick(DialogInterface dialog, int which) {//响应事件  
	 			             // TODO Auto-generated method stub
	 			         }  
	 			     }).show();//在按键响应事件中显示此对话框  
//	 				finish();
//	 				System.exit(0);
	 				return true;
	 			}
	 			if(current_url.equals("http://test.senguo.cc/madmin?jpush_id="+jpush_id))
				{
					new AlertDialog.Builder(MainActivity.this).setTitle("退出提示")//设置对话框标题  		  
				     .setMessage("你确定退出森果吗！")//设置显示的内容  
				     .setPositiveButton("确定",new DialogInterface.OnClickListener() {//添加确定按钮  
				         @Override  
				         public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件  	  
				             // TODO Auto-generated method stub
							 Intent intent=new Intent(MainActivity.this,BluetoothService.class);
							 stopService(intent);
				             finish();  
				         }  
				     }).setNegativeButton("取消",new DialogInterface.OnClickListener() {//添加返回按钮  
				         @Override  
				         public void onClick(DialogInterface dialog, int which) {//响应事件  
				             // TODO Auto-generated method stub
				         }  
				     }).show();//在按键响应事件中显示此对话框  
				}
				else if(current_url.indexOf("xui.ptlogin2.qq.com")!=-1){
					toweb.loadUrl("http://test.senguo.cc/madmin?jpush_id="+ jpush_id);
				}else if(current_url.indexOf("/mcrm/home")!=-1){

					new AlertDialog.Builder(MainActivity.this).setTitle("退出提示")//设置对话框标题
							.setMessage("你确定退出收银页面吗？退出后将会丢失收银信息！")//设置显示的内容
							.setPositiveButton("确定",new DialogInterface.OnClickListener() {//添加确定按钮
								@Override
								public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件
									// TODO Auto-generated method stub
									toweb.goBack();
								}
							}).setNegativeButton("取消",new DialogInterface.OnClickListener() {//添加返回按钮
						@Override
						public void onClick(DialogInterface dialog, int which) {//响应事件
							// TODO Auto-generated method stub
						}
					}).show();//在按键响应事件中显示此对话框
				}
				else{
					toweb.goBack();
				}
	 			return true;
	 		}
	 		if (keyCode == KeyEvent.KEYCODE_BACK
	 				&& event.getAction() == KeyEvent.ACTION_DOWN) {
	 			if ((System.currentTimeMillis() - exitTime) > 2000) {
	 				Toast.makeText(getApplicationContext(), "再按一次退出程序",
	 						Toast.LENGTH_SHORT).show();
	 				exitTime = System.currentTimeMillis();
	 			} else {
	 				finish();
	 				// System.exit(0);
	 			}
	 			return true;
	 		}
	 		return super.onKeyDown(keyCode, event);
	 	}
	     
	     @Override
	 	protected void onActivityResult(int requestCode, int resultCode,
	 			Intent intent) {
	 		if (requestCode == 1) {

	 			if (valueCallback == null)
	 				return;

	 			Uri result = intent == null || resultCode != RESULT_OK ? null
	 					: intent.getData();
				valueCallback.onReceiveValue(result);
				valueCallback = null;
//	 			if (result != null) {
//
//	 				if ("content".equals(result.getScheme())) {
//
//	 					Cursor cursor = this
//	 							.getContentResolver()
//	 							.query(result,
//	 									new String[] { android.provider.MediaStore.Images.ImageColumns.DATA },
//	 									null, null, null);
//	 					cursor.moveToFirst();
//	 					filePath = cursor.getString(0);
//	 					cursor.close();
//	 					System.out.println(filePath);
//
//	 				} else {
//	 					System.out.println(filePath);
//	 					filePath = result.getPath();
//
//	 				}
//	 				overtime=System.currentTimeMillis();
//	 				uploadImg();
//	 				prDialog = ProgressDialog.show(MainActivity.this, null, "正在上传图片,请耐心等待...");
//	 			} else {
//
//	 				valueCallback.onReceiveValue(result);
//	 				valueCallback = null;
//	 			}

	 		}
        else if(requestCode==200) {
				 switch (resultCode) {
					 case Activity.RESULT_OK:
						 String num = intent.getStringExtra(Intents.Scan.RESULT);
						 boolean isNum = isNumeric(num);

						 if (isNum==true) {
							 String url = "http://test.senguo.cc/madmin/orderDetail2/" + num;
							 toweb.loadUrl(url);
						 }else{
//							Toast.makeText(MainActivity.this,"订单号不正确，请扫描正确的订单号",Toast.LENGTH_SHORT);
							 AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
							 dialog.setMessage("订单号不正确，请扫描正确的订单号");
							 dialog.setPositiveButton("确定",null);
							 dialog.show();
						 }
						 break;
				 }
			 }
			//add by cm 2016.8.3 for order
			else if (requestCode==300){
				switch (resultCode){
					case Activity.RESULT_OK:
						String num = intent.getStringExtra(Intents.Scan.RESULT);
						boolean isNum = isNumeric(num);

						if (isNum==true) {
							toweb.loadUrl("javascript:getShopScanCb('"+num+"')");

						}else{
//							Toast.makeText(MainActivity.this,"订单号不正确，请扫描正确的订单号",Toast.LENGTH_SHORT);
							AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
							dialog.setMessage("二维码不正确，请扫描正确的二维码");
							dialog.setPositiveButton("确定",null);
							dialog.show();
						}
						break;
				}

			}else if(requestCode==400){
				switch (resultCode){
					case Activity.RESULT_OK:
						mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
						mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
						Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
						bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
						onbind_service=true;

				}
			}
	 	}

		public static boolean isNumeric(String str){
			for(int i = str.length() ; --i>=0;){
				if(!Character.isDigit(str.charAt(i))){
					return false;
				}
			}
			return true;
		}

	 	private void uploadImg() {
	 		   mythread=new Thread(new Runnable() {
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
	 											Message msg=new Message();
	 											// 上传图片 超时设置
	 											if ((System.currentTimeMillis() - overtime) > 2000*30) {
	 												msg.what=0;
	 												myhandler.sendMessage(msg);
	 											} 
	 											if (percent == 1.0) {
	 												msg.what=1;
	 												Bundle b=new Bundle();
	 												b.putDouble("percent", percent);
	 												msg.setData(b);
	 												myhandler.sendMessage(msg);
	 												Uri myUri = Uri.parse(filePath);
	 												valueCallback.onReceiveValue(myUri);
	 												toweb.loadUrl("javascript:uploadImgForAndroid('"+KEY+"')");
	 												valueCallback = null;
	 											}
	 										}
	 									}, new UpCancellationSignal() {
	 										public boolean isCancelled() {
	 											return isCancelled;
	 										}
	 										@SuppressWarnings("unused")
	 										private void cancell() {
	 										    isCancelled = true;
	 										    Message msg=new Message();
	 										    msg.what=2;
	 										    myhandler.sendMessage(msg);
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
	 		});
	 		mythread.start();
	 	}

	 	private String getUploadToken() {
	 		HttpGet httpRequest = new HttpGet(
	 				"http://test.senguo.cc/fruitzone/shop/apply/addImg");
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
	class WebAppInterface {
		Context mContext;

		/** Instantiate the interface and set the context */
		WebAppInterface(Context c) {
			mContext = c;
		}

		/** Show a toast from the web page */
		@JavascriptInterface
		public void showToast(String toast) {
			Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
		}
		@JavascriptInterface
		public boolean BeginGetShopScan(){
			myhandler.post(new Runnable() {
				public void run() {
					Intent intent = new Intent(MainActivity.this,CaptureActivity.class);
					startActivityForResult(intent,REQUEST_CODE_CM);
				}
			});
			return true;
		}
		@JavascriptInterface
		public boolean BeginGetWeight(){
			myhandler.post(new Runnable() {
				public void run() {
					float m=GlobalData.getWeight();
					toweb.loadUrl("javascript:getweightback('"+GlobalData.getWeight()+"')");
				}
			});
			return true;
		}

//		普通蓝牙链接方式
//		@JavascriptInterface
//		public boolean BluetoothSet(){
//			myhandler.post((new Runnable() {
//				@Override
//				public void run() {
//					BeginOpenBluetooth();//打开蓝牙设备和开始读取数据
//				}
//			}));
//			return true;
//		}
		//蓝牙4.0连接方式
		@JavascriptInterface
		public boolean BluetoothSet(){
			myhandler.post((new Runnable() {
				@Override
				public void run() {
					if(onbind_service){
						unbindService(mServiceConnection);
						mBluetoothLeService = null;
						onbind_service=false;
					}
					Intent intent = new Intent(MainActivity.this, DeviceScanActivity.class);
					startActivityForResult(intent,REQUEST_CODE_SCAN);
				}
			}));
			return true;
		}
	}
	//开始称重收银的取重以及开启蓝牙

	public void BeginOpenBluetooth(){
		//打开蓝牙设备
		Intent intent_blue = new Intent(MainActivity.this, BluetoothService.class);
		MainActivity.this.startService(intent_blue);
	}
	//实例化自己的广播接收处理
	private class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals("cc.senguo.SenguoAdmin.GetWeightFromBluetooth")) {
				boolean connected=intent.getBooleanExtra("connect_status",false);
				is_connected=connected;
				if(progressBar.isShowing()){
					progressBar.dismiss();
				}
				if(connected){
					Toast.makeText(MainActivity.this, "蓝牙连接成功......", Toast.LENGTH_LONG).show();
				}
				else{
					Toast.makeText(MainActivity.this, "蓝牙连接失败，请检查串口称是否正常工作！", Toast.LENGTH_SHORT).show();
				}
			}
			if(action.equals("cc.senguo.SenguoAdmin.BluetoothService")){
				choose_blooth_device();
			}
		}
	}

	public void choose_blooth_device(){
		//根据选择的设备链接蓝牙设备
		AlertDialog.Builder builder3 = new AlertDialog.Builder(MainActivity.this);
		builder3.setIcon(android.R.drawable.ic_dialog_info);
		builder3.setTitle("请选择蓝牙设备进行连接");
		final String[] devices_mac=GlobalData.getDevices_mac();
		final String[] devices_name=GlobalData.getDevices_name();
		builder3.setItems(devices_name, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				String tem_mac=devices_mac[which];
				connect_mac= devices_mac[which];
				//链接蓝牙设备并进行称重
				progressBar.setMessage("正在连接蓝牙："+ devices_name[which]);
				progressBar.show();
				if(is_connected && tem_mac.equals(GlobalData.getConnect_mac())){
					if(progressBar.isShowing()){
						progressBar.dismiss();
					}
					Toast.makeText(MainActivity.this, "蓝牙已经连接成功，不需重新连接！", Toast.LENGTH_LONG).show();
				}
				else{
					if(is_connected){
						Intent stop_intent=new Intent(MainActivity.this,GetWeightFromBluetooth.class);
						stopService(stop_intent);
//						MainActivity.this.unbindService(conn);
//						try {
//							Thread.sleep(200);
//						}catch (Exception e){
//							//// TODO: 16-8-5
//						}
//						Intent intent_weight = new Intent(MainActivity.this, GetWeightFromBluetooth.class);
//						GlobalData.setConnect_mac(connect_mac);
//						MainActivity.this.startService(intent_weight);

					}

						Intent intent_weight = new Intent(MainActivity.this, GetWeightFromBluetooth.class);
						GlobalData.setConnect_mac(connect_mac);
						MainActivity.this.startService(intent_weight);
				}
			}
		});
		 builder3.show();
	}
	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up initialization.
			mBluetoothLeService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};
	private View addViewtask() {
		// TODO 动态添加布局(xml方式)
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		LayoutInflater inflater3 = LayoutInflater.from(this);
		View view = inflater3.inflate(R.layout.taskcompleteview, null);
		view.setLayoutParams(lp);
		return view;
	}
	 TasksCompletedView ShowTaskCompleteView(){
		mTotalProgress = 100;
		mLl_parent=(RelativeLayout) findViewById(R.id.ll_parent);
		pop_view=addViewtask();
		 pop_view.setBackgroundColor(Color.TRANSPARENT);
		mLl_parent.addView(pop_view);
		mTasksView=(TasksCompletedView)pop_view.findViewById(R.id.tasks_view);
		return mTasksView;
	}

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
	//                        or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
//                mConnected = true;
//                updateConnectionState(R.string.connected);
//                invalidateOptionsMenu();
				Toast.makeText(MainActivity.this,"蓝牙连接成功.....",Toast.LENGTH_SHORT);
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//                mConnected = false;
//                updateConnectionState(R.string.disconnected);
//                invalidateOptionsMenu();
//                clearUI();
				Toast.makeText(MainActivity.this, "蓝牙连接失败，请检查串口称是否正常工作！", Toast.LENGTH_SHORT).show();
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				// Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());

			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				GlobalData.setWeight(intent.getFloatExtra(BluetoothLeService.EXTRA_DATA,0));
			}else if(Constants.CHOSE_OBE_BLE.equals(action)){
				mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
				mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
				Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
				bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
				onbind_service=true;
			}
		}
	};
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(Constants.CHOSE_OBE_BLE);
		return intentFilter;
	}
	// Demonstrates how to iterate through the supported GATT Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the ExpandableListView
	// on the UI.
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null) return;
		String uuid = null;
		String unknownServiceString = getResources().getString(R.string.unknown_service);
		String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
				= new ArrayList<ArrayList<HashMap<String, String>>>();
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();
			if(uuid.equals(Constants.TARGET_SERVICE_UUID)){
				currentServiceData.put(
						LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
				currentServiceData.put(LIST_UUID, uuid);
				gattServiceData.add(currentServiceData);

				ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
						new ArrayList<HashMap<String, String>>();
				List<BluetoothGattCharacteristic> gattCharacteristics =
						gattService.getCharacteristics();
				ArrayList<BluetoothGattCharacteristic> charas =
						new ArrayList<BluetoothGattCharacteristic>();

				// Loops through available Characteristics.
				for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
					charas.add(gattCharacteristic);
					HashMap<String, String> currentCharaData = new HashMap<String, String>();
					uuid = gattCharacteristic.getUuid().toString();
					//连接对应的gattCharacteristic
					if(uuid.equals(Constants.TARGET_CHAACTERISTIC_UUID)){
						final int charaProp = gattCharacteristic.getProperties();
						if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
							// If there is an active notification on a characteristic, clear
							// it first so it doesn't update the data field on the user interface.
							if (mNotifyCharacteristic != null) {
								mBluetoothLeService.setCharacteristicNotification(
										mNotifyCharacteristic, false);
								mNotifyCharacteristic = null;
							}
							mBluetoothLeService.readCharacteristic(gattCharacteristic);
						}
						if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
							mNotifyCharacteristic = gattCharacteristic;
							mBluetoothLeService.setCharacteristicNotification(
									gattCharacteristic, true);
						}
					}
					currentCharaData.put(
							LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
					currentCharaData.put(LIST_UUID, uuid);
					gattCharacteristicGroupData.add(currentCharaData);
				}
				mGattCharacteristics.add(charas);
				gattCharacteristicData.add(gattCharacteristicGroupData);


			}
		}
	}

}

 