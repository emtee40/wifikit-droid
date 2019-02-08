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

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import app.mxr.wlanshare.activities.Activity_Send;
import app.mxr.wlanshare.data.Devices;
import app.mxr.wlanshare.utils.IPUtils;


public class NetSendThread  implements Runnable {
	
	public static final String TAG = "NetThread";
	private static final int BUFFERLENGTH = 1024; //�����С
	private String sendername,sendergroup;
	private byte[] sendBuffer = null;
	private DatagramPacket udpSendPacket = null,udpResPacket = null;	//���ڷ��͵�udp���ݰ�
	public static DatagramSocket udpSocket = null;	//���ڽ��պͷ���udp���ݵ�socket
	private boolean onWork=false;
	private Thread udpThread = null,udp2=null;
	private byte[] resBuffer = new byte[BUFFERLENGTH];	//�������ݵĻ���
	private HashMap<String,Devices> devices;	//��ǰ�����û��ļ��ϣ���IPΪKEY
	//protected List<Devices> devices;
	private static LinkedList<NetSendThread> queue = new LinkedList<NetSendThread>();
	private boolean canCloseUdpSocket=false;
	private static final int Message_Send_Up_Data_Complete=1;
//	public static final int MESSAGE_SEND_REFUSE_INFO=0x2;
	private static Handler handler=new Handler(){
		public void handleMessage(Message msg) {
			switch(msg.what){
			default:if(queue.size()>0){				
						queue.getLast().processMessage(msg);
					} break;
			}
			
		}
	};
	
	public NetSendThread(String sendername){
		this.sendername=sendername;
		this.sendergroup="";
		devices = new HashMap<String,Devices>();
		if(!queue.contains(this)){
			queue.add(this);
		}
	//	devices = new ArrayList<Devices>();
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
	
	
	
	public void noticeOnline(){	// �������߹㲥���������������ӵ��豸
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName(sendername);
		ipmsgSend.setSenderHost(sendergroup);
		ipmsgSend.setCommandNo(IpMessageConst.IPMSG_REQUEST_ONLINE_DEVICES);	//��������
		ipmsgSend.setAdditionalSection(sendername + "\0" );	//������Ϣ������û����ͷ�����Ϣ
		
		InetAddress broadcastAddr;
		try {
			if(!Activity_Send.isAPEnabled){
				broadcastAddr = InetAddress.getByName("255.255.255.255");	//�㲥��ַ	
				sendUdpData(ipmsgSend.getProtocolString()+"\0", broadcastAddr, IpMessageConst.PORT);	//��������
			}
			else{								
				String initalIP = IPUtils.getanIPfromARP();
				Log.i("ͨ����ȡ�ļ���õ�IP", initalIP);
				if(initalIP.length()>=8){
					String broadcastadress = initalIP.substring(0, initalIP.lastIndexOf("."))+".255";
					broadcastAddr = InetAddress.getByName(broadcastadress);	//�㲥��ַ	
					sendUdpData(ipmsgSend.getProtocolString()+"\0", broadcastAddr, IpMessageConst.PORT);	//��������
				}
				
															
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "noticeOnline()....�㲥��ַ����");
		}
		
	}
	
	public void noticeOffline(){	//�������߹㲥������������ڷ��Ͷ��ò���
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName(sendername);
		ipmsgSend.setSenderHost(sendergroup);
		ipmsgSend.setCommandNo(IpMessageConst.IPMSG_DEVICE_OFFLINE);	//��������
		ipmsgSend.setAdditionalSection(sendername + "\0" + sendergroup);	//������Ϣ������û����ͷ�����Ϣ
		
		InetAddress broadcastAddr;
		try {			
			broadcastAddr = InetAddress.getByName("255.255.255.255");	//�㲥��ַ			
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
			udpThread=null;
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
					}
					Log.e(TAG, "�ɹ���IPΪ" + sendto.getHostAddress() + "����UDP���ݣ�" + sendStr);
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
	
	private synchronized void addUser(IpMessageProtocol ipmsgPro){ //����û���Users��Map��
		String userIp = udpResPacket.getAddress().getHostAddress();
		Devices device = new Devices();
//		user.setUserName(ipmsgPro.getSenderName());
		device.setAlias(ipmsgPro.getSenderName());	//�����ݶ�����������
		device.setUserName(ipmsgPro.getSenderName());
		String extraInfo = ipmsgPro.getAdditionalSection();
		String[] userInfo = extraInfo.split("\0");	//�Ը�����Ϣ���зָ�,�õ��û����ͷ�����
		if(userInfo.length < 1){
			device.setUserName(ipmsgPro.getSenderName());
			
		}else if (userInfo.length == 1){
			device.setUserName(userInfo[0]);
			
		}else{
			device.setUserName(userInfo[0]);
			
		}
			
		device.setIp(userIp);
		device.setHostName(ipmsgPro.getSenderHost());
		device.setMac("");	//��ʱû������ֶ�
		devices.put(userIp, device);
		//devices.add(device);
		Log.i(TAG, "�ɹ����ipΪ" + userIp + "���û�");
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
				Log.e(TAG, "��������ʱ��ϵͳ��֧��GBK����");
			}//��ȡ�յ�������
			Log.i(TAG, "���յ���UDP��������Ϊ:" + ipmsgStr);
			IpMessageProtocol ipmsgPro = new IpMessageProtocol(ipmsgStr);	//
			int commandNo = ipmsgPro.getCommandNo();
			int commandNo2 = 0x000000FF & commandNo;	//��ȡ������
			switch(commandNo2){
			case IpMessageConst.IPMSG_ANSENTRY:	{	//�յ��������ݰ�������û���������IPMSG_ANSENTRYӦ��
				addUser(ipmsgPro);	//����û�
				
				Activity_Send.sendEmptyMessage(IpMessageConst.IPMSG_ANSENTRY);    
				
				//���湹����ͱ�������
				IpMessageProtocol ipmsgSend = new IpMessageProtocol();
				ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
				ipmsgSend.setSenderName(sendername);
				ipmsgSend.setSenderHost(sendergroup);
				ipmsgSend.setCommandNo(IpMessageConst.IPMSG_ANSENTRY_SENDER);	//���ͱ�������
				ipmsgSend.setAdditionalSection(sendername + "\0" );	//������Ϣ������û����ͷ�����Ϣ
				
				sendUdpData(ipmsgSend.getProtocolString(), udpResPacket.getAddress(), udpResPacket.getPort());	//��������
			}	
				break;
			
		//	case IpMessageConst.IPMSG_ANSENTRY:	{	//�յ�����Ӧ�𣬸��������û��б�
		//		addUser(ipmsgPro);
		//	Activity_Send.sendEmptyMessage(IpMessageConst.IPMSG_ANSENTRY);
		//	}	
		//		break;
			
			case IpMessageConst.IPMSG_DEVICE_OFFLINE:{	//�յ����߹㲥��ɾ��users�ж�Ӧ��ֵ
				String userIp = udpResPacket.getAddress().getHostAddress();
				devices.remove(userIp);
				Activity_Send.sendEmptyMessage(IpMessageConst.IPMSG_DEVICE_OFFLINE);
				
				Log.i(TAG, "�������߱��ĳɹ�ɾ��ipΪ" + userIp + "���û�");
			}	
				break;
			
			case IpMessageConst.IPMSG_SENDMSG:{ //�յ���Ϣ������
				String senderIp = udpResPacket.getAddress().getHostAddress();	//�õ�������IP
				String senderName = ipmsgPro.getSenderName();	//�õ������ߵ�����
				String additionStr = ipmsgPro.getAdditionalSection();	//�õ�������Ϣ
			//	Date time = new Date();	//�յ���Ϣ��ʱ��
			//	String msgTemp;		//ֱ���յ�����Ϣ�����ݼ���ѡ���ж��Ƿ��Ǽ�����Ϣ
			//	String msgStr;		//���ܺ����Ϣ����
				
				//����������ĸ����ֶε��ж�
				
				//���������ִ�����֤ѡ���������յ���Ϣ����
				if( (commandNo & IpMessageConst.IPMSG_SENDCHECKOPT) == IpMessageConst.IPMSG_SENDCHECKOPT){
					//����ͨ���յ���Ϣ����
					IpMessageProtocol ipmsgSend = new IpMessageProtocol();
					ipmsgSend.setVersion("" +IpMessageConst.VERSION);	//ͨ���յ���Ϣ������
					ipmsgSend.setCommandNo(IpMessageConst.IPMSG_RECVMSG);
					ipmsgSend.setSenderName(sendername);
					ipmsgSend.setSenderHost(sendergroup);
					ipmsgSend.setAdditionalSection(ipmsgPro.getPacketNo() + "\0");	//������Ϣ����ȷ���յ��İ��ı��
					
					sendUdpData(ipmsgSend.getProtocolString(), udpResPacket.getAddress(), udpResPacket.getPort());	//��������
				}
				
				String[] splitStr = additionStr.split("\0"); //ʹ��"\0"�ָ���и����ļ���Ϣ�����ָ����
			//	msgTemp = splitStr[0]; //����Ϣ����ȡ��
				
				//�Ƿ��з����ļ���ѡ��.���У��򸽼���Ϣ���ȡ���������ļ���Ϣ
				if((commandNo & IpMessageConst.IPMSG_FILEATTACHOPT) == IpMessageConst.IPMSG_FILEATTACHOPT){	
					//������з����ļ���ش���
					
					Message msg = new Message();
					msg.what = (IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT);
					//�ַ������飬�ֱ����  IP�������ļ���Ϣ,���������ƣ���ID
					String[] extraMsg = {senderIp, splitStr[1],senderName,ipmsgPro.getPacketNo()};	
					msg.obj = extraMsg;	//�����ļ���Ϣ����
					Activity_Send.sendMessage(msg);
					
					break;
				}
																
				
			}
				break;
				
			case IpMessageConst.IPMSG_RELEASEFILES:{ //�ܾ������ļ�
			//	MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_RELEASEFILES);
				Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_REFUSED_SENDING_FILES);
			}
				break;
				
			case IpMessageConst.IPMSG_ACCEPT_RECEIVINGFILES:{
				Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_START_SENDING_FILES);
			}
			break;
			
			case IpMessageConst.IPMSG_INTERRUPT_FILETRANSFER:{
				Activity_Send.isSendFileSuccess=false;
				Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_SENDINGFILES_INTERRUPT);
			}
			break;
				
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
	
	public HashMap<String,Devices> getDevices(){
		return devices;
	}
//	public List<Devices>getDevices(){
//		return devices;
//	}
	public void refreshDevices(){	//ˢ�������û�
		devices.clear();	//��������û��б�
		noticeOnline(); //��������֪ͨ
	//	Activity_Send.sendEmptyMessage(IpMessageConst.IPMSG_ANSENTRY);
	}
	
	private  void processMessage(Message msg){
		switch(msg.what){
		default:break;
		case Message_Send_Up_Data_Complete:  
			if(canCloseUdpSocket){
				disconnectSocket();
			}
			break;
					
		}
	}
	
	public static void sendEmptyMessage(int what){
		handler.sendEmptyMessage(what);
	}
	
	public static void sendMessage(Message msg){
		handler.sendMessage(msg);
	}

}
