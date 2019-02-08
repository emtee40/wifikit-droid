package app.mxr.wlanshare.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.IllegalBlockingModeException;
import java.util.HashMap;
import java.util.LinkedList;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import app.mxr.wlanshare.activities.Activity_Receive;
import app.mxr.wlanshare.data.Devices;
import app.mxr.wlanshare.utils.IPUtils;


public class NetReceiveThread  implements Runnable {
	
	private static final int Message_Send_Up_Data_Complete=1;
	public static final int MESSAGE_START_RECEIVING_FILES = 0x0002;
							
	
	public static final String TAG = "NetThread";
	private static final int BUFFERLENGTH =65500; //�����С
	private String sendername,sendergroup;
	private byte[] sendBuffer = null;
	private DatagramPacket udpSendPacket = null,udpResPacket = null;	//���ڷ��͵�udp���ݰ�
	public static  DatagramSocket udpSocket = null;	//���ڽ��պͷ���udp���ݵ�socket
	private boolean onWork=false;
	private Thread udpThread = null,udp2=null;
	private byte[] resBuffer = new byte[BUFFERLENGTH];	//�������ݵĻ���
	private HashMap<String,Devices> devices;	//��ǰ�����û��ļ��ϣ���IPΪKEY
	//protected List<Devices> devices;
	private Context context;
	private boolean canCloseUdpSocket=false;	
	private static LinkedList<NetReceiveThread> queue = new LinkedList<NetReceiveThread>();
	private static Handler handler=new Handler(){
		public void handleMessage(Message msg) {
			switch(msg.what){
			default:if(queue.size()>0){				
						queue.getLast().processMessage(msg);
					} break;
			}
			
		}
	};
	
	public NetReceiveThread(String sendername,Context context){
		if(!queue.contains(this)){
			queue.add(this);
		}
		this.sendername=sendername;
		this.sendergroup="";
		devices = new HashMap<String,Devices>();
		this.context=context;
	//	this.resBuffer=new byte[Bufferlength];
	//	devices = new ArrayList<Devices>();
	//	this.isAPMode=isAPMode;
	}
	
	
	public boolean connectSocket(){	//�����˿ڣ�����UDP����
		boolean result = false;
		
		try {
			if(udpSocket == null){
				udpSocket = new DatagramSocket(IpMessageConst.PORT);	//�󶨶˿�
				Log.e("", "connectSocket()....��UDP�˿�" + IpMessageConst.PORT + "�ɹ�");
			}
			if(udpResPacket == null)
				udpResPacket = new DatagramPacket(resBuffer, BUFFERLENGTH);
			onWork = true;  //���ñ�ʶΪ�̹߳���
			startThread();	//�����߳̽���udp����
			result = true;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			disconnectSocket();
			Log.e(TAG, "connectSocket()....��UDP�˿�" + IpMessageConst.PORT + "ʧ��");
		}
		
		return result;
	}
	
	public void disconnectSocket(){	// ֹͣ����UDP����
		onWork = false;	// �����߳����б�ʶΪ������
		if(udpResPacket != null){
			udpResPacket = null;
		}
		
		if(udpSocket != null){
			udpSocket.close();
			udpSocket = null;
			Log.e(TAG, "UDP�շ��˿��ѹر�");
		}
		stopThread();
		if(udp2!=null){
			udp2.interrupt();
			udp2=null;
		}
		
		
	}
	
	
	
	public void noticeOnline(){	// �������߹㲥
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName(sendername);
		ipmsgSend.setSenderHost(sendergroup);
		ipmsgSend.setCommandNo(IpMessageConst.IPMSG_ANSENTRY);	//��������
		ipmsgSend.setAdditionalSection(sendername + "\0" );	//������Ϣ������û����ͷ�����Ϣ
		
		InetAddress broadcastAddr;
		try {
			if(Activity_Receive.isAPMode){
				broadcastAddr = InetAddress.getByName(IPUtils.getWifiRouteIPAddress(context));	//�㲥��ַ	--�ȵ�ģʽ
			}
			else{
				broadcastAddr = InetAddress.getByName("255.255.255.255");	//�㲥��ַ	-- ��ͨ·��ģʽ
			}
			
			sendUdpData(ipmsgSend.getProtocolString()+"\0", broadcastAddr, IpMessageConst.PORT);	//��������
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "noticeOnline()....�㲥��ַ����");
		}
		
	}
	
	public void noticeOffline(){	//�������߹㲥
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName(sendername);
		ipmsgSend.setSenderHost(sendergroup);
		ipmsgSend.setCommandNo(IpMessageConst.IPMSG_DEVICE_OFFLINE);	//��������
		ipmsgSend.setAdditionalSection(sendername + "\0" + sendergroup);	//������Ϣ������û����ͷ�����Ϣ
		
		InetAddress broadcastAddr;
		try {			
			if(Activity_Receive.isAPMode){
				broadcastAddr = InetAddress.getByName(IPUtils.getWifiRouteIPAddress(context));	//�㲥��ַ	--�ȵ�ģʽ
			}
			else{
				broadcastAddr = InetAddress.getByName("255.255.255.255");	//�㲥��ַ	-- ��ͨ·��ģʽ
			}		
			canCloseUdpSocket=true;
			sendUdpData(ipmsgSend.getProtocolString() + "\0", broadcastAddr, IpMessageConst.PORT);	//��������
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "noticeOnline()....�㲥��ַ����");
			
		}

	}
	
	private void startThread() {	//�����߳�
		// TODO Auto-generated method stub
		if(udpThread == null){
			udpThread = new Thread(this);
			udpThread.start();
			Log.e(TAG, "���ڼ���UDP����");
		}
	}
	
	private void stopThread() {	//ֹͣ�߳�
		// TODO Auto-generated method stub
		
		if(udpThread != null){			
			udpThread.interrupt();	//���̶߳��������ж�
		}
		Log.i("", "ֹͣ����UDP����");
	}
	
	public synchronized void sendUdpData(final String sendStr, final InetAddress sendto, final int sendPort){	//����UDP���ݰ��ķ���
		if(udp2!=null){
			udp2.interrupt();
			udp2=null;
		}
		udp2=new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					sendBuffer = sendStr.getBytes();
					// ���췢�͵�UDP���ݰ�
					udpSendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, sendto, sendPort);
					if(udpSocket!=null){
						udpSocket.send(udpSendPacket);	//����udp���ݰ�	
						Log.e(TAG, "�ɹ���IPΪ" + sendto.getHostAddress() + "����UDP���ݣ�" + sendStr);
					}
					udpSendPacket = null;		
					handler.sendEmptyMessage(Message_Send_Up_Data_Complete);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.e(TAG, "sendUdpData(String sendStr, int port)....ϵͳ��֧�ֱ���");
				} catch (IOException e) {	//����UDP���ݰ�����
					// TODO Auto-generated catch block
					e.printStackTrace();
					udpSendPacket = null;
					//udpSendPacket_Host=null;
					Log.e(TAG, "sendUdpData(String sendStr, int port)....����UDP���ݰ�ʧ��");
				}
				catch(SecurityException s){
					
				}
				catch(IllegalBlockingModeException ib){
					
				}
				catch(IllegalArgumentException ia){
					
				}
			}
			
		});
		udp2.start();
		
		
	}
	
	

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(onWork){
			
			try {
				udpSocket.receive(udpResPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				onWork = false;
				
				if(udpResPacket != null){
					udpResPacket = null;
				}
				
				if(udpSocket != null){
					udpSocket.close();
					udpSocket = null;
				}
				
				udpThread = null;
				Log.e("", "UDP���ݰ�����ʧ�ܣ��߳�ֹͣ");
				break;
			} 
			
			if(udpResPacket.getLength() == 0){
				Log.i(TAG, "�޷�����UDP���ݻ��߽��յ���UDP����Ϊ��");
				continue;
			}
			String ipmsgStr = "";
			try {
				ipmsgStr = new String(resBuffer, 0, udpResPacket.getLength());
			} catch (Exception e){//UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "��������ʱ��ϵͳ��֧�ֱ���");
			}//��ȡ�յ�������
			Log.i(TAG, "���յ���UDP��������Ϊ:" + ipmsgStr);
			IpMessageProtocol ipmsgPro = new IpMessageProtocol(ipmsgStr);	//
			int commandNo = ipmsgPro.getCommandNo();
			int commandNo2 = 0x000000FF & commandNo;	//��ȡ������
			switch(commandNo2){
				case  IpMessageConst.IPMSG_REQUEST_ONLINE_DEVICES:{
					//���湹����ͱ�������
					IpMessageProtocol ipmsgSend = new IpMessageProtocol();
					ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
					ipmsgSend.setSenderName(sendername);
					ipmsgSend.setSenderHost(sendergroup);
					ipmsgSend.setCommandNo(IpMessageConst.IPMSG_ANSENTRY);	//���ͱ�������
					ipmsgSend.setAdditionalSection(sendername + "\0" );	//������Ϣ������û����ͷ�����Ϣ
					
					sendUdpData(ipmsgSend.getProtocolString(), udpResPacket.getAddress(), udpResPacket.getPort());	//��������
				}
				break;
				case IpMessageConst.IPMSG_ANSENTRY_SENDER:	{	//�յ����Ͷ˻���
					//	addUser(ipmsgPro);
					//	Activity_Send.sendEmptyMessage(IpMessageConst.IPMSG_ANSENTRY);
				}	
				break;
				
				//case IpMessageConst.IPMSG_RELEASEFILES:{ //�ܾ������ļ�
				//	MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_RELEASEFILES);
				//	}
				//		break;
				case IpMessageConst.IPMSG_SENDMSG:{ //�յ���Ϣ������
					String senderIp = udpResPacket.getAddress().getHostAddress();	//�õ�������IP
					String senderName = ipmsgPro.getSenderName();	//�õ������ߵ�����
					String additionStr = ipmsgPro.getAdditionalSection();	//�õ�������Ϣ
					//String msgTemp;		//ֱ���յ�����Ϣ�����ݼ���ѡ���ж��Ƿ��Ǽ�����Ϣ
					String[] splitStr = additionStr.split("\0"); //ʹ��"\0"�ָ���и����ļ���Ϣ�����ָ����
				//	msgTemp = splitStr[0]; //����Ϣ����ȡ��
					if((commandNo & IpMessageConst.IPMSG_FILEATTACHOPT) == IpMessageConst.IPMSG_FILEATTACHOPT){	
						//������з����ļ���ش���
					
						Message msg = new Message();
						msg.what = (IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT);
						//�ַ������飬�ֱ����  IP�������ļ���Ϣ,���������ƣ���ID
						String[] extraMsg = {senderIp, splitStr[1],senderName,ipmsgPro.getPacketNo()};	
						msg.obj = extraMsg;	//�����ļ���Ϣ����
						Activity_Receive.sendMessage(msg);
					
						break;
					}
				
				
				}
				break;
				
				case IpMessageConst.IPMSG_SENDPORT_INTERRUPT:
					Log.i(TAG, "���Ͷ�ֹͣ�˷�������");
					Message msg_dis = new Message();
					msg_dis.what=Activity_Receive.MESSAGE_DISMISS_REQUESTDIALOG;
					String ip[]=new String [1];
					ip[0]=this.udpResPacket.getAddress().getHostAddress().toString();
					msg_dis.obj=ip;
					Activity_Receive.sendMessage(msg_dis);
					break;
				case IpMessageConst.IPMSG_INTERRUPT_FILETRANSFER:{
					Activity_Receive.isReceivingFileSuccess=false;
					Activity_Receive.sendEmptyMessage(Activity_Receive.MESSAGE_RECEIVINGFILES_INTERRUPT);
				}
				break;
				default:break;	
			}	//end of switch
			
			if(udpResPacket != null){	//ÿ�ν�����UDP���ݺ����ó��ȡ�������ܻᵼ���´��յ����ݰ����ضϡ�
				udpResPacket.setLength(BUFFERLENGTH);
			}
			
		}
		
		if(udpResPacket != null){
			udpResPacket = null;
		}
		
		if(udpSocket != null){
			udpSocket.close();
			udpSocket = null;
		}
		
		udpThread = null;
	}
	
	private  void processMessage(Message msg){
		switch(msg.what){
		default:break;
		case Message_Send_Up_Data_Complete:  
			if(canCloseUdpSocket){
				disconnectSocket();
			}
			break;
			
		case MESSAGE_START_RECEIVING_FILES:
			IpMessageProtocol ipmsgSend = new IpMessageProtocol();
			ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
			ipmsgSend.setSenderName(sendername);
			ipmsgSend.setSenderHost(sendergroup);
			ipmsgSend.setCommandNo(IpMessageConst.IPMSG_ACCEPT_RECEIVINGFILES);	//���ͱ�������
			ipmsgSend.setAdditionalSection(sendername + "\0" );	//������Ϣ������û����ͷ�����Ϣ
			
		//	sendUdpData(ipmsgSend.getProtocolString(), udpResPacket.getAddress(), udpResPacket.getPort());	//��������
			break;
			
		
		}
	}
	
	public HashMap<String,Devices> getDevices(){
		return devices;
	}
	
	public static void sendEmptyMessage(int what){
		handler.sendEmptyMessage(what);
	}

	
	
}