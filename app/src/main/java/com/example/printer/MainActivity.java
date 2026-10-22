package com.example.printer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final UUID SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private EditText input;
    private CheckBox cutBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        input = new EditText(this);
        input.setHint("Print செய்ய வேண்டிய text");
        input.setMinLines(5);
        input.setText("MY SHOP\n----------------\nItem 1      Rs.50\nItem 2      Rs.30\n----------------\nTotal       Rs.80\n\nThank you!");
        layout.addView(input);

        cutBox = new CheckBox(this);
        cutBox.setText("Paper cut செய்");
        cutBox.setChecked(true);
        layout.addView(cutBox);

        Button btn = new Button(this);
        btn.setText("Printer தேர்ந்தெடுத்து Print செய்");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askPermissionAndPrint();
            }
        });
        layout.addView(btn);

        setContentView(layout);
        handleShare(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleShare(intent);
    }

    private void handleShare(Intent intent) {
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null) {
                input.setText(shared);
            }
        }
    }

    private void askPermissionAndPrint() {
        if (Build.VERSION.SDK_INT >= 31
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return;
        }
        chooseAndPrint();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            chooseAndPrint();
        } else {
            Toast.makeText(this, "Bluetooth permission தேவை", Toast.LENGTH_LONG).show();
        }
    }

    private void chooseAndPrint() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth ON செய்யுங்க", Toast.LENGTH_LONG).show();
            return;
        }
        final ArrayList<BluetoothDevice> devices = new ArrayList<BluetoothDevice>(adapter.getBondedDevices());
        if (devices.isEmpty()) {
            Toast.makeText(this, "முதலில் printer-ஐ Bluetooth-ல் pair செய்யுங்க", Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            names[i] = devices.get(i).getName();
        }
        new AlertDialog.Builder(this)
                .setTitle("Printer தேர்ந்தெடுங்க")
                .setItems(names, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        printTo(devices.get(which));
                    }
                })
                .show();
    }

    private void printTo(final BluetoothDevice device) {
        final String text = input.getText().toString();
        final boolean cut = cutBox.isChecked();
        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothSocket socket = null;
                try {
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                    socket.connect();
                    OutputStream out = socket.getOutputStream();
                    out.write(new byte[]{0x1B, 0x40});
                    out.write((text + "\n\n\n\n").getBytes("UTF-8"));
                    if (cut) {
                        out.write(new byte[]{0x1D, 0x56, 0x42, 0x00});
                    }
                    out.flush();
                    Thread.sleep(1500);
                    toast("Print அனுப்பப்பட்டது");
                } catch (Exception e) {
                    toast("Error: " + e.getMessage());
                } finally {
                    try { if (socket != null) socket.close(); } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
