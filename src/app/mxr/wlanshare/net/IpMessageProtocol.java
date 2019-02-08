package app.mxr.wlanshare.net;

import java.util.Date;


/**
 * IPMSGЭ�������
 * 
 */
public class IpMessageProtocol {
	private String version;	//�汾�� Ŀǰ��Ϊ1
	private String packetNo;//���ݰ����
	private String senderName;	//�������ǳƣ�����PC����Ϊ��¼����
	private String senderHost;	//����������
	private int commandNo;	//����
	private String additionalSection;	//��������
	
	public IpMessageProtocol(){
		this.packetNo = getSeconds();
	}
	
	// ����Э���ַ�����ʼ��
	public IpMessageProtocol(String protocolString){
		String[] args = protocolString.split(":");	// ��:�ָ�Э�鴮
		version = args[0];
		packetNo = args[1];
		senderName = args[2];
		senderHost = args[3];
		commandNo = Integer.parseInt(args[4]);
		if(args.length >= 6){	//�Ƿ��и�������
			additionalSection = args[5];
		}else{
			additionalSection = "";
		}
		for(int i = 6; i < args.length; i++){	//��������������:�����
			additionalSection += (":" + args[i]);
		}
		
	}
	
	public IpMessageProtocol(
			String senderName, String senderHost, int commandNo,
			String additionalSection) {
		super();
		this.version = "1";
		this.packetNo = getSeconds();
		this.senderName = senderName;
		this.senderHost = senderHost;
		this.commandNo = commandNo;
		this.additionalSection = additionalSection;
	}


	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getPacketNo() {
		return packetNo;
	}
	public void setPacketNo(String packetNo) {
		this.packetNo = packetNo;
	}
	public String getSenderName() {
		return senderName;
	}
	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}
	public String getSenderHost() {
		return senderHost;
	}
	public void setSenderHost(String senderHost) {
		this.senderHost = senderHost;
	}
	public int getCommandNo() {
		return commandNo;
	}
	public void setCommandNo(int commandNo) {
		this.commandNo = commandNo;
	}
	public String getAdditionalSection() {
		return additionalSection;
	}
	public void setAdditionalSection(String additionalSection) {
		this.additionalSection = additionalSection;
	}
	
	//�õ�Э�鴮
	public String getProtocolString(){
		StringBuffer sb = new StringBuffer();
		sb.append(version);
		sb.append(":");
		sb.append(packetNo);
		sb.append(":");
		sb.append(senderName);
		sb.append(":");
		sb.append(senderHost);
		sb.append(":");
		sb.append(commandNo);
		sb.append(":");
		sb.append(additionalSection);
		
		return sb.toString();
	}
	
	//�õ����ݰ���ţ�������
	private String getSeconds(){
		Date nowDate = new Date();
		return Long.toString(nowDate.getTime());
	}
	
}