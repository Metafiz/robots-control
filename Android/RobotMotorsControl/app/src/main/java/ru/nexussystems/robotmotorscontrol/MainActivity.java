package ru.nexussystems.robotmotorscontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";
    private final static int REQUEST_ENABLE_BT = 101;

    private EditText etLog;
    private TextView tvInfo;
    private ArrayList<String> alDeviceList = new ArrayList<>();

    // SPP UUID сервиса
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-адрес Bluetooth модуля
    static final String BTDEV_MAC = "30:14:11:21:23:34";

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;

    // sensor
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private float xy_angle;
    private float xz_angle;
    private float zy_angle;

    private void addToLog(String s) {
        //etLog.setText(etLog.getText().toString() + ' ' + s);
        etLog.setText(s);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //disconnect(null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etLog = (EditText) findViewById(R.id.etLog);
        tvInfo = (TextView) findViewById(R.id.tvInfo);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();
        //connect(null);

        /*IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        IntentFilter filter2=new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver, filter2);

        chooseDevice(null);*/

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // Получаем менеджер сенсоров
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION); // Получаем датчик положения
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth не поддерживается");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth включен...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private final BroadcastReceiver mReceiver=new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            String action= intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device= intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                alDeviceList.add(device.getName()+":"+ device.getAddress());
                addToLog(device.getName()+":"+ device.getAddress());
                /*if (device.getAddress().equals(BTDEV_MAC)) {
                    Toast.makeText(getApplicationContext(), "Найдено устройство: " + device.getName(), Toast.LENGTH_LONG).show();
                    device.createBond();
                }*/
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                String state = intent.getParcelableExtra(BluetoothDevice.EXTRA_BOND_STATE);
                addToLog("Bond state: " + state);
            }
        }
    };

    public void chooseDevice(View v) {
        //tvInfo.setText(tvInfo.getText() + "\n" + "showPairedDevices: ");

        Set<BluetoothDevice> pairedDevices= btAdapter.getBondedDevices();
        if(pairedDevices.size()>0) {
            for(BluetoothDevice device: pairedDevices){
                addToLog(device.getName()+":"+ device.getAddress());
                /*if (device.getAddress().equals(BTDEV_MAC)) {
                    Toast.makeText(getApplicationContext(), "Найдено устройство: " + device.getName(), Toast.LENGTH_SHORT).show();
                    targetDevice = device;
                    connectToTargetDevice();
                }*/
            }
        }
    }


    public void connect(View v) {
        Log.d(TAG, "...onResume - попытка соединения...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(BTDEV_MAC);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);

            // Discovery is resource intensive.  Make sure it isn't going on
            // when you attempt to connect and pass your message.
            btAdapter.cancelDiscovery();

            // Establish the connection.  This will block until it connects.
            Log.d(TAG, "...Соединяемся...");
            try {
                btSocket.connect();
                outStream = btSocket.getOutputStream();
                Log.d(TAG, "...Соединение установлено и готово к передачи данных...");

                Toast.makeText(getBaseContext(), "Соединение установлено", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                }
            }

        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }
    }

    public boolean isConnected() {
        boolean res = false;
        if (btSocket != null) res = btSocket.isConnected();
        return res;
    }

    public void disconnect(View v) {
        Log.d(TAG, "...In onPause()...");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    public void sendCmd(String cmd) {
        //new SensorDataReceiver().execute("");

        try {

            //inStream = btSocket.getInputStream();

            outStream.write(cmd.getBytes());
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }

        //String r = sendCmdAndGetResponse(etCmd.getText().toString());
        //addToLog(r);
    }

    public void move(View v) {
        int speed = Integer.parseInt(((EditText)findViewById(R.id.etSpeed)).getText().toString());
        switch (v.getId()) {
            case R.id.btForw:
                sendCmd("f" + speed);
                break;
            case R.id.btStop:
                sendCmd("s");
                break;
            case R.id.btRew:
                sendCmd("b" + speed);
                break;
        }
    }

    private int convertAngleToSpeed(int angle) {
        int speed = 0;
        speed = (int)Math.round(0.1 * angle);
        /*if (angle > 0 && angle <= 9) speed = 0;
        else if (angle >= 10 && angle < 20) speed = 1;
        else if (angle >= 20 && angle < 30) speed = 2;
        else if (angle >= 30 && angle < 40) speed = 3;
        else if (angle >= 40 && angle < 50) speed = 4;
        else if (angle >= 50 && angle < 60) speed = 5;
        else if (angle >= 60 && angle < 70) speed = 6;
        else if (angle >= 70 && angle < 80) speed = 7;
        else if (angle >= 80 && angle < 90) speed = 8;
        //else if (angle > 0 && angle < 9) speed = 9;*/
        return speed;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        xy_angle = Math.round(event.values[0]); //Плоскость XY
        xz_angle = Math.round(event.values[1]); //Плоскость XZ
        zy_angle = Math.round(event.values[2]); //Плоскость ZY
        int speed = -1, oldSpeed;

        tvInfo.setText("xy = " + xy_angle + ", xz = " + xz_angle + ", zy = " + zy_angle);
        // вперёд-назад: ZY, перёд от 0 до -90, назад - от 0 до 90
        // влево-вправо: XZ. Влево: от 0 до 90, вправо - от 0 до -90

        if (isConnected()) {
            String cmd;
            oldSpeed = speed;
            if (zy_angle < -10 && zy_angle > -90) {
                speed = convertAngleToSpeed(Math.round(-zy_angle));
                if (speed != oldSpeed) sendCmd("f" + speed);
            } else if (zy_angle > 10 && zy_angle < 90) {
                speed = convertAngleToSpeed(Math.round(zy_angle));
                if (speed != oldSpeed) sendCmd("b" + speed);
            } else {
                sendCmd("s");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
