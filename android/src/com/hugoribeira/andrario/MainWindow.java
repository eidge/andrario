// TODO 
// set vario options on android and send them to arduino

package com.hugoribeira.andrario;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainWindow extends Activity {
	private FTDriver mSerial;
    private int mDataBits           = FTDriver.FTDI_SET_DATA_BITS_8;
    private int mParity             = FTDriver.FTDI_SET_DATA_PARITY_NONE;
    private int mStopBits           = FTDriver.FTDI_SET_DATA_STOP_BITS_1;
    private int mFlowControl        = FTDriver.FTDI_SET_FLOW_CTRL_NONE;
    private int mBreak              = FTDriver.FTDI_SET_NOBREAK;
    
    private static final int MENU_ID_START        = 0;
    private static final int MENU_ID_CONNECT      = 1;
    private static final int MENU_ID_OPTIONS      = 2;
    
    private boolean mStop = false;
    private boolean mRunningMainLoop = false;
    
	
	private TextView varioText;
	private LinearLayout mainLayout;
	
	private float vario_value;

	private Menu optionsMenu;
	
	String text = "";
	
	Handler mHandler = new Handler();
	
	
	private static final String ACTION_USB_PERMISSION =
            "com.hugoribeira.andrario.USB_PERMISSION";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //No bar on the top
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_main_window);
        
        
        varioText = (TextView) findViewById(R.id.vario_text);
        mainLayout = (LinearLayout) findViewById(R.id.main_layout);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
    }
    
    @Override
    public void onDestroy() {
    	if(mSerial != null)
    		mSerial.end();
    	
        mStop = true;
        super.onDestroy();
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE, MENU_ID_START, Menu.NONE, "Start Vario");
    	menu.add(Menu.NONE, MENU_ID_CONNECT, Menu.NONE, "Find Vario");
    	
    	menu.getItem(MENU_ID_START).setEnabled(false);
    	
    	optionsMenu = menu;

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_CONNECT:
                connect();
                return true;
            case MENU_ID_START:
                readSerial();
                return true;
            default:
                return false;
        }
    }
    
    public void openOptions(View view){
    	openOptionsMenu ();
    }
    
    public void connect() {
        mSerial = new FTDriver((UsbManager)getSystemService(Context.USB_SERVICE));
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                ACTION_USB_PERMISSION), 0);
        mSerial.setPermissionIntent(permissionIntent);

        if(mSerial.begin(FTDriver.BAUD9600)) {
            Toast.makeText(this, "connected", Toast.LENGTH_SHORT).show();
            mSerial.setSerialPropertyDataBit(mDataBits, FTDriver.CH_A);
            mSerial.setSerialPropertyToChip(FTDriver.CH_A);
            mSerial.setSerialPropertyParity(mParity, FTDriver.CH_A);
            mSerial.setSerialPropertyToChip(FTDriver.CH_A);
            mSerial.setSerialPropertyStopBits(mStopBits, FTDriver.CH_A);
            mSerial.setSerialPropertyToChip(FTDriver.CH_A);
            mSerial.setFlowControl(FTDriver.CH_A, mFlowControl);
            mSerial.setSerialPropertyBreak(mBreak, FTDriver.CH_A);
            mSerial.setSerialPropertyToChip(FTDriver.CH_A);
            
            optionsMenu.getItem(MENU_ID_START).setEnabled(true);
            optionsMenu.getItem(MENU_ID_CONNECT).setEnabled(false);
        }
        else {
            Toast.makeText(this, "cannot connect", Toast.LENGTH_SHORT).show();
            optionsMenu.getItem(MENU_ID_START).setEnabled(false);
        }
    }
    
    public void readSerial(){
    	if(!mRunningMainLoop){
    		mRunningMainLoop = true;
    		optionsMenu.getItem(MENU_ID_START).setEnabled(false);
    		findViewById(R.id.vario_units).setVisibility(0);
    		
    		new Thread(mLoop).start();
    	} else{
    		Toast.makeText(this, "Already started", Toast.LENGTH_SHORT).show();
    	}
    }
    
    private Runnable mLoop = new Runnable() {
		@Override
		public void run() {
			byte[] rbuf = new byte[4096];
			int len = 0;
			
			//Read values from serial port and update the vario field
			for(;;){
				len = mSerial.read(rbuf);
				
				if(len>0){
					vario_value = -15 + (float)0.1*unsignedToBytes((byte)rbuf[len - 1]);
					
					if(vario_value <= 0.05){
						text = String.format("%.1f", vario_value);
					}
					else{
						text = String.format("+%.1f", vario_value);
					}
					

					
					//Update value in the main thread
					mHandler.post(new Runnable() {
						public void run() {
							varioText.setText(text);
							
							//Change screen color with vario input
							if(vario_value < -2.0) //TODO set properties
								mainLayout.setBackgroundColor(Color.RED);
							else if(vario_value > 1) //TODO set properties
								mainLayout.setBackgroundColor(Color.GREEN);
							else
								mainLayout.setBackgroundColor(Color.parseColor("#0099cc"));
						}
					});				
				}
				
	            try {
	                Thread.sleep(50);
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }

	            if (mStop) {
	                mRunningMainLoop = false;
	                return;
	            }
			}
		}
    };
    
    public static int unsignedToBytes(byte b) {
    	return b & 0xFF;
    }
    
}