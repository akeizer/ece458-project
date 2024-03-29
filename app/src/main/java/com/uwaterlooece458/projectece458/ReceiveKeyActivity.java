package com.uwaterlooece458.projectece458;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.ClosedByInterruptException;

public class ReceiveKeyActivity extends AppCompatActivity {
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothServerSocket tmp = null;
    private Handler mHandler;
    public static final java.util.UUID MY_UUID
            = java.util.UUID.fromString("B10E7007-CCD4-BBD7-1AAA-5EC000000017");
    private AcceptThread bluetoothThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_key);

        Button button = (Button) findViewById(R.id.cancel_get_key);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ReceiveKeyActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });


        boolean performRun = true;
        try {
            // MY_UUID is the app's UUID string, also used by the client code.
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("AcceptKeys", MY_UUID);
        } catch (IOException e) {
            Log.e("BLUETOOTHSECURITY", "Socket's listen() method failed", e);
        } catch (NullPointerException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(ReceiveKeyActivity.this, MainActivity.class);
                    intent.putExtra("BLUETOOTHERROR", "No Bluetooth, please connect your device via bluetooth");
                    startActivity(intent);
                }
            });
            performRun = false;
        }
        if (!performRun) {
            Intent intent = new Intent(ReceiveKeyActivity.this, MainActivity.class);
            intent.putExtra("BLUETOOTHERROR", "No Bluetooth, please connect your device via bluetooth");
            startActivity(intent);
        }
        bluetoothThread = new AcceptThread(tmp);
        bluetoothThread.start();
//        Button fakeSuccess = (Button) findViewById(R.id.fake_success);
//        fakeSuccess.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                saveIncomingKey("Saved File Name", "Key!");
//                Intent intent = new Intent(ReceiveKeyActivity.this, MainActivity.class);
//                startActivity(intent);
//            }
//        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("BLUETOOTHONDESTROY", "SENDING INTERRUPT");
        try {
            tmp.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveIncomingKey(String filename, byte[] contents) {
        File keysDir = getDir("keys", Context.MODE_PRIVATE);
        File keyFile = new File(keysDir, filename);
        try {
            keyFile.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(keyFile);
            outputStream.write(contents, 0 , contents.length);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int byteArrayToInt(byte[] b)
    {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private byte[] mmBuffer; // mmBuffer store for the stream
        private boolean performRun = true;

        public AcceptThread(BluetoothServerSocket tmp) {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.

            mmServerSocket = tmp;
        }

        public void run() {
            if (!performRun) {
                return;
            }
            BluetoothSocket socket = null;
            InputStream tmpIn = null;
            // Keep listening until exception occurs or a socket is returned.
            int port = 0;
            try {
                Field field = mmServerSocket.getClass().getDeclaredField("mSocket");
                field.setAccessible(true);
                BluetoothSocket hiddensocket = (BluetoothSocket) field.get(mmServerSocket);
                Method method = hiddensocket.getClass().getDeclaredMethod("getPort");
                method.setAccessible(true);
                port = (int) method.invoke(hiddensocket);
            } catch (Throwable e) {
                Log.e("BLUETOOTHSECURITY", "reflection fail", e);
            }
            final int finalPort = port;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView portDisplay = (TextView) findViewById(R.id.portNumReceive);
                    portDisplay.setText(String.valueOf(finalPort));
                }
            });
            Log.i("BLUETOOTHSECURITY", String.valueOf(port));
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                    tmpIn = socket.getInputStream();
                } catch (ClosedByInterruptException e) {
                    Log.i("BLUETOOTHCLOSING","Intterupted and closing");
                    try {
                        mmServerSocket.close();
                        return;
                    } catch (IOException ioe) {
                        e.printStackTrace();
                    }

                }
                catch (IOException e) {
                    Log.e("BLUETOOTHSECURITY", "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    //manageMyConnectedSocket(socket);
                    try{
                        mmBuffer = new byte[4];
                        tmpIn.read(mmBuffer);

                        int numBytes = byteArrayToInt(mmBuffer);
                        Log.i("BLUETOOTHSECURITY", "Byte array allocation successful " + String.valueOf(numBytes));
                        mmBuffer = new byte[numBytes];
                        tmpIn.read(mmBuffer);
                        String filename = new String(mmBuffer);
                        mmBuffer = new byte[4];
                        tmpIn.read(mmBuffer);
                        numBytes = byteArrayToInt(mmBuffer);
                        Log.i("BLUETOOTHSECURITY", "Key size " + String.valueOf(numBytes));
                        mmBuffer = new byte[numBytes];
                        tmpIn.read(mmBuffer);
                        saveIncomingKey(filename, mmBuffer);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(ReceiveKeyActivity.this, MainActivity.class);
                                startActivity(intent);
                            }
                        });
//                        cancel();

//                        Message readMsg = mHandler.obtainMessage(0, numBytes, -1, mmBuffer);
//                        readMsg.sendToTarget();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        socket.close();
                        mmServerSocket.close();

                    } catch (IOException e) {
                        Log.e("BLUETOOTHSECURITY", "Socket's close() method failed", e);
                    }
                    cancel();
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
//            try {
////                mmServerSocket.close();
//            } catch (IOException e) {
//                Log.e("BLUETOOTHSECURITY", "Could not close the connect socket", e);
//            }
        }
    }
}
