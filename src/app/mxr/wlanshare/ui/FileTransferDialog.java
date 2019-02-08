package app.mxr.wlanshare.ui;

import java.text.DecimalFormat;

import android.app.AlertDialog;
import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.github.ghmxr.wlanshare.R;

public class FileTransferDialog extends AlertDialog{
	private Context context;
	private ProgressBar pg;
	private TextView progress,speed,currentfile;
	private long finishedBytes=0,totalBytes=0,speedofBytes=0;
	private int finishedpercent=0;

	public FileTransferDialog(Context context) {
		super(context);
		this.context=context;
		// TODO Auto-generated constructor stub
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		View dialogview = layoutInflater.inflate(R.layout.dialog_filetransfer,null);
		this.setView(dialogview);
		currentfile=(TextView)dialogview.findViewById(R.id.currentfile);
		pg=(ProgressBar)dialogview.findViewById(R.id.filetransferprogressBar);
		progress=(TextView)dialogview.findViewById(R.id.transferprogress);
		speed=(TextView)dialogview.findViewById(R.id.transferspeed);
	}
	
	
	/**
	 * ���öԻ���Ľ��ȣ���λBytes
	 * @param long Bytes  ����ɵ�Bytes��
	 */
	public void setProgress(long Bytes){
		this.finishedBytes=Bytes;	
		refreshProgress(this.finishedBytes);
	}
	
	/**
	 * ���öԻ���������ȣ���λBytes
	 * @param long Bytes  ����Bytes��
	 */
	public void setMax(long Bytes){
		this.totalBytes=Bytes;
		this.pg.setMax((int)(this.totalBytes/1024));
	}
	
	/**
	 * ���öԻ���Ĵ����ٶȣ���λBytes���Ի�����Զ������ٶȵĵ�λֵkb��mb/s
	 * @param long Bytes  ��ǰ���ٶȣ���λBytes
	 */
	public void setSpeed(long speedofBytes){
		this.speedofBytes=speedofBytes;
		refreshSpeed();
	}
	/**
	 * ���õ�ǰ�Ի�����ʾ���ļ���Ϣ
	 */
	public void setFilename(String name){
		this.currentfile.setText(name);
	}
	
	private void refreshProgress(long progressofBytes){	
		DecimalFormat dm=new DecimalFormat("#.00");			
		int percent=(int)(Double.valueOf(dm.format((double)this.finishedBytes/this.totalBytes))*100);		
		this.pg.setProgress((int)(progressofBytes/1024));
	//	if(percent!=this.finishedpercent){
		this.finishedpercent=percent;

			/* if(this.totalBytes>(1024*1024*20)){			
				this.progress.setText(Double.valueOf(dm.format(((double)this.finishedKBytes/1024)))+"MB/"+Double.valueOf(dm.format(((double)this.totalKBytes/1024)))+"MB("+this.finishedpercent+"%)");
			}
			else{			
				this.progress.setText(this.finishedKBytes+"KB/"+this.totalKBytes+"KB("+this.finishedpercent+"%)");
			}   */
			
						
			
	//	}
		
		this.progress.setText(Formatter.formatFileSize(this.context, this.finishedBytes)+"/"+Formatter.formatFileSize(this.context, this.totalBytes)+"("+this.finishedpercent+"%)");
		
	}
	
	private void refreshSpeed(){
	/*	DecimalFormat dm=new DecimalFormat("#.00");
		if(this.speedofKBytes>1000){
			double speedofMB=Double.valueOf(dm.format(((double)speedofKBytes/1024)))  ;
			this.speed.setText(speedofMB+"MB/s");
		}
		else{
			this.speed.setText(speedofKBytes+"KB/s");
		}  */
		
		
		this.speed.setText(Formatter.formatFileSize(this.context, this.speedofBytes)+"/s");
		
	}
		

}
