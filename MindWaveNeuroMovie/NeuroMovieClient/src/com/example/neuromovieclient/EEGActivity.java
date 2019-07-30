package com.example.neuromovieclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts.Intents.Insert;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGEegPower;

public class EEGActivity extends Activity {
    
    private static final String TAG = "HelloEEG";

    BluetoothAdapter            bluetoothAdapter;
    TGDevice                    device;

    final boolean               rawEnabled = true;

    ScrollView                  sv;
    EditText 					et;
    TextView                    tv;
    Button                      b;
    
    double sum = 0;
    double sum_anfis = 0;
    int count = 0;
    int time = 0;
    double[] window = new double [10];
    double[] window_anfis = new double [10];
    double avgsum = 50;
    double avgsum_anfis = 50;
    double thr = 40;
    
    long[] TS1 = new long [3000];
    long[] TS2 = new long [3000];
    long[] TE1 = new long [3000];
    long[] TE2 = new long [3000];

    long Latency_Sum_1 = 0;
    int counter_1 = 0;
    
    long Latency_Sum_2 = 0;
    int counter_2 = 0;
    
    // MarkovModel mm = new MarkovModel();
    
    private Socket socket1;
    private Socket socket2;
    private static final int SERVERPORT1 = 6000;
    private static final int SERVERPORT2 = 6969;
    private static String SERVER_IP1 = null;
    private static String SERVER_IP2 = null;
    
    String predictRCV = "0 0 0 0 0 0";
    
    PrintWriter out1;
    BufferedReader input2;
    PrintWriter out2;
    PrintStream out;
     
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        sv = (ScrollView)findViewById(R.id.scrollView1);
        et = (EditText)findViewById(R.id.editText1);
        tv = (TextView)findViewById(R.id.textView1);
        
        tv.setText( "" );
        tv.append( "Android version: " + Integer.valueOf(android.os.Build.VERSION.SDK) + "\n" );
        	            
        // Check if Bluetooth is available on the Android device
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if( bluetoothAdapter == null ) {            

        	// Alert user that Bluetooth is not available
        	Toast.makeText( this, "Bluetooth not available", Toast.LENGTH_LONG ).show();
        	//finish();
        	return;

        } else {
            
        	// create the TGDevice 	
        	device = new TGDevice(bluetoothAdapter, handler);
        } 

        tv.append("NeuroSky: " + TGDevice.version + " " + TGDevice.build_title);
        tv.append("\n" );
        
        // new Thread(new ClientThread()).start();
   
    } 
	/* end onCreate() */
    
    

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        //if (!bluetoothAdapter.isEnabled()) {
          //  Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableIntent, 1);
        //}
    }

    @Override
    public void onPause() {
    	// device.close();
        super.onPause();
    }
    
    @Override
    public void onStop() {
        //device.close();
        super.onStop();
    }
    
    @Override
    public void onDestroy() {
    	device.close();
        super.onDestroy();
    }
       
    /**
     * Handles messages from TGDevice
     */
    final Handler handler = new Handler() {  	  	
        @Override
        public void handleMessage( Message msg ) {

            switch( msg.what ) {
                case TGDevice.MSG_STATE_CHANGE:
    
                    switch( msg.arg1 ) {
    	                case TGDevice.STATE_IDLE:
    	                    break;
    	                case TGDevice.STATE_CONNECTING:       	
    	                	tv.append( "Connecting...\n" );
    	                	break;	
                        case TGDevice.STATE_CONNECTED:
                            tv.append( "Connected.\n" );
                            device.start();                                                     
                            break;
    	                case TGDevice.STATE_NOT_FOUND:
    	                	tv.append( "Could not connect any of the paired BT devices.  Turn them on and try again.\n" );
    	                	break;
                        case TGDevice.STATE_ERR_NO_DEVICE:
                            tv.append( "No Bluetooth devices paired.  Pair your device and try again.\n" );
                            break;
    	                case TGDevice.STATE_ERR_BT_OFF:
    	                    tv.append( "Bluetooth is off.  Turn on Bluetooth and try again." );
    	                    break;
    	                case TGDevice.STATE_DISCONNECTED:
    	                	tv.append( "Disconnected.\n" );
                    } /* end switch on msg.arg1 */

                    break;
                                       
//                case TGDevice.MSG_EEG_POWER:
//                	TGEegPower ep = (TGEegPower)msg.obj;
//                	Log.v( "HelloEEG", "Alpha: " + ep.lowAlpha );
//                	
//                	double alpha = ep.lowAlpha + ep.highAlpha;
//                	
//                	if (alpha != 0) {
//                	               	
//                	if (mm.ComputeProbability(alpha) == 0) {
//                		tv.append(alpha + " , " + 0 + "\n");
//                		out.println(0);
//                		
//                	} else if (mm.ComputeProbability(alpha) == 1){
//                		tv.append(alpha + " , " + 1 + "\n");
//                		out.println(1);
//                		              		
//                	} else {
//                		tv.append( "Processing...\n");	
//                	}
//                }
//                	
//                	break;
                    
                case TGDevice.MSG_MEDITATION:
                	
                	TS1[counter_1] = System.currentTimeMillis();
                	Log.e("Start", String.valueOf(TS1[counter_1]));
                	//TS2[counter_2] = System.currentTimeMillis();
                	out2.println(msg.arg1);
			String s = Integer.toString(msg.arg1);
			out.println(s);
			out.flush();
				
                	
                	window[count] = msg.arg1;
                	window_anfis[count] = window[count];
                	
//				try {
//					predictRCV = input2.readLine();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				
				String[] predictRCVs = predictRCV.split(" ");
	            double[] prediction = new double[predictRCVs.length];
	            for (int i = 0; i < predictRCVs.length; i++){
	                prediction[i] = Double.parseDouble(predictRCVs[i]);
	                Log.i("Prediction: ", Double.toString(prediction[i]));
	            }
	            
	            for (int i = 0; i < 4; i++)
            		window_anfis[i] = window_anfis[i + 6];
	            
	            for (int i = 0; i < 6; i++) {
            		window_anfis[i + 4] = prediction[i];
	            }
                	
                	time++;
                	count = time % 10;
                	sum = 0;
                	sum_anfis = 0;
                	
                	for (int i = 0; i < 10; i++)
    		            sum = sum + window[i];
                	
                	for (int i = 0; i < 10; i++)
                		sum_anfis = sum_anfis + window_anfis[i];
                	               	
                	avgsum = sum / 10;
                	avgsum_anfis = sum_anfis / 10;
                	
                	if (time == 300) {
                		thr = avgsum;
                	}
                	
                	if (avgsum_anfis < thr * 75 / 100) {

                		tv.append(CurrentTime("yyyy-MM-dd HH:mm:ss") + ": " + "Nervous, " + "Med_level: " + avgsum + ", " + "ANFIS: " + avgsum_anfis + ",thr: " + thr + "\n");
                		//tv.append(CurrentTime("yyyy-MM-dd HH:mm:ss") + ": " + "Nervous, " + "Med_level: " + avgsum + ", " + "ANFIS: " + ",thr: " + thr + "\n");
                		out1.println(0);
                		
//                		TE2[counter_2] = System.currentTimeMillis();
//                    	Latency_Sum_2 = Latency_Sum_2  + (TE2[counter_2] - TS2[counter_2]);
//                    	counter_2++;
//                    	Log.e("Average Client Latency", String.valueOf(Latency_Sum_2 / counter_2));
                		                		
                	} else if (avgsum_anfis < thr) {
                		
                		tv.append(CurrentTime("yyyy-MM-dd HH:mm:ss") + ": " + "Unknown, " + "Med_level: " + avgsum + ", " + "ANFIS: " + avgsum_anfis + ",thr: " + thr + "\n");
                		//tv.append(CurrentTime("yyyy-MM-dd HH:mm:ss") + ": " + "Unknown, " + "Med_level: " + avgsum + ", " + "ANFIS: " + ",thr: " + thr + "\n");
                		out1.println(1);
                		
//                		TE2[counter_2] = System.currentTimeMillis();
//                    	Latency_Sum_2 = Latency_Sum_2  + (TE2[counter_2] - TS2[counter_2]);
//                    	counter_2++;
//                    	Log.e("Average Client Latency", String.valueOf(Latency_Sum_2 / counter_2));
                		
                	} else {
                		
                		tv.append(CurrentTime("yyyy-MM-dd HH:mm:ss") + ": " + "Neutral, " + "Med_level: " + avgsum + ", " + "ANFIS: " + avgsum_anfis + ",thr: " + thr + "\n");
                		//tv.append(CurrentTime("yyyy-MM-dd HH:mm:ss") + ": " + "Neutral, " + "Med_level: " + avgsum + ", " + "ANFIS: " + ",thr: " + thr + "\n");
                		out1.println(2);
                		
//                		TE2[counter_2] = System.currentTimeMillis();
//                    	Latency_Sum_2 = Latency_Sum_2  + (TE2[counter_2] - TS2[counter_2]);
//                    	counter_2++;
//                    	Log.e("Average Client Latency", String.valueOf(Latency_Sum_2 / counter_2));
                		
                	}
                	
                	TE2[counter_2] = System.currentTimeMillis();
                	Log.e("End", String.valueOf(TE2[counter_2]));
                	
                	
                        	
                	// tv.setText(Integer.toString(msg.arg1));            	
                	break;
                                                
                default:
                	break;
                	
        	} /* end switch on msg.what */
            
        	sv.fullScroll( View.FOCUS_DOWN );
        	
        } /* end handleMessage() */
        
    }; /* end Handler */
    
    /**
     * This method is called when the user clicks on the "Connect" button.
     * 
     * @param view
     */
    public void doStuff(View view) {
    	
    	int startPos1 = et.getLayout().getLineStart(0);
    	int endPos1 = et.getLayout().getLineEnd(0);
    	    	
       	SERVER_IP1 = et.getText().subSequence(startPos1, endPos1).toString();
		Log.e("IP1:", SERVER_IP1);
		
		int startPos2 = et.getLayout().getLineStart(1);
    	int endPos2 = et.getLayout().getLineEnd(1);
    	    	
       	SERVER_IP2 = et.getText().subSequence(startPos2, endPos2).toString();
		Log.e("IP2:", SERVER_IP2);
		
		new Thread(new ClientThread1()).start();
		//new Thread(new ClientThread2()).start();
		new Thread(new Client()).start();
		
		try {
		    Thread.sleep(3000);                 //3000 milliseconds is three seconds.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
    	    	
    	if( device.getState() != TGDevice.STATE_CONNECTING && device.getState() != TGDevice.STATE_CONNECTED ) {
    	    
    		device.connect( rawEnabled );
    		   		
    	}
    	
    } /* end doStuff() */
    
    class ClientThread1 implements Runnable {
    	 
        @Override
        public void run() {
 
            try {
                InetAddress serverAddr1 = InetAddress.getByName(SERVER_IP1);
 
                socket1 = new Socket(serverAddr1, SERVERPORT1);
                
                out1 = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket1.getOutputStream())),
                        true);
                 
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
             
        }       
    }
    

    class Client implements Runnable {
		
		@Override
		public void run(){
			try{
				Socket socketObject = new Socket();
				socketObject.connect(new InetSocketAddress("10.218.107.189",10001),1000);
				PrintStream out = new PrintStream (socketObject.getOutputStream());
				//String str = "anu";
				//out.println(str);
				//out.flush();
					//BufferedReader in;
					//in = new BufferedReader(new InputStreamReader(socketObject.getInputStream()));
					//String line = in.readLine();
					//System.out.println(line);
					//Thread.sleep(10);
					
					//in.close();
				
			}catch(Exception E){
				E.printStackTrace();
			}finally{
				
			}
		}
    }

	class ClientThread2 implements Runnable {
   	 
        @Override
        public void run() {
 
            try {
                InetAddress serverAddr2 = InetAddress.getByName(SERVER_IP2);
 
                socket2 = new Socket(serverAddr2, SERVERPORT2);
                
                input2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
                
                out2 = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket2.getOutputStream())),
                        true);
                
                //Log.e ("Bita: ", input2.readLine());
                
                
                while ((predictRCV = input2.readLine()) != null) {
                	
                	
                	TE1[counter_1] = System.currentTimeMillis();
                	// Log.e("End", String.valueOf(TE1[counter_1]));
                	Latency_Sum_1 = Latency_Sum_1  + (TE1[counter_1] - TS1[counter_1]);
                	counter_1++;
                	//Log.e("Average Fog Latency", String.valueOf(Latency_Sum_1 / counter_1));
                	
                	
                
                //predictRCV = input2.readLine();
                
                }
                 
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
             
        }       
    }
    
    public void createFile(){
        FileWriter fWriter;
        try{
             
        	 String s1 = "/sdcard/nMovie/"; 
        	 String s2 = String.valueOf(System.nanoTime());
        	 String s3 = ".txt";
        	 String s = s1 + s2 + s3;
        	 fWriter = new FileWriter(s);
        	
             fWriter.write(tv.getText().toString());
             fWriter.flush();
             fWriter.close();
         }catch(Exception e){
                  e.printStackTrace();
         }
    }
    
  //turn off app when touch return button of phone
    public boolean onKeyDown(int keyCode,KeyEvent event)
    {
    	if(keyCode==KeyEvent.KEYCODE_BACK&&event.getRepeatCount()==0)
    	{
    		createDirIfNotExists("nMovie");
    		createFile();
    		device.close();	
    		this.finish();
    		return true;
    	}
    	return super.onKeyDown(keyCode, event);
    }
    
    public static boolean createDirIfNotExists(String path) {
        boolean ret = true;

        File file = new File(Environment.getExternalStorageDirectory(), path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e("TravellerLog :: ", "Problem creating Image folder");
                ret = false;
            }
        }
        return ret;
    }
    
    private String CurrentTime(String timeFormat) {
    	  String time = "";
    	  SimpleDateFormat df = new SimpleDateFormat(timeFormat);
    	  Calendar c = Calendar.getInstance();
    	  time = df.format(c.getTime());
    	 
    	  return time;
    	}
  
    
    
} /* end EEGActivity() */