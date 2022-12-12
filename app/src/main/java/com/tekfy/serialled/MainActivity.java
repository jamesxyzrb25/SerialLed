package com.tekfy.serialled;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";

    UsbDevice device;
    UsbManager usbManager;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    Button btnIniciar, btnDetener;
    SwitchCompat swLed13,swLed12,swLed11, swLed10;

    ImageView ivMic;
    TextView txtSpeechTotText;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usbManager = (UsbManager)getSystemService(this.USB_SERVICE);
        btnIniciar = findViewById(R.id.btnOn);
        btnDetener = findViewById(R.id.btnOff);
        swLed13 = findViewById(R.id.swLed13);
        swLed12 = findViewById(R.id.swLed12);
        swLed11 = findViewById(R.id.swLed11);
        swLed10 = findViewById(R.id.swLed10);

        ivMic = findViewById(R.id.iv_mic);
        txtSpeechTotText = findViewById(R.id.txtSpeechTotText);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        ivMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Speak to Text");

                try{
                    startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
                }catch (Exception e){
                    Toast.makeText(MainActivity.this, "Fail startActivityForResult", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_SPEECH_INPUT){
            if(serialPort != null){
                if(resultCode == RESULT_OK && data != null){
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    txtSpeechTotText.setText(Objects.requireNonNull(result).get(0));
                    serialPort.write(Objects.requireNonNull(result).get(0).getBytes());
                }
            }else{
                Toast.makeText(this, "No hay conexión",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ACTION_USB_PERMISSION)){
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if(granted){
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if(serialPort != null){
                        if(serialPort.open()){
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallBack);
                            Toast.makeText(context, "Serial connection open!", Toast.LENGTH_SHORT).show();
                        }else{
                            Log.d("SERIAL","PORT NO OPEN");
                            Toast.makeText(context, "PORT NO OPEN", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Log.d("SERIAL","PORT IS NULL");
                        Toast.makeText(context, "PORT IS NULL", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Log.d("SERIAL","PERM NOT GRANTED");
                    Toast.makeText(context, "PERM NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
            }else if(intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
                onClickConectar(btnIniciar);
            }else if(intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)){
                onClickDesconectar(btnDetener);
            }
        }
    };

    UsbSerialInterface.UsbReadCallback mCallBack = (arg0) -> {
            String data = null;
            try{
                data = new String(arg0, "UTF-8");
                data.concat("\n");
            }catch(UnsupportedEncodingException e){
                e.printStackTrace();
            }
    };

    public void onClickConectar(View view) {
        HashMap<String,UsbDevice> usbDevices = usbManager.getDeviceList();
        if(!usbDevices.isEmpty()){
            boolean keep = true;
            for(Map.Entry<String, UsbDevice>entry:usbDevices.entrySet()){
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if(deviceVID == 6790 || deviceVID==1659 || deviceVID == 2341 || deviceVID == 9025){
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                }else{
                    connection = null;
                    device = null;
                    Toast.makeText(this, "Conexion null o device null", Toast.LENGTH_SHORT).show();
                    Toast.makeText(this, "VID es: "+deviceVID, Toast.LENGTH_SHORT).show();
                }
                if(!keep)
                    break;
            }
        }
    }

    public void onClickDesconectar(View view) {
        if(serialPort != null){
            serialPort.close();
            Toast.makeText(this, "Cerrando conexión", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "No hay conexión", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickLed13(View view) {
        if(serialPort != null){
            if(view.getId()== R.id.swLed13){
                if(swLed13.isChecked()){
                    String string = "P";
                    serialPort.write(string.getBytes());
                }else{
                    String string = "P";
                    serialPort.write(string.getBytes());
                }
            }
        }else{
            Toast.makeText(this, "No hay conexión", Toast.LENGTH_SHORT).show();
        }

    }

    public void onClickLed12(View view) {
        if(serialPort != null){
            if(view.getId()== R.id.swLed12){
                if(swLed12.isChecked()){
                    String string = "R";
                    serialPort.write(string.getBytes());
                }else{
                    String string = "R";
                    serialPort.write(string.getBytes());
                }
            }
        }else{
            Toast.makeText(this, "No hay conexión", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickLed11(View view) {
        if(serialPort != null){
            if(view.getId()== R.id.swLed11){
                if(swLed11.isChecked()){
                    String string = "Y";
                    serialPort.write(string.getBytes());
                }else{
                    String string = "Y";
                    serialPort.write(string.getBytes());
                }
            }
        }else{
            Toast.makeText(this, "No hay conexión", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickLed10(View view) {
        if(serialPort != null){
            if(view.getId()== R.id.swLed10){
                if(swLed10.isChecked()){
                    String string = "G";
                    serialPort.write(string.getBytes());
                }else{
                    String string = "G";
                    serialPort.write(string.getBytes());
                }
            }
        }else{
            Toast.makeText(this, "No hay conexión", Toast.LENGTH_SHORT).show();
        }
    }
}