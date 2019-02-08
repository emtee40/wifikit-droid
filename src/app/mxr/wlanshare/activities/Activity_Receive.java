package app.mxr.wlanshare.activities;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.wlanshare.R;
import app.mxr.wlanshare.net.IpMessageConst;
import app.mxr.wlanshare.net.IpMessageProtocol;
import app.mxr.wlanshare.net.NetReceiveThread;
import app.mxr.wlanshare.net.NetTcpFileReceiveThread;
import app.mxr.wlanshare.ui.FileTransferDialog;
import app.mxr.wlanshare.ui.FileTransferNotification;
import app.mxr.wlanshare.ui.NotificationManagement;
import app.mxr.wlanshare.utils.IPUtils;
import app.mxr.wlanshare.utils.StorageUtil;
import app.mxr.wlanshare.utils.WifiManagement;

public class Activity_Receive extends BaseActivity{
	ImageView img_done;
	ProgressBar pg;
	TextView attention,ssidinfo;
	RelativeLayout bottomarea;
	boolean  isConnected=false;
	public static boolean isAPMode=false;
	private FileTransferDialog receivingFileDialog;
	AlertDialog ad_receivefile;
	WifiManagement wmanage;
	protected static LinkedList<Activity_Receive> queue=new LinkedList<Activity_Receive>();
	private static final int NO_WIFI_AVALIABLE =   0x1;
	private static final int WIFI_CONNECTED    =   0x2;
	private static final String SSID_INDEX="mxr-wlanshare-";
	private Thread wait10s=null;
	private boolean ifneedCloseWifi=false;
	protected static NetReceiveThread netThread=null;
	private String senderip;
	Thread fileReceiveThread;
	List<String[]> filevalues;
	long totalBytes=10;
	public static boolean isReceivingFileSuccess=true;
	private  boolean ifcancallclose=true;
	private boolean isinReceivingProcess=false;
	private boolean isatFront=false;
	private FileTransferNotification fnotification;
	private NotificationManagement notificationmanagement;
	public static final int 
			MESSAGE_WIFI_CLOSED_MANNUAL			=0x00019,			//wifi���ֶ��رգ���������������ֹͣ
			MESSAGE_OPEN_WIFI_FAILED			=0x00020, 			//��wifiʧ��
			MESSAGE_REFRESH_FILEDIALOG_PROGRESS =0x00025,          //ˢ�´����ļ��Ի���Ľ���
			MESSAGE_START_RECEIVING_FILES		=0x00024,             //��ʼ�����ļ�
			MESSAGE_REFRESH_FILEDIALOG_SPEED	=0X00023,
			MESSAGE_REFUSED_RECEIVING_FILES		=0X00022,		//�ܾ������ļ�
			MESSAGE_RECEIVING_FILES_COMPLETE	=0x00026,		//�����ļ����
			MESSAGE_RECEIVINGFILES_INTERRUPT	=0x00027,      //������;�ж�
			MESSAGE_RECEIVING_CURRENTFILE		=0x00028,        //��ǰ���յ��ļ�
			MESSAGE_DISMISS_REQUESTDIALOG		=0x00030,       //  �Է��ȴ����˻ظ�ʱȡ���˷������󣬱��˹ر�����Ի���
			MESSAGE_RECEIVINGFILES_INTERRUPT_SELF=0x00031;		//�����ֶ�ֹͣ����
			
	
	private static Handler handler=new Handler(){
		
		public void handleMessage(Message msg) {
			switch(msg.what){
			default:if(queue.size() > 0)
				queue.getLast().processMessage(msg);
		//	Log.e("Activity_Receive", "Activity_Receive.handler received message");
			break;
			}
		}
	};
	
	
	
	
	protected void onCreate(Bundle myBundle){
		super.onCreate(myBundle);
		this.getActionBar().setDisplayHomeAsUpEnabled(true);
		this.getActionBar().setDisplayShowHomeEnabled(true);
		this.getActionBar().setIcon(R.drawable.ic_launcher);
		if(Activity_Send.queue.size()>0){
			new AlertDialog.Builder(this)
			.setTitle(this.getResources().getString(R.string.dialog_main_receive_warn_title))
			.setMessage(this.getResources().getString(R.string.dialog_main_receive_warn_message))
			.setIcon(R.drawable.icon_alertdialog_warn)
			.setCancelable(false)
			.setPositiveButton(this.getResources().getString(R.string.button_turnto), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Intent i = new Intent();
					i.setClass(Activity_Receive.this, Activity_Send.class);
					Activity_Receive.this.startActivity(i);
					Activity_Receive.this.finish();
				}
			})
			.setNegativeButton(this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Activity_Receive.this.finish();
				}
			})
			.show();
		}
		else{
			if(!queue.contains(this)){
				queue.add(this);
			}
			
			wmanage=new WifiManagement(this);
			wmanage.setSSIDIndex(SSID_INDEX);
		//	BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
		//	this.deviceName = myDevice.getName();
			
			if(!wmanage.isWifiEnabled()){
				ifneedCloseWifi=true;
			/*	AlertDialog ad_openwifi = new AlertDialog.Builder(this).setTitle(Activity_Receive.this.getResources().getString(R.string.dialog_openwifi_title))
						.setMessage(Activity_Receive.this.getResources().getString(R.string.dialog_openwifi_message))
						.setCancelable(false)
						.setPositiveButton(Activity_Receive.this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								
								
							}
						})
						.setNegativeButton(Activity_Receive.this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								Activity_Receive.this.finish();
							}
						})
						.create();  */
			//	ad_openwifi.show();
				setView();
				//setViewofSearching();
				Activity_Receive.this.connectToOpenWifi();
						
			}
			else{	
				if(wmanage.isWifiConnected()){
				/*	new AlertDialog.Builder(this)
					.setTitle(this.getResources().getString(R.string.dialog_attention_connecttosamenet_title))
					.setMessage(this.getResources().getString(R.string.dialog_attention_connecttosamenet_message))
					.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
						//	setView();
						//	setViewofSearching();
						//	Activity_Receive.this.connectToOpenWifi();
							
						}
					})				
					.setCancelable(false)
					.create();   */
					
					
					setView();						
					Activity_Receive.sendEmptyMessage(WIFI_CONNECTED);
				}
				else{
					setView();
				//	setViewofSearching();
					Activity_Receive.this.connectToOpenWifi();
				}
							
			}   
			
		}
		
	}
	
	
	protected void onResume(){
		super.onResume();
		this.isatFront=true;
	}
	
	protected void onPause(){
		super.onPause();
		this.isatFront=false;
	}
	
	protected void setView(){
		setContentView(R.layout.layout_receive);
		pg=(ProgressBar)findViewById(R.id.progressBar_receive);
		img_done=(ImageView)findViewById(R.id.imageview_receive);
		attention=(TextView)findViewById(R.id.receive_att);
		ssidinfo = (TextView)findViewById(R.id.ssidinfo);
		bottomarea=(RelativeLayout)findViewById(R.id.bottomarea);
	}
	
	private void setViewofSearching(){
		pg.setVisibility(View.VISIBLE);
		img_done.setVisibility(View.INVISIBLE);
		bottomarea.setVisibility(View.INVISIBLE);
		attention.setText(Activity_Receive.this.getResources().getString(R.string.textview_searchingwifi));
	}
	
	private void setViewofConnected(boolean isAPMode){		
		pg.setVisibility(View.INVISIBLE);
		bottomarea.setVisibility(View.VISIBLE);
		if(isAPMode){
			img_done.setImageResource(R.drawable.icon_apreceive);
			attention.setText(this.getResources().getString(R.string.textview_ap_wificonnected));
			ssidinfo.setText(this.getResources().getString(R.string.text_ssid_info)+wmanage.getSSID()+"\n"+this.getResources().getString(R.string.text_receive_apssid_att));
		}
		else{
			img_done.setImageResource(R.drawable.icon_wifimode);
			attention.setText(Activity_Receive.this.getResources().getString(R.string.textview_wificonnected));
			ssidinfo.setText(this.getResources().getString(R.string.text_ssid_info)+wmanage.getSSID()+"\n"+this.getResources().getString(R.string.text_receive_normalssid_att));
		}
		img_done.setVisibility(View.VISIBLE);
		
	}
	
	private void setViewofNull(){
		pg.setVisibility(View.GONE);
		img_done.setVisibility(View.GONE);
		attention.setVisibility(View.GONE);
		ssidinfo.setVisibility(View.GONE);
	}
	
	private void connectToOpenWifi(){
	//	wmanage.setWifiEnabled(true);
		setViewofSearching();
		wmanage.connecttoOpenWifi();	
		wait10s=new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try{
					Thread.sleep(10*1000);
					
					if(!wmanage.isWifiConnected()){
						Activity_Receive.sendEmptyMessage(NO_WIFI_AVALIABLE);
						Log.i("Activity_Receive", "��ǰδ����wifi��ȡ�õ�SSIDֵ��  "+wmanage.getSSID());
					}
					else{
						if(wmanage.isWifiConnected()){
							Activity_Receive.sendEmptyMessage(WIFI_CONNECTED);
						}
						
					}
					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			
		});
		
		wait10s.start();
		
	}
	
	private void startNet(){
		netThread=new NetReceiveThread(deviceName,this);
		netThread.connectSocket();
		netThread.noticeOnline();
		if(!Activity_Receive.isAPMode){
			attention.setText(this.getResources().getString(R.string.textview_wificonnected)+"\n"
					+this.getResources().getString(R.string.text_send_wifimode_ip)+IPUtils.getLocalIpAddress(this));
		}
		else{
			attention.setText(this.getResources().getString(R.string.textview_ap_wificonnected)+"\n"
					+this.getResources().getString(R.string.text_send_wifimode_ip)+IPUtils.getLocalIpAddress(this));
		}
		
		
		if(!wmanage.isWifiConnected()){
			this.ifcancallclose=false;
			closeAllOperationsAndExit();
			setViewofNull();
			new AlertDialog.Builder(this)
			.setTitle(this.getResources().getString(R.string.dialog_receive_wificlosed_title))
			.setMessage(this.getResources().getString(R.string.dialog_receive_wificlosed_message))
			.setCancelable(false)
			.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Activity_Receive.this.finish();
				}
			})
			.show();
		}
	}
	
	
	public  void processMessage(Message msg){
		switch(msg.what){
		case MESSAGE_OPEN_WIFI_FAILED:
			setViewofNull();
			if(wait10s!=null){
				wait10s.interrupt();
				wait10s=null;
			}
			new AlertDialog.Builder(this).setIcon(R.drawable.icon_alertdialog_warn)
			.setTitle(this.getResources().getString(R.string.dialog_openwifi_failed_title))
			.setMessage(this.getResources().getString(R.string.dialog_openwifi_failed_message))
			.setCancelable(false)
			.setPositiveButton(this.getResources().getString(R.string.button_retry), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub					
					Activity_Receive.this.connectToOpenWifi();
					setView();
					setViewofSearching();
				}
			})
			.setNegativeButton(this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Activity_Receive.this.finish();
				}
			})
			.show();
			break;
		case NO_WIFI_AVALIABLE:
			this.isConnected=false;
			wmanage.stopConnecttoOpenWifi();
			Log.e("Activity_Send", "connected value"+wmanage.isWifiConnected());
			AlertDialog ad_retry = new AlertDialog.Builder(Activity_Receive.this)
					.setIcon(R.drawable.icon_face_ops)
					.setTitle(Activity_Receive.this.getResources().getString(R.string.dialog_nowifiavaliable_title))
			.setMessage(Activity_Receive.this.getResources().getString(R.string.dialog_nowifiavaliable_message))
			.setCancelable(false)
			.setPositiveButton(Activity_Receive.this.getResources().getString(R.string.button_retry), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					if(wait10s!=null){
						wait10s.interrupt();
						wait10s=null;
					}
					Activity_Receive.this.connectToOpenWifi();
					setView();
					setViewofSearching();
				}
			})
			.setNegativeButton(Activity_Receive.this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Activity_Receive.this.finish();
				}
			})
			.create();
			setViewofNull();
			ad_retry.show();
			
			
			break;
			
		case WIFI_CONNECTED: 
			if(wait10s!=null){
				wait10s.interrupt();
				wait10s=null;
			}
			wmanage.stopConnecttoOpenWifi();
			this.isConnected=true;
			
			if(wmanage.getSSID().indexOf(SSID_INDEX)!=-1){
				isAPMode=true;
			}
			else{
				isAPMode=false;
			}
			setViewofConnected(isAPMode);
			Log.i("Activity_Receive", "��ǰ���ӵ�wifi��SSID��  "+wmanage.getSSID());
			startNet();
			break;
			
			
		case IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT:{  
			//�յ������ļ�����  
			if(!this.isinReceivingProcess){
				this.isinReceivingProcess=true;
				final String[] extraMsg = (String[]) msg.obj;	//�õ������ļ���Ϣ,�ַ������飬�ֱ����  IP�������ļ���Ϣ,���������ƣ���ID
				Log.d("receive file....", "receive file from :" + extraMsg[2] + "(" + extraMsg[0] +")");
				Log.d("receive file....", "receive file info:" + extraMsg[1]);
				byte[] bt = {0x07};		//���ڷָ���������ļ����ַ�
				String splitStr = new String(bt);
				final String[] fileInfos = extraMsg[1].split(splitStr);	//ʹ�÷ָ��ַ����зָ�
				this.senderip=extraMsg[0];
				Log.d("Activity_Send", "�յ��ļ���������,����" + fileInfos.length + "���ļ�");
				notificationmanagement=new NotificationManagement(this);
				notificationmanagement.notification.setSmallIcon(R.drawable.icon_download);
				notificationmanagement.setDefaults(NotificationManagement.DEFAULT_ALL);
				notificationmanagement.notification.setContentTitle(this.getResources().getString(R.string.notification_receive_receiverequest));
				notificationmanagement.notification.setContentText("������IP��"+extraMsg[0]+"\n"+"���������ƣ�"+extraMsg[2]);
				notificationmanagement.notification.setAutoCancel(true);								
				notificationmanagement.notification.setOngoing(false);
				notificationmanagement.setTargetActivity(Activity_Receive.class);
						
				if(Build.VERSION.SDK_INT>=21){
					notificationmanagement.setFullSreenIntent(Activity_Receive.class);
				}
				if(!this.isatFront){
					notificationmanagement.notify(2);
					Toast.makeText(this, Activity_Receive.this.getResources().getString(R.string.notification_receive_receiverequest)+"  ����" + fileInfos.length + "���ļ�", Toast.LENGTH_SHORT).show();	
				}
								
			//	String infoStr = "������IP:\t" + extraMsg[0] + "\n" + 
			//					 "����������:\t" + extraMsg[2] + "\n" +
			//					 "�ļ�����:\t" + fileInfos.length +"��";
				
				String fileinfostotal = "";
				
				for(int f=0;f<fileInfos.length;f++){
					String fileval[]=fileInfos[f].split(":");
					String filename=fileval[1];
					long filelength = Long.parseLong(fileval[2],16);
					fileinfostotal+=filename+"  "+Formatter.formatFileSize(Activity_Receive.this, filelength)+"\n\n";
					
				}
				
				if(this.ad_receivefile!=null){
					this.ad_receivefile.cancel();
					this.ad_receivefile=null;
				}
				 ad_receivefile = new AlertDialog.Builder(Activity_Receive.this).setIcon(R.drawable.icon_files_small)
						.setTitle(this.getResources().getString(R.string.dialog_receivefilerequest_title))
						.setMessage(this.getResources().getString(R.string.dialog_receivefilerequest_message1)+
								this.getResources().getString(R.string.dialog_receivefilerequest_message2)+extraMsg[0]+"\n"
								+this.getResources().getString(R.string.dialog_receivefilerequest_message3)+extraMsg[2]+"\n"
								+this.getResources().getString(R.string.dialog_receivefilerequest_message4)+fileInfos.length+"\n"
								+"�ļ���Ϣ���£�\n\n"+fileinfostotal)
						.setPositiveButton(this.getResources().getString(R.string.button_receive),new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								filevalues=new ArrayList<String[]>();
								long totalBytes = 0;
								for(int i =0;i<fileInfos.length;i++){
									String[] val = fileInfos[i].split(":");
									filevalues.add(val);
									totalBytes+=Long.parseLong(val[2],16);
								}	
								Activity_Receive.this.totalBytes = totalBytes;
								if(totalBytes+10*1024*1024>StorageUtil.getSDAvaliableSize()){   //�洢����
									//���;ܾ�����
									Activity_Receive.this.isinReceivingProcess=false;
									Activity_Receive.this.senupUDP(IpMessageConst.IPMSG_RELEASEFILES, extraMsg[0]);
									
									//��ʾ����
									new AlertDialog.Builder(Activity_Receive.this)
									.setTitle(Activity_Receive.this.getResources().getString(R.string.dialog_storage_notenough_title))
									.setMessage(Activity_Receive.this.getResources().getString(R.string.dialog_storage_notenough_message))
									.setPositiveButton(Activity_Receive.this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int which) {
											// TODO Auto-generated method stub
										
										}
									})
									.show();
								}
								else{
									Activity_Receive.isReceivingFileSuccess=true;
									NetReceiveThread.sendEmptyMessage(NetReceiveThread.MESSAGE_START_RECEIVING_FILES);
									fileReceiveThread = new Thread(new NetTcpFileReceiveThread(extraMsg[3], extraMsg[0],fileInfos));	//�½�һ�������ļ��߳�
									fileReceiveThread.start();	//�����߳�	
									Activity_Receive.this.isinReceivingProcess=true;
									//totalKBytes=0;
																		
									receivingFileDialog = new FileTransferDialog(Activity_Receive.this);
									receivingFileDialog.setTitle(Activity_Receive.this.getResources().getString(R.string.dialog_receivingfiles_title));
									receivingFileDialog.setIcon(R.drawable.icon_files_small);
									receivingFileDialog.setMax(totalBytes);
									receivingFileDialog.setButton(AlertDialog.BUTTON_NEGATIVE, Activity_Receive.this.getResources().getString(R.string.button_stop), 
											new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int which) {
											// TODO Auto-generated method stub
											Activity_Receive.isReceivingFileSuccess=false;
											Activity_Receive.this.isinReceivingProcess=false;
											if(fileReceiveThread!=null){
												fileReceiveThread.interrupt();
												fileReceiveThread=null;
												Activity_Receive.this.senupUDP(IpMessageConst.IPMSG_INTERRUPT_FILETRANSFER, extraMsg[0]);
												try{
													if(NetTcpFileReceiveThread.socket!=null){
														NetTcpFileReceiveThread.socket.close();
														NetTcpFileReceiveThread.socket=null;
													}											
												}catch(Exception e){
													e.printStackTrace();
												}	
											}
											Activity_Receive.sendEmptyMessage(MESSAGE_RECEIVINGFILES_INTERRUPT_SELF);
											
										}
									});
									receivingFileDialog.setCancelable(false);
									
									fnotification=new FileTransferNotification(Activity_Receive.this);
									fnotification.notification.setSmallIcon(R.drawable.icon_download);
									fnotification.notification.setContentTitle(Activity_Receive.this.getResources().getString(R.string.notification_receive_receiving_title));
									fnotification.notification.setContentText("����ɣ�");
									fnotification.notification.setOngoing(true);
									Intent intent = new Intent();
									intent.setClass(Activity_Receive.this, Activity_Receive.class);
									PendingIntent pi = PendingIntent.getActivity(Activity_Receive.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
									fnotification.notification.setContentIntent(pi);
									
									
									Activity_Receive.this.senupUDP(IpMessageConst.IPMSG_ACCEPT_RECEIVINGFILES, extraMsg[0]);

								//	IpMessageProtocol ipmsgSend = new IpMessageProtocol();
								//	ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
								//	ipmsgSend.setSenderName("android");
								//	ipmsgSend.setSenderHost("none");
								//	ipmsgSend.setCommandNo(IpMessageConst.IPMSG_ACCEPT_RECEIVINGFILES);	//���ͱ�������
								//	ipmsgSend.setAdditionalSection(extraMsg[3] + "\0");	//������Ϣ������û����ͷ�����Ϣ
								//	InetAddress sendAddress = null;
								//	try {
								//		sendAddress = InetAddress.getByName(extraMsg[0]);
								//	} catch (UnknownHostException e) {
										// TODO Auto-generated catch block
								//		e.printStackTrace();
								//	}
								//	netThread.sendUdpData(ipmsgSend.getProtocolString(), sendAddress, IpMessageConst.PORT);	//��������
									Activity_Receive.sendEmptyMessage(Activity_Receive.MESSAGE_START_RECEIVING_FILES);
									Toast.makeText(Activity_Receive.this, "��ʼ�����ļ�", Toast.LENGTH_SHORT).show();
								}
								
								
								
							}
						}).setNegativeButton(this.getResources().getString(R.string.button_refuse), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								//���;ܾ�����
					 			//����ܾ�����
								Activity_Receive.this.isinReceivingProcess=false;
								Activity_Receive.this.senupUDP(IpMessageConst.IPMSG_RELEASEFILES, extraMsg[0]);
								
							//	IpMessageProtocol ipmsgSend = new IpMessageProtocol();
							//	ipmsgSend.setVersion("" +IpMessageConst.VERSION);	//�ܾ�������
							//	ipmsgSend.setCommandNo(IpMessageConst.IPMSG_RELEASEFILES);
							//	ipmsgSend.setSenderName("android�ɸ�");
							//	ipmsgSend.setSenderHost("android");
							//	ipmsgSend.setAdditionalSection(extraMsg[3] + "\0");	//������Ϣ����ȷ���յ��İ��ı��
					 			
							//	InetAddress sendAddress = null;
							//	try {
							//		sendAddress = InetAddress.getByName(extraMsg[0]);
							//	} catch (UnknownHostException e) {
									// TODO Auto-generated catch block
							//		e.printStackTrace();
							//	}
								
							//	netThread.sendUdpData(ipmsgSend.getProtocolString(), sendAddress, IpMessageConst.PORT);
							}
						}).create();
				 ad_receivefile.setCancelable(false);
				 
				 ad_receivefile.show(); 
				 
			}
			
			
		}break;
		case Activity_Receive.MESSAGE_START_RECEIVING_FILES:{
			if(this.ad_receivefile!=null){
				this.ad_receivefile.cancel();
			}
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.show();
			}
			if(this.fnotification!=null){
				fnotification.notification.setOngoing(true);
				fnotification.notification.setAutoCancel(false);
				fnotification.notify(2);
			}
		
		}
		break;
		case Activity_Receive.MESSAGE_REFRESH_FILEDIALOG_SPEED:{
			long value_speed[]=(long[])msg.obj;
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.setSpeed(value_speed[0]);
			}
			
		}
		break;
		case Activity_Receive.MESSAGE_REFRESH_FILEDIALOG_PROGRESS:{
			long value_progress[]=(long[])msg.obj;
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.setProgress(value_progress[0]);
			}			
			DecimalFormat dm=new DecimalFormat("#.00");	
			int percent =(int) (Double.valueOf(dm.format((double)value_progress[0]/this.totalBytes))*100);
			if(this.fnotification!=null){
				fnotification.notification.setContentText("�����"+percent+"%");
				fnotification.setProgress(percent, 2);
			}
			
		}
		break;
		case Activity_Receive.MESSAGE_RECEIVING_CURRENTFILE:{
			String value_filename[]=(String []) msg.obj;
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.setTitle(this.getResources().getString(R.string.dialog_receivingfiles_title)+value_filename[0]+"/"+value_filename[1]);
				this.receivingFileDialog.setFilename(this.getResources().getString(R.string.dialog_receivingfiles_title)+": "+value_filename[2]);
			}			
		}
		break;
		case Activity_Receive.MESSAGE_DISMISS_REQUESTDIALOG:
			String ip[] =(String[]) msg.obj;
			String requestip=ip[0];
			if(requestip.equals(this.senderip)){
				this.isinReceivingProcess=false;
				if(this.ad_receivefile!=null){
					ad_receivefile.cancel();
				}
				
				if(this.notificationmanagement!=null){
					this.notificationmanagement.manager.cancel(2);
				}
				
				Toast.makeText(this, "�Է�ȡ���˷����ļ�", Toast.LENGTH_SHORT).show();
			}
			break;
		case Activity_Receive.MESSAGE_RECEIVING_FILES_COMPLETE:
			this.isinReceivingProcess=false;
			String[] newpath=(String[])msg.obj;
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.cancel();
			}
			if(this.fnotification!=null){
				fnotification.notification.setContentTitle(this.getResources().getString(R.string.notification_receive_complete_title));
				fnotification.notification.setProgress(100, 100, false);
				fnotification.notification.setContentText("�����");
				fnotification.notification.setOngoing(false);
				fnotification.notification.setAutoCancel(true);
				fnotification.notify(2);
			}
			if(this.notificationmanagement!=null&&!this.isatFront){
				this.notificationmanagement.notification.setContentTitle(this.getResources().getString(R.string.notification_receive_complete_title));
				this.notificationmanagement.notification.setContentText("�ļ��������");
				this.notificationmanagement.notification.setOngoing(false);
				this.notificationmanagement.notification.setAutoCancel(true);
				this.notificationmanagement.notify(2);
				Toast.makeText(this, this.getResources().getString(R.string.notification_receive_complete_title), Toast.LENGTH_SHORT).show();
			}
			new AlertDialog.Builder(this).setTitle("�������").setIcon(R.drawable.icon_files_small)
			.setMessage("�ļ��ѽ����� �����洢/WiFiRev/"+newpath[0])
			.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					   
				}
			})
			.setCancelable(true)
			.show();
			
			break;
			
		case	MESSAGE_RECEIVINGFILES_INTERRUPT:
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.cancel();
			}
			
			if(fileReceiveThread!=null){
				fileReceiveThread.interrupt();
				fileReceiveThread=null;
			}
			if(NetTcpFileReceiveThread.socket!=null){
				try{
					NetTcpFileReceiveThread.socket.close();
				}catch(Exception e){
					e.printStackTrace();
				}
				NetTcpFileReceiveThread.socket=null;
			}
			this.isinReceivingProcess=false;
			if(this.fnotification!=null){
				fnotification.notification.setContentTitle(this.getResources().getString(R.string.notification_receive_stopreceiving_title));
				fnotification.notification.setContentText("�Է�ֹͣ�˷���");
				fnotification.notification.setOngoing(false);
				fnotification.notification.setAutoCancel(true);
				fnotification.notify(2);
			}
			if(this.notificationmanagement!=null&&!this.isatFront){
				this.notificationmanagement.notification.setContentTitle(this.getResources().getString(R.string.notification_receive_stopreceiving_title));
				this.notificationmanagement.notification.setContentText("�Է�ֹͣ�˷���");
				this.notificationmanagement.notification.setOngoing(false);
				this.notificationmanagement.notification.setAutoCancel(true);
				this.notificationmanagement.notify(2);				
			}	
			
			Toast.makeText(this, this.getResources().getString(R.string.notification_receive_stopreceiving_title)+" �Է���ֹ�˷����ļ�", Toast.LENGTH_LONG).show();
			
			break;
		case  NET_CONNECTIVITY_CHANGED:
			if(this.ifcancallclose&&this.isConnected&&!wmanage.isWifiConnected()&&queue.size()>0){
				this.ifcancallclose=false;
				closeAllOperationsAndExit();
				setViewofNull();
				
				this.notificationmanagement=new NotificationManagement(this);
				this.notificationmanagement.notification.setSmallIcon(R.drawable.icon_alertdialog_warn);
				this.notificationmanagement.notification.setContentTitle(this.getResources().getString(R.string.notification_connectivity_broken));
				this.notificationmanagement.notification.setContentText("�������ӶϿ����շ���ֹ");	
				this.notificationmanagement.setTargetActivity(Activity_Receive.class);
				this.notificationmanagement.notification.setAutoCancel(true);
				this.notificationmanagement.notification.setOngoing(false);
				this.notificationmanagement.notify(2);
				
				new AlertDialog.Builder(this)
				.setTitle(this.getResources().getString(R.string.dialog_receive_wificlosed_title))
				.setMessage(this.getResources().getString(R.string.dialog_receive_wificlosed_message))
				.setCancelable(false)
				.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						Activity_Receive.this.finish();
					}
				})
				.show();
			}
			
		
		break;
		case MESSAGE_RECEIVINGFILES_INTERRUPT_SELF:{
			if(Activity_Receive.this.fnotification!=null){
				Activity_Receive.this.fnotification.notification.setOngoing(false);
				Activity_Receive.this.fnotification.notification.setContentTitle(this.getResources().getString(R.string.notification_receive_stopreceiving_title));
				Activity_Receive.this.fnotification.notification.setContentText("��ֹͣ����");
				Activity_Receive.this.fnotification.notification.setAutoCancel(true);
				Activity_Receive.this.fnotification.notify(2);
			}
		}
		break;
		default:break;
		}
	}
	
	public static void sendEmptyMessage(int what) {
		handler.sendEmptyMessage(what);
	}
	
	public static void sendMessage(Message msg) {
		handler.sendMessage(msg);
	}
	
	private void closeAllOperationsAndExit(){
		
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.cancel();
			}
			if(wait10s!=null){
				wait10s.interrupt();
				wait10s=null;
			}
			
			if(this.wmanage!=null){
				this.wmanage.stopConnecttoOpenWifi();
			}
			
			if(fileReceiveThread!=null){
				fileReceiveThread.interrupt();
				fileReceiveThread=null;
			}
			if(NetTcpFileReceiveThread.socket!=null){
				try{
					NetTcpFileReceiveThread.socket.close();
				}catch(Exception e){
					e.printStackTrace();
				}
				NetTcpFileReceiveThread.socket=null;
			}
			if(netThread!=null){
				netThread.disconnectSocket();
			}
		
		
	}
	
	public void onWifiStateChanged(int state){
		if(state==NET_CONNECTIVITY_CHANGED&&queue.size()>0){
			Activity_Receive.sendEmptyMessage(NET_CONNECTIVITY_CHANGED);			
		}
	}
	
	public void onAPStateChanged(int state){
		
	}
	
	public void finish(){
		super.finish();
		if(queue.contains(this)){
			queue.remove(this);
		}
		if(wait10s!=null){
			wait10s.interrupt();
			wait10s=null;
		}
		if(wmanage!=null){
			wmanage.stopConnecttoOpenWifi();
		}
		
		if(ifneedCloseWifi){
		//	wmanage.setWifiEnabled(false);
		}
		if(this.isConnected){	
			netThread.noticeOffline();
		//	netThread.disconnectSocket();			
			Log.e("Activity_Receive", "�ѳ��Է����������ݰ��͹رն˿�");
		}
	
	}
	
	private void senupUDP(int command,String IP){
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName("android");
		ipmsgSend.setSenderHost("none");
		ipmsgSend.setCommandNo(command);	//���ͱ�������
		ipmsgSend.setAdditionalSection("");	//������Ϣ������û����ͷ�����Ϣ
		InetAddress sendAddress = null;
		try {
			sendAddress = InetAddress.getByName(IP);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		netThread.sendUdpData(ipmsgSend.getProtocolString(), sendAddress, IpMessageConst.PORT);	//��������
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			this.finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	

}
