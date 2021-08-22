package de.lumabit.robot_controller;

import androidx.appcompat.app.AppCompatActivity;
import de.lumabit.robot_controller.motor.MotorService;
import de.lumabit.robot_controller.speech.SpeechActivity;
import io.github.controlwear.virtual.joystick.android.JoystickView;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import static android.companion.CompanionDeviceManager.*;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MyActivity";
    private Context context;
    private TextView console;
    private Character command = ' ';
    private MotorService motorService;

    private CompanionDeviceManager deviceManager;
    private AssociationRequest pairingRequest;
    private BluetoothDeviceFilter deviceFilter;

    private BluetoothSocket socket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private static final int SELECT_DEVICE_REQUEST_CODE = 42;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = context;
        setContentView(R.layout.activity_main);

        Intent serviceIntent = new Intent(this, MotorService.class);
        startService(serviceIntent);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

        console = (TextView) findViewById(R.id.text_console);
        Button connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(v -> { connectToRobot(); });

        Button speechButton = (Button) findViewById(R.id.speech);
        speechButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SpeechActivity.class);
            startActivity(intent);
        });

        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                motorService.setSpeed(strength, angle);
            }
        });


    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MotorService.LocalBinder binder = (MotorService.LocalBinder) service;
            motorService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };


    private void connectToRobot(){

        deviceManager = getSystemService(CompanionDeviceManager.class);

        // To skip filtering based on name and supported feature flags (UUIDs),
        // don't include calls to setNamePattern() and addServiceUuid(),
        // respectively. This example uses Bluetooth.
        deviceFilter = new BluetoothDeviceFilter.Builder()
                .setNamePattern(Pattern.compile("ESP32test"))
                //.addServiceUuid(new ParcelUuid(new UUID(0x123abcL, -1L)), null)
                .build();

        // The argument provided in setSingleDevice() determines whether a single
        // device name or a list of device names is presented to the user as
        // pairing options.
        pairingRequest = new AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .setSingleDevice(true)
                .build();

        // When the app tries to pair with the Bluetooth device, show the
        // appropriate pairing request dialog to the user.
        deviceManager.associate(pairingRequest,
                new Callback() {
                    @Override
                    public void onDeviceFound(IntentSender chooserLauncher) {
                        try {
                            startIntentSenderForResult(chooserLauncher,
                                    SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, e.getMessage());
                            Log.e(TAG, "exception: " + e.toString());
                        }
                    }

                    @Override
                    public void onFailure(CharSequence charSequence) {
                        Toast.makeText(context ,"Verbinden fehlgeschlagen.", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Fehler: " + charSequence.toString());
                    }
                },
                null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_DEVICE_REQUEST_CODE &&
                resultCode == Activity.RESULT_OK) {
            // User has chosen to pair with the Bluetooth device.
            BluetoothDevice deviceToPair = data.getParcelableExtra(EXTRA_DEVICE);
            deviceToPair.createBond();
            Log.e(TAG, "connected");

            // ... Continue interacting with the paired device.
            try {
                socket =  deviceToPair.createRfcommSocketToServiceRecord(MY_UUID);
                Toast.makeText(this ,"Connected " + socket.isConnected(), Toast.LENGTH_LONG).show();
                socket.connect();
                 motorService.setOutStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}