package app.mxr.wlanshare.net;

/**
 * Э�鳣��
 */

public class IpMessageConst {
	public static final int VERSION = 0x001;		// �汾��
	public static final int PORT = 0x097a;			// �˿ں�   Ĭ�϶˿�2426
	
	
	public static final int IPMSG_NOOPERATION		 		 = 0x00000000;	//�������κβ���

	public static final int IPMSG_REQUEST_ONLINE_DEVICES	 = 0x00000001;	//����Ѱ�������豸����
	public static final int IPMSG_DEVICE_OFFLINE		 	 = 0x00000002;	//�û��˳�
	public static final int IPMSG_ANSENTRY			         = 0x00000003;	//ͨ������
	public static final int IPMSG_ANSENTRY_SENDER			 = 0x00000004;	//���Ͷ˸���ķ���
	public static final int IPMSG_SENDPORT_INTERRUPT 		 = 0x00000005;  //���Ͷ��ڵȴ��Է�����ȷ��ʱȡ���˷���
	public static final int IPMSG_FILE_REGULAR 			 	 = 0x00000006;
	
	public static final int IPMSG_SENDMSG 			         = 0x00000020;	//������Ϣ
	public static final int IPMSG_RECVMSG 			 		 = 0x00000021;	//ͨ���յ���Ϣ
	
	public static final int IPMSG_GETFILEDATA		 		 = 0x00000060;	//�ļ���������
	public static final int IPMSG_RELEASEFILES		 		 = 0x00000061;	//���ն˾ܾ������ļ�
	public static final int IPMSG_ACCEPT_RECEIVINGFILES 	 = 0x00000062;  //���ն�ͬ������ļ�
	public static final int IPMSG_INTERRUPT_FILETRANSFER     = 0x00000063;  //�ļ�������;����һ����ֹ�˷��ͻ����
	
	public static final int IPMSG_SENDCHECKOPT 				 = 0x00000100;	//������֤
	
	
	public static final int IPMSG_FILEATTACHOPT 	         = 0x00200000;	//�����ļ�
	

}
