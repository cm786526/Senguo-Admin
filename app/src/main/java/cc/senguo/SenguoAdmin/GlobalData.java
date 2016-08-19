package cc.senguo.SenguoAdmin;

/**
 * Created by skipjack on 16-8-4.
 */
public class GlobalData {
    private static String[] devices_name;   //全局保存获取的配对蓝牙设备信息
    private static  String[] devices_mac;
    private static  float weight=0;
    private static  String connect_mac; //选择链接的蓝牙的MAC
    private static boolean  connected=false;
    private static boolean is_new_intent=false;
    private static boolean finish_scan_blooth=false;
    public static void setDevices_name(String[] m_device_name) {
       devices_name = m_device_name;
    }

    public static void setDevices_mac(String[]m_device_mac){
        devices_mac=m_device_mac;
    }

    public static void setConnect_mac(String m_connect_mac){
        connect_mac=m_connect_mac;
    }

    public static void setWeight(float m_weight){
        weight=m_weight;
    }

    public static void setConnected(boolean m_connected){connected=m_connected;}
    public static void setIs_new_intent(boolean m_is_new_intent){is_new_intent=m_is_new_intent;}
    public static void setFinish_scan_blooth(boolean m_finish_scan_blooth){finish_scan_blooth=m_finish_scan_blooth;}
    public static String[] getDevices_name(){
        return devices_name;
    }

    public static String[] getDevices_mac(){
        return devices_mac;
    }

    public static String   getConnect_mac(){
        return  connect_mac;
    }
    public static float getWeight(){
        return weight;
    }
    public static boolean getConnected(){return connected;}
    public static boolean getis_new_intent(){return is_new_intent;}
    public static boolean getfinish_scan_blooth(){return finish_scan_blooth;}
}
