package com.example.touristapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    private Button bluetoothOnBtn;
    private Button bluetoothOffBtn;
    private ImageView imageView;
    private Button scanButton;
    private Button sendButton;
    private TextView pub_box;
    StringBuffer nearbyDevicesString = new StringBuffer();
    // ArrayList<String> nearbyDevices = new ArrayList<>();
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    // MQTT SETTINGS
    private MqttAndroidClient client;
    private static final String SERVER_URI = "tcp://192.168.0.81:1883";
    private static final String TAG = "MainActivity";
    private final String PUB_TOPIC = "bluetooth";
    private String clientId;




    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    // ON CREATE
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int REQUEST_ENABLE_BT = 1;
        bluetoothOnBtn = findViewById(R.id.on_bt);
        bluetoothOffBtn = findViewById(R.id.off_btn);
        imageView = findViewById(R.id.imageView3);
        //scanButton = findViewById(R.id.scan_bt);
        //sendButton = findViewById(R.id.send_bt);
        pub_box = findViewById(R.id.publish_box);
        connect();
        start();






        // Callback for MQTT
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    System.out.println("Reconnected to : " + serverURI);
                    // Re-subscribe as we lost it due to new session
                    subscribe(" YOUR-TOPIC");
                } else {
                    System.out.println("Connected to: " + serverURI);
                    subscribe(" YOUR-TOPIC");
                }
            }
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("The Connection was lost.");
            }
            @Override
            public void messageArrived(String topic, MqttMessage message) throws
                    Exception {
                String newMessage = new String(message.getPayload());
                System.out.println("Incoming message: " + newMessage);
                /* add code here to interact with elements
                (text views, buttons)
                using data from newMessage
                */
                String[] messArray = newMessage.split(" ", 2);
                int id = Integer.parseInt(messArray[0]);
                if(id == 1) {
                    imageView.setImageResource(R.drawable.gustav);
                } else if(id == 2) {
                    imageView.setImageResource(R.drawable.skepps);
                } else if(id == 3) {
                imageView.setImageResource(R.drawable.globen1);
            }

                pub_box.setText(messArray[1]);
                // set imageview. source
               // imageView.setImageResource()


            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });




        // Code asking for permission to use location data on phone.
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permissionCheck != 0) {
            System.out.println("Permission was not good, adding permission.");
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
        }




        // Ask to turn on bluetooth on app start
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        // Bluetooth ON BUTTON
        bluetoothOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        });

        // Bluetooth OFF BUTTON
        bluetoothOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothAdapter.disable();
            }
        });


        // discover reciever?
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);


    }

    public void start() {
        Timer t = new Timer();

        TimerTask t1 = new TimerTask() {
            @Override
            public void run() {
                int n = 1;
                scan();
                System.out.println(n + " varv i loopen");
                n++;
            }
        };
        //t.schedule(t1,12000,30000);
        t.schedule(t1,12000,15000);



        TimerTask t2 = new TimerTask() {
            @Override
            public void run() {
                int n = 1;
                sendData();
                System.out.println("data har skickats " + n + " antal gånger");
                n++;
            }
        };
        t.schedule(t2,23000,10000);
    }






    // SEND DATA METHOD
    private void sendData() {
        String deviceList = nearbyDevicesString.toString();
        try {
            subscribe(clientId);        // Här subscribar vi till topic "vår klient-id". För att senare kunna få ett svar från servern.
            String id = client.getClientId();
            //System.out.println(id);
            System.out.println("Sending data: " + deviceList + " to server");
            publishMqttMessage(id + " " + deviceList);
            System.out.println("clientID är: " + clientId);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Problem med konvertera Buffer till sträng");
        }
    }


    private void scan() {
        nearbyDevicesString.delete(0, nearbyDevicesString.length());
        bluetoothAdapter.startDiscovery();
        System.out.print("Scan startas!!!!!!!!!!!!!!\n\n");
    }

    // MQTT CONNECT
    private void connect(){
        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), SERVER_URI, clientId);
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "onSuccess");
                    System.out.println(TAG + "Success. Connected to" + SERVER_URI);
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception)
                {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(TAG, "onFailure");
                    System.out.println(TAG + "Oh no! Failed to connect to" +
                        SERVER_URI);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    // MQTT SUBSCRIBE
    private void subscribe(String topicToSubscribe) {
        final String topic = topicToSubscribe;
        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    System.out.println("Subscription successful to topic: " + topic);
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    System.out.println("Failed to subscribe to topic: " + topic);
                // The subscription could not be performed, maybe the user was not
                // authorized to subscribe on the specified topic e.g. using wildcards
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }



    private void publishMqttMessage(String message) throws Exception {
        MqttMessage msg = createMqttTopic(message);
        client.publish(PUB_TOPIC, msg);
    }

    private MqttMessage createMqttTopic(String message) {
        byte[] payload = String.format(message).getBytes();
        return new MqttMessage(payload);
    }









    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                System.out.println("DEVICE NAME: " + deviceName);
                System.out.println("MAC ADDRESS: " + deviceHardwareAddress + "\n");
                System.out.println("           ");
                nearbyDevicesString.append(deviceHardwareAddress + ",");



            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

            }

        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }












}












