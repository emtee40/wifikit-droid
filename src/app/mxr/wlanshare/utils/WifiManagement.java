package app.mxr.wlanshare.utils;

import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.text.TextUtils;
import android.util.Log;
import app.mxr.wlanshare.activities.Activity_Receive;

public class WifiManagement implements Runnable{
	
	private WifiManager wifimanager;
	private List<ScanResult> scanresultlist; 
	 
	 WifiLock wifilock;
	 Thread thread_openwifi=null;
	 String index,SSID;
	 
	 public static final int WIFI_TYPE_OPEN   =   0x1;
	 public static final int WIFI_TYPE_WEP    =   0x2;
	 public static final int WIFI_TYPE_WPA    =   0x3;
	 public static final int WIFI_TYPE_UNKNOWN=   0x4;
	 
	
	public WifiManagement(Context context){
		wifimanager=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		this.SSID="";
		this.index="";
	}
	
	public boolean setWifiEnabled(boolean isEnabled){
		if(isEnabled){
			if(!wifimanager.isWifiEnabled()){
				return wifimanager.setWifiEnabled(true);
			}
		}
		else{
			if(wifimanager.isWifiEnabled()){
				return wifimanager.setWifiEnabled(false);
			}
			
		}
		return true;
		
	}
	
	public boolean isWifiEnabled(){
		return wifimanager.isWifiEnabled();
	}
	
	public int getWifiState(){
		return wifimanager.getWifiState();
	}
	
	public void acquireWifiLock(String tag){
		wifilock=wifimanager.createWifiLock(tag);
		wifilock.acquire();
	}
	
	public void releaseWifiLock(){
		wifilock.release();
	}
	
	public String getWifiList() {	
		StringBuilder stringBuilder = new StringBuilder();  
		for (int i = 0; i < scanresultlist.size(); i++) {  
		stringBuilder.append("Index_" + Integer.valueOf(i + 1).toString() + ":");  
		// ��ScanResult��Ϣת����һ���ַ�����   
		// ���аѰ�����BSSID��SSID��capabilities��frequency��level   
		stringBuilder.append((scanresultlist.get(i)).toString());  
		stringBuilder.append("\n");  
		}  
		        
		return stringBuilder.toString(); 	
	} 
	
	/**
	 * ��ȡ��ǰ���ӵ�wifi��SSID
	 */
	public String getSSID(){
		return wifimanager.getConnectionInfo().getSSID().toString();
	}
	
	/**
	 * �жϵ�ǰ�Ƿ��Ѿ������Ͽ��õ�wifi
	 * @return boolean �Ƿ��������Ͽ���wifi
	 * 
	 */
	public boolean isWifiConnected(){
	/*	if(wifimanager.getConnectionInfo().getSSID()==null
				||wifimanager.getConnectionInfo().getSSID().trim().equals("<unknown ssid>")
				||wifimanager.getConnectionInfo().getSSID().trim().equals("")
				||wifimanager.getConnectionInfo().getSSID().length()==0
				||!wifimanager.isWifiEnabled()){
			return false;
		 }
		 else{
			 Log.i("WifiManagement", "ͨ��Wifimanagement��isWifiConnected�����жϵ�ǰ�Ѿ�������Wifi��SSIDֵΪ  "+getSSID());
			return true;
		 }     */
		
		WifiInfo wifiInfo=wifimanager.getConnectionInfo();
		int ipAddress = wifiInfo == null ? 0 : wifiInfo.getIpAddress();
		if (isWifiEnabled() && ipAddress != 0){
			return true;
		}
		else{
			return false;
		}
		
	}
	
	/**
	 * ָ��Ҫ���ӵ�WIFI��SSID��Ҫ�����Ĺؼ���
	 * @param index : �ؼ���
	 * 
	 */
	public void setSSIDIndex(String index){
		this.index=index;
	}
	
	/**
	 * ���ӵ������ð����ؼ��ֵ�SSID��wifi
	 * ִ�д˷���ǰ������ͨ�� ִ��setSSIDIndex(String index)�������ùؼ���
	 */
	public void connecttoOpenWifi(){
		if(thread_openwifi!=null){
			thread_openwifi.interrupt();
			thread_openwifi=null;
		}
		thread_openwifi=new Thread(this);	
		thread_openwifi.start();
	}
	
	public void stopConnecttoOpenWifi(){
		if(thread_openwifi!=null){
			thread_openwifi.interrupt();
			thread_openwifi=null;
		}
	}
	
	
	@Override
	public void run(){
		// TODO Auto-generated method stub
		this.SSID="";
		try{
			if(!setWifiEnabled(true)){
				Activity_Receive.sendEmptyMessage(Activity_Receive.MESSAGE_OPEN_WIFI_FAILED);
				if(thread_openwifi!=null){
					thread_openwifi.interrupt();
					thread_openwifi=null;
				}
			}
			Thread.currentThread();
			Thread.sleep(200);
			while(wifimanager.getWifiState()==WifiManager.WIFI_STATE_ENABLING){				
					Thread.sleep(100);												
			}			
			while(this.SSID==""){
				if(wifimanager.getWifiState()==WifiManager.WIFI_STATE_ENABLED){					
					Thread.sleep(200);
					this.scanresultlist=wifimanager.getScanResults();														
				}
				
				for(int i=0;i<scanresultlist.size();i++){
					String scannedSSID=scanresultlist.get(i).SSID;
					
					if(scannedSSID.indexOf(index)!=-1){
						this.SSID=scannedSSID;
						break;						
					}
				}
				
				Thread.sleep(200);				
			}
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		if(this.SSID!=""){
			WifiConfiguration wifiConfig = createWifiInfo(this.SSID, "123456789000",
	                WifiManagement.WIFI_TYPE_OPEN);
			if (wifiConfig == null) {
	           // sendMsg("wifiConfig is null!");
	            return;
	        }
	        
			WifiConfiguration tempConfig= isConfigExsits(this.SSID);
			if (tempConfig != null) {
	            wifimanager.removeNetwork(tempConfig.networkId);
	            Log.e("WifiManagement", "���Ƴ��Ѵ��ڵ���������"+this.SSID);
	        }
			int netID = wifimanager.addNetwork(wifiConfig);
			
			wifimanager.disconnect();
			wifimanager.enableNetwork(netID, true);
			Log.e("WifiManagement", "�ѳ������ӵ�����"+this.SSID);
			wifimanager.reconnect();
			Log.e("WifiManagement", "�ѳ�������������"+this.SSID);
			while(!isWifiConnected()){
				try{
					Thread.sleep(1000);
					Log.i("WifiManagerment", "���ڵȴ�wifi����");
				}catch(Exception e){
					e.printStackTrace();
				}
				if(isWifiConnected()){					
					Activity_Receive.sendEmptyMessage(0x2);   //0x2ֵ��Activity_Receive����private 
					break;
				}
			}
			
	
		}             
       	
	}
	
	 private WifiConfiguration createWifiInfo(String SSID, String Password,
	            int Type) {
	        WifiConfiguration config = new WifiConfiguration();
	        config.allowedAuthAlgorithms.clear();
	        config.allowedGroupCiphers.clear();
	        config.allowedKeyManagement.clear();
	        config.allowedPairwiseCiphers.clear();
	        config.allowedProtocols.clear();
	        config.SSID = "\"" + SSID + "\"";
	        // nopass
	        if (Type ==WifiManagement.WIFI_TYPE_OPEN) {
	            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
	        }
	        // wep
	        if (Type == WifiManagement.WIFI_TYPE_WEP) {
	            if (!TextUtils.isEmpty(Password)) {
	                if (isHexWepKey(Password)) {
	                    config.wepKeys[0] = Password;
	                } else {
	                    config.wepKeys[0] = "\"" + Password + "\"";
	                }
	            }
	            config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
	            config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
	            config.allowedKeyManagement.set(KeyMgmt.NONE);
	            config.wepTxKeyIndex = 0;
	        }
	        // wpa
	        if (Type == WifiManagement.WIFI_TYPE_WPA) {
	            config.preSharedKey = "\"" + Password + "\"";
	            config.hiddenSSID = true;
	            config.allowedAuthAlgorithms
	                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
	            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
	            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
	            config.allowedPairwiseCiphers
	                    .set(WifiConfiguration.PairwiseCipher.TKIP);
	            // �˴���Ҫ�޸ķ������Զ�����
	             config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
	            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
	            config.allowedPairwiseCiphers
	                    .set(WifiConfiguration.PairwiseCipher.CCMP);
	            config.status = WifiConfiguration.Status.ENABLED;
	        }
	        return config;
	    }
	 
	
	 private static boolean isHexWepKey(String wepKey) {
	        final int len = wepKey.length();

	        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
	        if (len != 10 && len != 26 && len != 58) {
	            return false;
	        }

	        return isHex(wepKey);
	    }

	 private static boolean isHex(String key) {
	        for (int i = key.length() - 1; i >= 0; i--) {
	            final char c = key.charAt(i);
	            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
	                    && c <= 'f')) {
	                return false;
	            }
	        }

	        return true;
	}
	 private WifiConfiguration isConfigExsits(String SSID) {
	        List<WifiConfiguration> existingConfigs = wifimanager
	                .getConfiguredNetworks();
	        for (WifiConfiguration existingConfig : existingConfigs) {
	            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
	                return existingConfig;
	            }
	        }
	        return null;
	    }
	 
	  
	
}
