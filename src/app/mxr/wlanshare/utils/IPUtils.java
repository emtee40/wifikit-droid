package app.mxr.wlanshare.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

//import org.apache.http.conn.util.InetAddressUtils;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

public class IPUtils {
	
	/**
     * wifi��ȡ ·��ip��ַ
     *
     * @param context
     * @return String ·����IP��ַ
     */
	public static String getWifiRouteIPAddress(Context context) {
		WifiManager manager=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
	    DhcpInfo dhcpInfo = manager.getDhcpInfo();
//	    WifiInfo wifiinfo = manager.getConnectionInfo();	        
//	        System.out.println("Wifi info----->" + wifiinfo.getIpAddress());
//	        System.out.println("DHCP info gateway----->" + Formatter.formatIpAddress(dhcpInfo.gateway));
//	        System.out.println("DHCP info netmask----->" + Formatter.formatIpAddress(dhcpInfo.netmask));
	    //DhcpInfo�е�ipAddress��һ��int�͵ı�����ͨ��Formatter����ת��Ϊ�ַ���IP��ַ
	    String routeIp = Formatter.formatIpAddress(dhcpInfo.gateway);
	    Log.e("WifiManagerment Print out", "wifi route ip��" + routeIp);

	    return routeIp;
	}
	  
	/**
	 * ��ȡ����Ip
	 * @return String ����IP��ַ
	 */
	public static String getLocalIpAddress(Context context){
		WifiManager wifimanager=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		DhcpInfo dhcpinfo=wifimanager.getDhcpInfo();
		return Formatter.formatIpAddress(dhcpinfo.ipAddress);
		
		
		
		/*	try{
			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); 
			while(en.hasMoreElements()){
				NetworkInterface nif = en.nextElement();
				Enumeration<InetAddress> enumIpAddr = nif.getInetAddresses();
				while(enumIpAddr.hasMoreElements()){
					InetAddress mInetAddress = enumIpAddr.nextElement();
					if(!mInetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(mInetAddress.getHostAddress())){
						return mInetAddress.getHostAddress().toString();
					}
				}
			}
		}catch(SocketException ex){
			Log.e("IPUtils", "��ȡ����IP��ַʧ��");
		}  */
			
		//return null;
	}
	/**
	 * ��ȡ�����ӱ����ȵ�������豸������һ���豸��IP��ַ
	 * 	
	 */
	
	public static  String getanIPfromARP(){
		String anIP="";
		BufferedReader reader = null;
		try {
		    reader = new BufferedReader(new FileReader("/proc/net/arp"));
		    String line = reader.readLine();
		    //��ȡ��һ����Ϣ������IP address HW type Flags HW address Mask Device
		    while ((line = reader.readLine()) != null) {
		        String[] tokens = line.split("[ ]+");
		        if (tokens.length < 6) {
		            continue;
		        }
		        String ip = tokens[0]; //ip
		        anIP=ip;
		    //    String mac = tokens[3];  //mac ��ַ
		     //  String flag = tokens[2];//��ʾ����״̬
		    }
		} catch (FileNotFoundException e) {
		} catch (IOException e) {  
		} finally {
		    try {
		        if (reader != null) {
		            reader.close();
		        }
		    }
		    catch (IOException e) {
		    }
		}
		return anIP;
	}
}
