package cc.senguo.SenguoAdmin.wxapi;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import cn.jpush.android.api.JPushInterface;

import cc.senguo.SenguoAdmin.Constants;
import cc.senguo.SenguoAdmin.MainActivity;
import cc.senguo.SenguoAdmin.R;
import cc.senguo.SenguoAdmin.Senguo_admin;
import cc.senguo.SenguoAdmin.ViewPageActivity;
import com.tencent.mm.sdk.openapi.BaseReq;
import com.tencent.mm.sdk.openapi.BaseResp;
import com.tencent.mm.sdk.openapi.ConstantsAPI;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.SendAuth;
import com.tencent.mm.sdk.openapi.SendAuth.Resp;
import com.tencent.mm.sdk.openapi.WXAPIFactory;



public class WXEntryActivity extends Activity implements IWXAPIEventHandler{
	
	private static final int TIMELINE_SUPPORTED_VERSION = 0x21020001;

	private static final String TAG_GET = null;
	private Handler handler; 
	private Button gotoBtn;
	
	// IWXAPI �ǵ�����app��΢��ͨ�ŵ�openapi�ӿ�
    public static  IWXAPI api;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ͨ��WXAPIFactory��������ȡIWXAPI��ʵ��
               if(api==null){  // 创建微信api对象
            	   api = WXAPIFactory.createWXAPI(this, Constants.APP_ID, false);
               }
//				Toast.makeText(WXEntryActivity.this,"success~~~~~~??????????",Toast.LENGTH_LONG).show();
//    	        注册appid
			    api.registerApp(Constants.APP_ID);  
			    api.handleIntent(getIntent(),this);
				 if (!api.isWXAppInstalled()){  //判断用户是否安装了微信客户端
			        	Toast.makeText(WXEntryActivity.this,"你还没有安装微信，请安装合适版本的微信",Toast.LENGTH_LONG).show();
			          return;
			        }
				 
//				 发送启动页面请求
		        final SendAuth.Req req=new SendAuth.Req();
//		        req.openId=Constants.APP_ID;
		        req.scope="snsapi_userinfo";
		        req.state="carjob_wx_login";
		        api.sendReq(req);
//		        onReq(req);
//		        final SendAuth.Resp resp=new SendAuth.Resp();
//		        onResp(resp);
   	          
//		        onResp(req);
//		        int wxSdkVersion = api.getWXAppSupportAPI();
//				if (wxSdkVersion <TIMELINE_SUPPORTED_VERSION) {
//					Toast.makeText(WXEntryAcrivity.this, "wxSdkVersion = " + Integer.toHexString(wxSdkVersion) + "\ntimeline supported", Toast.LENGTH_LONG).show();
//				} 
//				 api.openWXApp();  //打开微信 
		       
			}
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
        api.handleIntent(intent, this);
	}

	// ΢�ŷ������󵽵�����Ӧ��ʱ����ص����÷���
	@Override
	public void onReq(BaseReq req) {
		switch (req.getType()) {
		case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:
			break;
		case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX:
			break;
//			授权
		case ConstantsAPI.COMMAND_SENDAUTH:
			break;
		default:
			break;
		}
	}

	// ������Ӧ�÷��͵�΢�ŵ�����������Ӧ�������ص����÷���
	@Override
	public void onResp(BaseResp resp) {
		int result = 0;
		 Bundle bundle=new Bundle();
		 Intent intent=new Intent(WXEntryActivity.this,MainActivity.class);
		switch (resp.errCode) {
		case BaseResp.ErrCode.ERR_OK:
			result = R.string.errcode_success;
			resp.toBundle(bundle);
	        Resp sp=new Resp(bundle);
	        final  String code=sp.token;
	        SharedPreferences settings = getSharedPreferences("setting", 0);
	        SharedPreferences.Editor editor = settings.edit();
	        editor.remove("code");
	        editor.commit();
	        editor.putString("code",code);
	        editor.commit();
	        new Thread(){
	        	@Override
	        	public void run(){
	        	goToGetMsg(code);
	        		
	        	}
	        }.start();
	        this.finish();
	        break;
		case BaseResp.ErrCode.ERR_USER_CANCEL:
			result = R.string.errcode_cancel;
			Toast.makeText(this, "用户取消授权", Toast.LENGTH_LONG).show();
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.putExtra("activity_flag", 0);
//            startActivity(intent);
            finish();
			break;
		case BaseResp.ErrCode.ERR_AUTH_DENIED:
//			Toast.makeText(this, "用户拒绝授权", Toast.LENGTH_LONG).show();
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.putExtra("activity_flag", 0);
//            startActivity(intent);
//            finish();
			break;
		case ConstantsAPI.COMMAND_SENDAUTH:
            break;
		default:
			result = R.string.errcode_unknown;
			break;
		}
	}
	
	private void goToGetMsg(String code) {
		String result = null;
		 // http地址
        URL url = null;
        HttpURLConnection connection = null;
        InputStreamReader in = null;
        try {
            url = new URL("https://api.weixin.qq.com/sns/oauth2/access_token?appid="+Constants.APP_ID+"&secret="+Constants.APP_SECRET+"&code="+code+"&grant_type=authorization_code");
            connection = (HttpURLConnection) url.openConnection();
            in = new InputStreamReader(connection.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(in);
            StringBuffer strBuffer = new StringBuffer();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                strBuffer.append(line);
            }
            result = strBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
 
        }
        String ACCESS_TOKEN=null;
        String OPENID=null;
           try {
			JSONObject datajson=new JSONObject(result);
			ACCESS_TOKEN=datajson.getString("access_token");
//		    OPENID=datajson.getString("unionid");
          OPENID = datajson.getString("openid");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
          
		         String httpUrl="https://api.weixin.qq.com/sns/userinfo?access_token="+ACCESS_TOKEN+"&openid="+OPENID;
//      		  String httpUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid="+Constants.APP_ID+"&secret="+Constants.APP_SECRET+"&code="+code+"&grant_type=authorization_code";
      		  // HttpGet连接对象
      		  HttpGet httpRequest = new HttpGet(httpUrl);
      		  try 
      		  {
      		   // 取得HttpClient对象
      		   HttpClient httpclient = new DefaultHttpClient();
      		   // 请求HttpClient，取得HttpResponse
      		   HttpResponse httpResponse = httpclient.execute(httpRequest);
      		   // 请求成功
      		   if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) 
      		   {
      		     // 取得返回的字符串
      		     String strResult = EntityUtils.toString(httpResponse.getEntity());
      		   JSONObject  user;
      			String unionid = null;
      			String openid = null;
      			String country=null;
      			String province=null;
      			String city=null;
      			String headimgurl=null;
      			String nickname=null;
      			int sex=0;
      		   try {
      			   user=new JSONObject(strResult);
      			   unionid=user.getString("unionid");
      		      openid=user.getString("openid");
      				country=user.getString("country");
      				province=user.getString("province");
      				 city=user.getString("city");
      				 headimgurl=user.getString("headimgurl");
      				 nickname=user.getString("nickname");
      			    sex=user.getInt("sex");
      			} catch (JSONException e) {
      				// TODO Auto-generated catch block
      				e.printStackTrace();
      			}
				   if (unionid != null) {
					   String info = "http://test.senguo.cc/customer/weixinphoneadmin?openid=" + openid + "&unionid=" + unionid + "&country=" + country + "&province=" +
							   province + "&city=" + city + "&headimgurl=" + headimgurl + "&nickname=" + nickname + "&sex=" + sex;
					   Intent intent = new Intent(WXEntryActivity.this, MainActivity.class);
					   Bundle bundle = new Bundle();
					   bundle.putCharSequence("url", info);
					   intent.putExtra("activity_flag", 2);
					   intent.putExtras(bundle);
					   startActivity(intent);
					   finish();
				   }else{
					   Log.e("error","获取用户信息失败");
				   }
      		   } 
      		  else 
      		   {
      		     Log.e("error","请求错误!");
      		   }
      		  } 
      		catch (ClientProtocolException e) 
      		  {
      		   Log.e("duizhan",e.getMessage().toString());
      		  } 
      		catch (IOException e) 
      		  {
      			Log.e("duizhan",e.getMessage().toString());
      		  } 
      		catch (Exception e) 
      		  {
      			Log.e("duizhan",e.getMessage().toString());
      		  }
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
}
