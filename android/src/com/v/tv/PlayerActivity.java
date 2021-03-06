package com.v.tv;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class PlayerActivity extends Activity {
	public MediaController mCtl = null;
	public VideoView  video = null;
	public Button btnSetup = null;
	public WebView   webView = null;
	private java.util.Timer timerStartupDownload = null;
	private java.util.Timer timerQueryServer = null;
	public static  long  startTime = 0;
	final long lWaitSecForSetup = 20*1000;
	public static boolean webMode = false;
	public static PlayerActivity playActivity = null;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playActivity = this;
        startTime = System.currentTimeMillis();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD, 
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                
         // Hide status bar
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        

        webView = (WebView) findViewById(R.id.webView1);
        video = (VideoView) findViewById(R.id.videoView1);
        btnSetup = (Button) findViewById(R.id.buttonSetting);
        btnSetup.setOnClickListener(new OnClickListener() {
        	 @Override 
            public void onClick(View v) {
         		Intent t = new Intent();
         		t.setClass(PlayerActivity.this, SetupActivity.class);
         		startActivity(t);
         		PlayerActivity.this.finish();
            }
          });
        
        if (webMode){
        	WebSettings set = webView.getSettings();
        	set.setAllowFileAccess(true);
        	set.setJavaScriptEnabled(true);
        	webView.loadUrl("http://www.baidu.com");
        	video.setVisibility(View.GONE);
        	webView.setVisibility(View.VISIBLE);
        }
        else{
        	video.setVisibility(View.VISIBLE);
        	webView.setVisibility(View.GONE);
        }
      
       
        boolean show = Configuration.getInstance().basConfig.showSetting;
      //  if (!show) btnSetup.setVisibility(View.GONE);
        
        settingChanges();
        
        String strId = Utility.UID(this);
        
    } 
    public void settingChanges()
    {
    	 AppKernel.appStatus = App_Status.waitingResource;
         new Thread(downloadRun).start();  
         
       
         timerStartupDownload = new java.util.Timer(true); 
         timerStartupDownload.schedule(timerTaskDownload, new Date(), 2000);
         
         timerQueryServer = new java.util.Timer(true); 
         timerQueryServer.schedule(taskQueryServer, new Date(), 2000);
    }
    public Runnable downloadRun = new Runnable(){  
	    @Override  
	    public void run() {  
          
	        // TODO Auto-generated method stub  
	        NetClient c = new NetClient();
	            
	        //the client.cfg must download very time
	        DownloadItem item = Configuration.getInstance().siteConfig.listFiles.get(0);
	    	AppKernel.netStatus = Net_Status.downloadingMovie;
	    	while(true)
	    	{
	    		boolean bRet = c.downloadFile(item.URL);
	     		if (bRet) {
	     			item.status = Net_Status.finish;
	     			Configuration.getInstance().siteConfig.ParseConfig();
	     			break;
	     		}
	    	}
       	
	    	int size = Configuration.getInstance().siteConfig.listFiles.size();
	    	for (int i = 1; i<size; i++)
	    	{
            	item = Configuration.getInstance().siteConfig.listFiles.get(i);
	           	String localFile = AppEnv.getLocalFile(item.URL);
	           	boolean exist = Utility.fileExists(localFile);
	           	if (exist){
	           		item.status = Net_Status.finish;
	           	}
	           	else {
	           		boolean bRet = c.downloadFile(item.URL);
	           		if (bRet)  AppKernel.netStatus = Net_Status.finish;
	           	}
	           
	        }//for
	            
	    }
    } ;
    private String lastPlayfile;
    public void HandlePlayMessage(Message msg)
    {
    	  try{
              String filename = getPlayFileName();
	           video.setVideoPath(filename);
	           lastPlayfile = filename;  
	            final MediaController mc = new MediaController(PlayerActivity.this);
  	        video.setMediaController(mc);
  	        video.requestFocus();
  	        video.start();
  	        
  	        video.setOnCompletionListener(new OnCompletionListener() {
                  public void onCompletion(MediaPlayer mp) {
                     // stopAudio();
                  	System.out.print("on complettion");
                  	String filename = getPlayFileName();
                  	
                  	//decrease the flash
                  	if (filename.compareTo(lastPlayfile) !=0 ){
                      	video.setVideoPath(filename);		
                  	}

                  	lastPlayfile = filename;
                    video.start();
                  }
              });
  	        video.setOnErrorListener(new MediaPlayer.OnErrorListener(){
  	   			@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					// TODO Auto-generated method stub
	  	   		  	System.out.print("on complettion");
	              	String filename = getPlayFileName();
	                mp.reset();
	              	//decrease the flash
	              	if (filename.compareTo(lastPlayfile) !=0 ){
	                  	video.setVideoPath(filename);		
	              	}
	
	              	lastPlayfile = filename;
	                video.start();
					return false;
				}
  	        });

  	        //setOnInfoListener (MediaPlayer.OnInfoListener listener)
	        }
	        catch (Exception e)
	        {
	        	String str = e.getMessage();
	        	System.out.print(str);
	   
	        }   	
    }
    private int fileIndex = 3;
    private String getPlayFileName()
    {
    	List<DownloadItem> list = Configuration.getInstance().siteConfig.listFiles;
    	/*PlayMode m = Configuration.getInstance().siteConfig.playMode;
    	
    	switch (Configuration.getInstance().siteConfig.playMode)
		{
		case singleFileLoop:
			//siteConfig
			break;
		case listLoop:
			break;
		case once:
			break;
		}*/
    	
    	int size = list.size();
    	if (fileIndex>=size) fileIndex = 1;
    	  	
    	DownloadItem it = list.get(fileIndex);
    	String localFile = AppEnv.getLocalPath() + it.fileName;
    	fileIndex++;
    	
    	return localFile;
    }
    //start to play
    Handler mHandler = new Handler() {  
        @Override  
        public void handleMessage(Message msg)
        {  
        	switch (msg.what)
        	{
        		case 1:  //play
        			HandlePlayMessage(msg);
        			break;
        		case 2: {//hide button
        			if (btnSetup.getVisibility() != View.GONE){
            			btnSetup.setVisibility(View.GONE);
        			}

        		 }
        		break;
        	}
            super.handleMessage(msg);  
        }  
    };
    
    //check the download 
    public TimerTask timerTaskDownload = new TimerTask() {   
    	public void run() {   //implemented with sub-thread
    		System.out.print("task");
    		boolean finish = Configuration.getInstance().siteConfig.allFinishDownload();
    		if (finish)  //play
    		{
    	        Message msg = mHandler.obtainMessage();  
                msg.what = 1;  
                
   
               // msg.obj 
                msg.sendToTarget(); 
    	        
    	        timerStartupDownload.cancel();
    		}
    		
    		//hide button
    		long span = System.currentTimeMillis() - PlayerActivity.startTime;	
    		if (span >lWaitSecForSetup)
    		{
    		     Message msg = mHandler.obtainMessage();  
                 msg.what = 2;  //hide button
                 msg.sendToTarget(); 
    		}

    		
    		//timerStartupDownload.cancel();
    	 }   
    };  
    // 
    public TimerTask taskQueryServer = new TimerTask() {   
    	public void run() {   //implemented with sub-thread
    		System.out.print("task");
    		// get task and execute, sendToget the message
    	 }   
    };
    public void OnStart()
    {
    	super.onStart();
    	
    }
    public void OnResume()
    {
    	super.onResume();
    }
    public void OnPause()
    {
    	super.onPause();
    }
    public void OnStop()
    {
    	super.onStop();
 
    }
}