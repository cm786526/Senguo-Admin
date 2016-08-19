package cc.senguo.SenguoAdmin;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import cc.senguo.SenguoAdmin.wxapi.WXEntryActivity;

import cn.jpush.android.api.InstrumentedActivity;
import cn.jpush.android.api.JPushInterface;

public class Main2Activity extends InstrumentedActivity {
    final static String murl="http://test.senguo.cc/customer/weixin";
    final static String qqurl = "http://test.senguo.cc/customer/qq?next=";
    final static String childurl="http://test.senguo.cc/customer/login";
    final String jpush_id= JPushInterface.getRegistrationID(this);

    WebView toweb2;
    ValueCallback<Uri> valueCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        final Intent intent=this.getIntent();
        String url = intent.getStringExtra("url");
        toweb2=(WebView)findViewById(R.id.toweb2);
        toweb2.loadUrl(url);
        WebSettings webSettings = toweb2.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("GBK");//设置字符编码
//        toweb2.addJavascriptInterface(new WebAppInterface(this), "Android");
        toweb2.setWebChromeClient(new WebChromeClient() {

            // Android > 4.1.1 调用这个方法
            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType,
                                        String capture) {
                valueCallback = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "完成操作需要使用"),
                        1);

            }

            // 3.0 + 调用这个方法
            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                        String acceptType) {
                valueCallback = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(
                        Intent.createChooser(intent, "完成操作需要使用"), 1);
            }

            // Android < 3.0 调用这个方法
            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                valueCallback = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(
                        Intent.createChooser(intent, "完成操作需要使用"), 1);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message,
                                     final JsResult result) {
                AlertDialog.Builder b2 = new AlertDialog.Builder(
                        Main2Activity.this)
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
        toweb2.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                //  重写此方法表明点击网页里面的链接还是在当前的webview里跳转，不跳到浏览器那边
                if (url.equals(murl)) {
                    Intent intent = new Intent(Main2Activity.this,WXEntryActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  //注意本行的FLAG设置
                    startActivity(intent);
                    finish();
                    return true;
                } else if (url.equals("http://test.senguo.cc/customer/logout")) {
                    view.loadUrl(url + "?user_type=0&jpush_id=" + jpush_id);
                    Intent intent=new Intent(Main2Activity.this,MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  //注意本行的FLAG设置
                    intent.putExtra("activity_flag", 0);
                    startActivity(intent);
//                    finish();
                    return true;
                } else if (url.equals("http://test.senguo.cc/customer/login")) {
                    toweb2.loadUrl("http://test.senguo.cc/madmin?jpush_id=" + jpush_id);
                    CookieSyncManager.createInstance(Main2Activity.this);
                    CookieSyncManager.getInstance().startSync();
                    CookieManager.getInstance().removeSessionCookie();

                    return true;
                } else if (url.equals("http://test.senguo.cc/customer/qq?next=/madmin?jpush_id=0802773f7aa")) {
                  view.loadUrl(url);
                    return true;
                } else {
                    Toast.makeText(Main2Activity.this, url, Toast.LENGTH_SHORT).show();
                    System.out.println(url);
                    view.loadUrl(url);
                    return true;
                }
            }
         });

    }
}
