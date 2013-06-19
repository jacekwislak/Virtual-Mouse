package com.example.remotemouse;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.R.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.gesture.Gesture;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener, android.view.View.OnClickListener, OnGestureListener 
{

	private InetAddress adresip;
	private Integer port;
	private DatagramSocket soket;
	private short prevx,prevy;
	private GestureDetector gestureScanner;
	private boolean polaczony=false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!polaczony)
			setContentView(R.layout.activity_main);
		else
			setContentView(R.layout.touchpad);
		gestureScanner = new GestureDetector(this);
        Button btn1 = (Button)findViewById(R.id.button1);
        btn1.setOnClickListener(this);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    if (soket!=null)
			this.polaczony=true;
	}


    public void onClick(View v) {
        switch(v.getId()){
            case R.id.button1:
            	//POLACZ
            	String ip;
            	
            	//pobranie ip i portu z formularza
            	ip=((EditText) findViewById(R.id.editText1)).getText().toString();
        	    port = Integer.valueOf(
        	    		((EditText) findViewById(R.id.editText2)).getText().toString());
        	    
        	    //tworzenie gnizda
				try {
					soket = new DatagramSocket(port);
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//pobranie ip serwera
				try {
					adresip = InetAddress.getByName(ip);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				setContentView(R.layout.touchpad);
				polaczony=true;
            	break;
        }
    }


    public boolean onTouchEvent(MotionEvent event) {
    	short x=1,y=1,tmpx, tmpy;
    	byte [] wspolrzedne=new byte[4];
    	DatagramPacket pakiet;
    	
    	
        switch (event.getAction()) {

        case MotionEvent.ACTION_DOWN:
        	 prevx = (short) new Float(event.getX()).intValue();
        	 prevy = (short) new Float(event.getY()).intValue();
            break;
        case MotionEvent.ACTION_MOVE:
        	//pobranie wspó³rzednych dotyku 	
    		x=(short) new Float(event.getX()).intValue();
    		y=(short) new Float(event.getY()).intValue();
    		tmpx=x;
    		tmpy=y;
    		
        	///odejmowanie wspó³rzednych od startowych
    		x-=prevx;
    		y-=prevy;
    		
    		//przypisanie wspó³rzednych jako poprzednie
    		prevx=tmpx;
    		prevy=tmpy;

        	//konwersja do przyjetego formatu bufora
        	wspolrzedne[0]=(byte) (x>>8);
        	wspolrzedne[1]=(byte) x;
        	wspolrzedne[2]=(byte) (y>>8);
        	wspolrzedne[3]=(byte) y;
        	
        	//tworzenie pakietu
        	pakiet=new DatagramPacket(
        			wspolrzedne, wspolrzedne.length,this.adresip,this.port);
        	
        	//wysylanie pakietu w odzielnym watku
        	new sendudp(this.soket, pakiet).execute();
            break;
            
        }
        return gestureScanner.onTouchEvent(event);
    	

    }


	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public boolean onDown(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void onLongPress(MotionEvent arg0) {
		byte [] wspolrzedne=new byte[4];
		DatagramPacket pakiet;
		
    	//konwersja do przyjetego formatu bufora
    	wspolrzedne[0]=0;
    	wspolrzedne[1]=0;
    	wspolrzedne[2]=(byte) (32767>>8);
    	wspolrzedne[3]=(byte) 32767;
    	
    	//tworzenie pakietu
    	pakiet=new DatagramPacket(
    			wspolrzedne, wspolrzedne.length,this.adresip,this.port);
    	
    	//wysylanie pakietu w odzielnym watku
    	new sendudp(this.soket, pakiet).execute();
	}


	@Override
	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void onShowPress(MotionEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public boolean onSingleTapUp(MotionEvent arg0) {
		byte [] wspolrzedne=new byte[4];
		DatagramPacket pakiet;
		
    	//konwersja do przyjetego formatu bufora
    	wspolrzedne[0]=(byte) (32767>>8);
    	wspolrzedne[1]=(byte) 32767;
    	wspolrzedne[2]=0;
    	wspolrzedne[3]=0;
    	
    	//tworzenie pakietu
    	pakiet=new DatagramPacket(
    			wspolrzedne, wspolrzedne.length,this.adresip,this.port);
    	
    	//wysylanie pakietu w odzielnym watku
    	new sendudp(this.soket, pakiet).execute();

		return false;
	}
	

}

class sendudp extends AsyncTask<Void, Void, Void> {

	private DatagramPacket pakiet;
	private DatagramSocket soket;
	
	public sendudp(DatagramSocket socket, DatagramPacket packet) {
		this.pakiet=packet;
		this.soket=socket;
	}


	@Override
	protected Void doInBackground(Void... params) {
		try {
			soket.send(pakiet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

 }
