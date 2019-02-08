package app.mxr.wlanshare.utils;

import java.io.File;

public class ExtName {
	
	/**
	 * ��ȡһ���ļ�����չ��
	 * ���� abc.png  ���� png
	 * @param   �ļ�·��
	 * @return  String  �ļ�����չ��
	 */
	
	public static String  getFileExtName(String filepath) {
        File file = new File(filepath);
        String fileName = file.getName();
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
     //   System.out.println(suffix);
        return suffix;
    }
	
	
	/**
	 * ��ȡһ���ļ�����չ��
	 * ���� abc.png  ���� png
	 * @param File file  �ļ�
	 * @return  String  �ļ�����չ��
	 */
	public static String  getFileExtName(File file){
		return file.getName().substring(file.getName().lastIndexOf(".")+1);
	}

}
