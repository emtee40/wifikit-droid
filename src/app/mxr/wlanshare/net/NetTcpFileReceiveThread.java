package app.mxr.wlanshare.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;

import android.os.Message;
import android.util.Log;
import app.mxr.wlanshare.activities.Activity_Receive;
import app.mxr.wlanshare.utils.StorageUtil;

/**
 * Tcp�����ļ��߳���
 */
public class NetTcpFileReceiveThread implements Runnable {
	private final static String TAG = "NetTcpFileReceiveThread";
	
	private String[] fileInfos;	//�ļ���Ϣ�ַ�����
	private String senderIp;
	private long packetNo;	//�����
	private String savePath;	//�ļ�����·��
	
	private String selfName;
	private String selfGroup;
	
	public static Socket socket;
	private BufferedInputStream bis;	
	private BufferedOutputStream bos;
	BufferedOutputStream fbos;
	private byte[] readBuffer = new byte[1024];
	private String newfoldername;
	
	public NetTcpFileReceiveThread(String packetNo,String senderIp, String[] fileInfos){
		this.packetNo = Long.valueOf(packetNo);
		this.fileInfos = fileInfos;
		this.senderIp = senderIp;
		Activity_Receive.isReceivingFileSuccess=true;
		Calendar calendar=Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		int month=calendar.get(Calendar.MONTH)+1;
		String newfoldername=format(calendar.get(Calendar.YEAR))+"-"+format(month)+"-"+format(calendar.get(Calendar.DAY_OF_MONTH))+"-"+format(calendar.get(Calendar.HOUR_OF_DAY))+"-"+format(calendar.get(Calendar.MINUTE))+"-"+format(calendar.get(Calendar.SECOND));
		selfName = "android";
		selfGroup = "android";
		savePath= StorageUtil.getSDPath()+"/WiFiRev/"+newfoldername+"/";
		this.newfoldername=newfoldername;
		
		File path_wifirev=new File(StorageUtil.getSDPath()+"/WiFiRev");
		if(path_wifirev.exists()&&!path_wifirev.isDirectory()){
			path_wifirev.delete();
		}
		if(!path_wifirev.exists()){
			path_wifirev.mkdir();
		}
		
		
		File datefolder = new File(StorageUtil.getSDPath()+"/WiFiRev/"+newfoldername);
		if(datefolder.exists()&&!datefolder.isDirectory()){
			datefolder.delete();
		}
		if(!datefolder.exists()){
			datefolder.mkdir();
		}
		
		
	}
	
	private String format(int x){
		String s = "" + x;
		if (s.length() == 1)
			s = "0" + s;
		return s;
	} 
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		long receiveBytes=0;			
		for(int i = 0; i < fileInfos.length; i++){	//ѭ������ÿ���ļ�
			//ע�⣬������ʱδ�����ļ�������ð�ŵ������Э��涨�����ļ�������ð�ţ�����˫ð���������������������ʱû��
			String[] fileInfo = fileInfos[i].split(":");	//ʹ��:�ָ��õ��ļ���Ϣ����
			//�ȷ���һ��ָ����ȡ�ļ��İ�
			IpMessageProtocol ipmsgPro = new IpMessageProtocol();
			ipmsgPro.setVersion(String.valueOf(IpMessageConst.VERSION));
			ipmsgPro.setCommandNo(IpMessageConst.IPMSG_GETFILEDATA);
			ipmsgPro.setSenderName(selfName);
			ipmsgPro.setSenderHost(selfGroup);
			String additionStr = Long.toHexString(packetNo) + ":" + i + ":" + "0:";
			ipmsgPro.setAdditionalSection(additionStr);
			
			
			try {
				socket = new Socket(senderIp, IpMessageConst.PORT);
				Log.d(TAG, "�������Ϸ��Ͷ�");
				bos = new BufferedOutputStream(socket.getOutputStream());
				
				//������ȡ�ļ�����
				//byte[] sendBytes = ipmsgPro.getProtocolString().getBytes("gbk");
				byte[] sendBytes = ipmsgPro.getProtocolString().getBytes();
				bos.write(sendBytes, 0, sendBytes.length);
				bos.flush();
				
				Log.d(TAG, "ͨ��TCP���ͽ���ָ���ļ�������������ǣ�" + ipmsgPro.getProtocolString());
				
				Message message_filename = new Message();
				String [] fileinfos= new String [3];
				int arra = i+1;
				fileinfos[0]=""+arra;
				fileinfos[1]=""+fileInfos.length;
				fileinfos[2]=fileInfo[1];
				message_filename.what=Activity_Receive.MESSAGE_RECEIVING_CURRENTFILE;				
				message_filename.obj=fileinfos;
				Activity_Receive.sendMessage(message_filename);
															
				
			/*	String rootPath="";
				//�ж��ļ���չ������ȡ�ļ����ͣ����ļ��д��
				if(ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("png")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("jpg")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("jpeg")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("gif")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("bmp")){   //image files
					rootPath="images/";
					//�жϽ����ļ����ļ����Ƿ���ڣ��������ڣ��򴴽�
					File fileDir = new File(savePath+"images");
					
					if(fileDir.exists()&&!fileDir.isDirectory()){
						fileDir.delete();
					}					
					if( !fileDir.exists()){	//��������						
						fileDir.mkdir();
					}
				}else
				if(ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("apk")){     //APK Files
					rootPath="APKs/";
					File fileDir = new File(savePath+"APKs");
					if(fileDir.exists()&&!fileDir.isDirectory()){
						fileDir.delete();
					}					
					if( !fileDir.exists()){	//��������						
						fileDir.mkdir();
					}
				}else
				if(ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("doc")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("docx")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("ppt")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("ppt")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("pptx")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("xls")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("xlsx")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("text")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("pdf")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("wps")){
					rootPath="documents/";
					File fileDir = new File(savePath+"documents");
					if(fileDir.exists()&&!fileDir.isDirectory()){
						fileDir.delete();
					}					
					if( !fileDir.exists()){	//��������						
						fileDir.mkdir();
					}
				}else
				if(ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("avi")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("mp4")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("wmv")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("3gp")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("asf")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("asx")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("rm")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("rmvb")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("mov")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("mpg")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("mpeg")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("mpe")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("m4v")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("mkv")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("flv")){
					rootPath="videos/";
					File fileDir = new File(savePath+"videos");
					if(fileDir.exists()&&!fileDir.isDirectory()){
						fileDir.delete();
					}					
					if( !fileDir.exists()){	//��������						
						fileDir.mkdir();
					}
				}else
				if(ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("mp3")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("wav")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("wma")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("flac")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("midi")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("ogg")
						||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("ra")||ExtName.getFileExtName(fileInfo[1]).toLowerCase(Locale.ENGLISH).equals("ape")){
					rootPath="musics/";
					File fileDir = new File(savePath+"musics");
					if(fileDir.exists()&&!fileDir.isDirectory()){
						fileDir.delete();
					}					
					if( !fileDir.exists()){	//��������						
						fileDir.mkdir();
					}
				}else{
					rootPath="others/";
					File fileDir = new File(savePath+"others");
					if(fileDir.exists()&&!fileDir.isDirectory()){
						fileDir.delete();
					}					
					if( !fileDir.exists()){	//��������						
						fileDir.mkdir();
					}
				}   */
															
								
				//�����ļ�
				//File receiveFile = new File(savePath+rootPath+fileInfo[1]);
				File receiveFile = new File(savePath+fileInfo[1]);
				if(receiveFile.exists()){	//����Ӧ�ļ������ļ��Ѵ��ڣ������������
					int newid =1;
					
					while(receiveFile.exists()){						
						receiveFile = new File(savePath+newid+"-"+fileInfo[1]);
						newid++;
					}
																				
				}				
				fbos = new BufferedOutputStream(new FileOutputStream(receiveFile));
				Log.d(TAG, "׼����ʼ�����ļ�....");
				bis = new BufferedInputStream(socket.getInputStream());
				int len = 0;
				long sended = 0;	//�ѽ����ļ���С								
				long receiveBytes_thisloop=0;
				long temp_Bytes_thisloop=0;
				long startTime = System.currentTimeMillis();
				long writeBytespersecond=0;
				while((len = bis.read(readBuffer)) != -1){
					fbos.write(readBuffer, 0, len);
					fbos.flush();					
					sended += len;	//�ѽ����ļ���С
					writeBytespersecond+=len;
					receiveBytes_thisloop=sended;
					
					if(receiveBytes_thisloop>(temp_Bytes_thisloop+100*1024)){//ÿд100K����һ�θ��µ�message
						temp_Bytes_thisloop=receiveBytes_thisloop;
						Message message_process = new Message();
						message_process.what=Activity_Receive.MESSAGE_REFRESH_FILEDIALOG_PROGRESS;
						long[] valueofprocess = new long[1];
						valueofprocess[0]=receiveBytes_thisloop+receiveBytes;
						message_process.obj= valueofprocess;
						Activity_Receive.sendMessage(message_process);
					}
					
					long endTime = System.currentTimeMillis();
					if((endTime-startTime)>1000){
						startTime=System.currentTimeMillis();
						long speedofBytes = writeBytespersecond;
						Log.i("ReceiveThread", "�����ٶ�"+speedofBytes+"Bytes/s");
						writeBytespersecond=0;
						Message message_speed = new Message();
						long [] valueofspeed = new long[1];
						valueofspeed[0]=speedofBytes;
						message_speed.what=Activity_Receive.MESSAGE_REFRESH_FILEDIALOG_SPEED;
						message_speed.obj=valueofspeed;
						Activity_Receive.sendMessage(message_speed);
					}
				}
				receiveBytes+=receiveBytes_thisloop;
				Log.i(TAG, "��" + (i+1) + "���ļ����ճɹ����ļ���Ϊ"  + fileInfo[1]);
				int[] success = {i+1, fileInfos.length};
				Message msg4success = new Message();
			//	msg4success.what = UsedConst.FILERECEIVESUCCESS;
				msg4success.obj = success;
			
				
			}catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				//Activity_Receive.isReceivingFileSuccess=false;
				e.printStackTrace();
				Log.e(TAG, "....ϵͳ��֧��GBK����");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				Activity_Receive.isReceivingFileSuccess=false;
				e.printStackTrace();
				Log.e(TAG, "Զ��IP��ַ����");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				Activity_Receive.isReceivingFileSuccess=false;
				e.printStackTrace();
				Log.e(TAG, "�ļ�����ʧ��");
			}catch (IOException e) {
				// TODO Auto-generated catch block
				Activity_Receive.isReceivingFileSuccess=false;
				e.printStackTrace();
				Log.e(TAG, "����IO����");
			}finally{	//����
				
				if(bos != null){	
					try {
						bos.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					bos = null;
				}
				
				if(fbos != null){
					try {
						fbos.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					fbos = null;
				}
				
				if(bis != null){
					try {
						bis.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					bis = null;
				}
				
				if(socket != null){
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					socket = null;
				}
				
			}
			
			
			
		}
		if(Activity_Receive.isReceivingFileSuccess){
			Message msg_complete=new Message();
			msg_complete.what=Activity_Receive.MESSAGE_RECEIVING_FILES_COMPLETE;
			String newpath[]=new String[1];
			newpath[0]=this.newfoldername;
			msg_complete.obj=newpath;
			Activity_Receive.sendMessage(msg_complete);
		}
		

	}
	
	

}
