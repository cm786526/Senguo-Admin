package cc.senguo.SenguoAdmin;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import cn.jpush.android.api.JPushInterface;

public class ViewPageActivity extends Activity {  
  
    ViewPager mViewPager;  
    //导航页图片资源  
    public int[] guides = new int[] { R.drawable.first,  
            R.drawable.second, R.drawable.third,  
            R.drawable.fourth};  
   String isFirstIn;
    @Override  
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences("localdata",
        	    MODE_PRIVATE);
        	isFirstIn = preferences.getString("isFirstIn", "");
        	Log.e("tag", isFirstIn);
        	if(isFirstIn!=""){
        		 finish();
        		 Intent intent=new Intent(ViewPageActivity.this,MainActivity.class);
                 intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                 intent.putExtra("activity_flag", 0);
                 startActivity(intent);
                 finish();
        	}
        setContentView(R.layout.viewpage_main); 
        mViewPager = (ViewPager) findViewById(R.id.viewFlipper1);  
         initWithPageGuideMode();  
    }  
  
//    大图加载处理
    public static Bitmap readBitMap(Context context, int resId){  
	     BitmapFactory.Options opt = new BitmapFactory.Options();  
	     opt.inPreferredConfig = Bitmap.Config.RGB_565;   
	     opt.inPurgeable = true;  
        opt.inInputShareable = true;  
	       //获取资源图片  
	      InputStream is = context.getResources().openRawResource(resId);  
	      return BitmapFactory.decodeStream(is,null,opt);  
	 } 
  
    /** 
     * 程序导航页效果 
     */  
    
 
    
    public void initWithPageGuideMode() {  
            
        List<View> mList = new ArrayList<View>();  
        LayoutInflater inflat = LayoutInflater.from(this);  
        //先添加一个最左侧空的view  
        View item = inflat.inflate(R.layout.pageguide, null);  
        mList.add(item);  
        for (int index : guides) {  
            item = inflat.inflate(R.layout.pageguide, null);  
            Bitmap bp=readBitMap(this, index);
            BitmapDrawable bd = new BitmapDrawable(bp);
            item.setBackgroundDrawable(bd);  
            mList.add(item);  
        }  
        //经过遍历，此时item是最后一个view，设置button  
        Button btn = (Button) item.findViewById(R.id.button1);  
        btn.setVisibility(View.VISIBLE);  
        btn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				  finish();
				  Intent intent=new Intent(ViewPageActivity.this,MainActivity.class);
                  intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  intent.putExtra("activity_flag", 0);
                  startActivity(intent);
                finish();
			}
        });
        SharedPreferences  sp=getSharedPreferences("localdata", MODE_PRIVATE);
        Editor  edit=sp.edit();
        edit.putString("isFirstIn","second");
        edit.commit();
        //设置最后一个页面上button的监听  
        //再添加一个最右侧空的view  
        item = inflat.inflate(R.layout.pageguide, null);  
        mList.add(item);  
        //ViewPager最重要的设置Adapter，这和ListView一样的原理  
        MViewPageAdapter adapter = new MViewPageAdapter(mList);  
        mViewPager.setAdapter(adapter);  
        mViewPager.setOnPageChangeListener(adapter);  
        mViewPager.setCurrentItem(1);  
  
    }  
  
    /** 
     * 内部类，继承PagerAdapter，当然你也可以直接 new PageAdapter 
     *  
     * @author yangxiaolong 
     *  
     */  
    class MViewPageAdapter extends PagerAdapter implements OnPageChangeListener {  
  
        private List<View> mViewList;  
  
        public MViewPageAdapter(List<View> views) {  
            mViewList = views;  
        }  
  
        @Override  
        public int getCount() {  
            return mViewList.size();  
        }  
  
        @Override  
        public boolean isViewFromObject(View arg0, Object arg1) {  
  
            return arg0 == arg1;  
        }  
  
        @Override  
        public Object instantiateItem(ViewGroup container, int position) {  
            container.addView(mViewList.get(position), 0);  
            return mViewList.get(position);  
        }  
  
        @Override  
        public void destroyItem(ViewGroup container, int position, Object object) {  
            container.removeView(mViewList.get(position));  
        }  
  
        @Override  
        public void onPageScrollStateChanged(int arg0) {  
  
        }  
  
        @Override  
        public void onPageScrolled(int arg0, float arg1, int arg2) {  
  
        }  
  
        @Override  
        public void onPageSelected(int position) {  
  
                if (position == 0) {  
                    mViewPager.setCurrentItem(1);  
                } else if (position == mViewList.size() - 1) {  
                    mViewPager.setCurrentItem(position - 1);  
                    Toast.makeText(getApplicationContext(), "页面即将跳转",
                            Toast.LENGTH_SHORT).show();
                    //应该在这里跳转到MainActivity  
                    // startActivity(intent);  
                    SharedPreferences  sp=getSharedPreferences("localdata", MODE_PRIVATE);
                    SharedPreferences.Editor edit=sp.edit();
                    edit.putString("isFirstIn", "second");
                    edit.commit();
                    Intent intent=new Intent(ViewPageActivity.this,MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("activity_flag", 0);
                    startActivity(intent);
                    finish();
                }  
      
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
