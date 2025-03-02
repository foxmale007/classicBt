package io.dcloud.uniplugin;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * 经典蓝牙模块单元
 * 1)适配安卓12+
 * 2)增加解除配对动作
 * @author Jim 2025/02/24
 */

public class BluetoothModule extends UniModule {
    private UniJSCallback discoveryCallback;

    private UniJSCallback readCallback;

    private UniJSCallback permissionCallback;

    private UniJSCallback btConnectionCallback;

    String TAG = "BluetoothModule";

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothSocket bluetoothSocket;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    @UniJSMethod(uiThread = true)
    public void enableBluetooth(UniJSCallback callback) {


// 检查 BluetoothAdapter
        if (this.bluetoothAdapter == null) {
            Toast.makeText(this.mUniSDKInstance.getContext(), "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }

        this.permissionCallback = callback;

        // 检查和请求权限
        Activity activity = (Activity) this.mUniSDKInstance.getContext();
        String[] permissions = {
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (!hasPermissions(activity, permissions)) {
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_PERMISSIONS);
            return;
        }

        // 检查蓝牙是否启用
        if (!this.bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            // 蓝牙已启用，返回结果
            JSONObject data = new JSONObject();
            data.put("msg", "蓝牙已启用");
            data.put("success", true);
            callback.invoke(data);
        }
    }

    private boolean hasPermissions(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @UniJSMethod(uiThread = true)
    public void startBluetoothDiscovery(UniJSCallback callback) {
        this.discoveryCallback = callback;
        if (this.bluetoothAdapter == null) {
            Toast.makeText(this.mUniSDKInstance.getContext(), "", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!this.bluetoothAdapter.isEnabled()) {
            JSONObject data = new JSONObject();
            data.put("success", Boolean.valueOf(false));
            data.put("msg", "");
            this.discoveryCallback.invokeAndKeepAlive(data);
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.FOUND");
        intentFilter.addAction("android.bluetooth.adapter.action.DISCOVERY_STARTED");
        intentFilter.addAction("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
        this.mUniSDKInstance.getContext().registerReceiver(this.btDiscoveryReceiver, intentFilter);
        this.bluetoothAdapter.startDiscovery();
    }

    @UniJSMethod(uiThread = true)
    public void stopBluetoothDiscovery(UniJSCallback uniJSCallback) {
        this.bluetoothAdapter.cancelDiscovery();
        this.mUniSDKInstance.getContext().unregisterReceiver(this.btDiscoveryReceiver);
        JSONObject data = new JSONObject();
        data.put("msg", "");
        data.put("success", Boolean.valueOf(true));
        uniJSCallback.invoke(data);
    }

    @UniJSMethod(uiThread = true)
    public void getPairedDevices(UniJSCallback uniJSCallback) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        JSONArray pairedDevicesArray = new JSONArray();
        if (bluetoothAdapter != null)
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                JSONObject deviceJson = new JSONObject();
                deviceJson.put("name", device.getName());
                deviceJson.put("address", device.getAddress());
                pairedDevicesArray.add(deviceJson);
            }
        JSONObject data = new JSONObject();
        data.put("devices", pairedDevicesArray);
        data.put("msg", "");
        data.put("success", Boolean.valueOf(true));
        uniJSCallback.invoke(data);
    }

    @UniJSMethod(uiThread = true)
    public void connectBluetooth(String deviceAddress, UniJSCallback callback, UniJSCallback callback2) {
        try {
            this.readCallback = callback2;
            if (this.bluetoothSocket == null || !Objects.equals(this.bluetoothSocket.getRemoteDevice().getAddress(), deviceAddress) || !this.bluetoothSocket.isConnected()) {
                BluetoothDevice device = this.bluetoothAdapter.getRemoteDevice(deviceAddress);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    boolean pairingStarted = false;
                    if (Build.VERSION.SDK_INT >= 19)
                        pairingStarted = device.createBond();
                } else {
                    Log.i(this.TAG, "connectBT:");
                }
                this.bluetoothSocket = device.createRfcommSocketToServiceRecord(this.uuid);
                this.bluetoothSocket.connect();
                startListeningForData();
            }
            JSONObject data = new JSONObject();
            data.put("msg", "");
            data.put("success", Boolean.valueOf(true));
            callback.invoke(data);
        } catch (IOException e) {
            this.bluetoothSocket = null;
            JSONObject data = new JSONObject();
            data.put("msg", "");
            data.put("success", Boolean.valueOf(false));
            callback.invoke(data);
            e.printStackTrace();
        }
    }

    @UniJSMethod(uiThread = true)
    public void bluetoothStatusChange(UniJSCallback callback) {
        this.btConnectionCallback = callback;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        filter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        this.mUniSDKInstance.getContext().registerReceiver(this.bluetoothConnectionReceiver, filter);
    }

    @UniJSMethod(uiThread = true)
    public void writeHexData(final String hexString, final UniJSCallback uniJSCallback) {
        (new Thread(new Runnable() {
            public void run() {
                try {
                    OutputStream outputStream = BluetoothModule.this.bluetoothSocket.getOutputStream();
                    byte[] hexBytes = BluetoothModule.this.hexStringToByteArray(hexString);
                    outputStream.write(hexBytes);
                    outputStream.flush();
                    JSONObject data = new JSONObject();
                    data.put("msg", "");
                    data.put("success", Boolean.valueOf(true));
                    uniJSCallback.invoke(data);
                } catch (IOException e) {
                    JSONObject data = new JSONObject();
                    data.put("msg", "");
                    data.put("success", Boolean.valueOf(false));
                    uniJSCallback.invoke(data);
                    Log.e(BluetoothModule.this.TAG, "run");
                }
            }
        })).start();
    }

    @UniJSMethod(uiThread = true)
    public void disconnectBluetooth(UniJSCallback uniJSCallback) {
        try {
            this.bluetoothSocket.close();
            this.mUniSDKInstance.getContext().unregisterReceiver(this.bluetoothConnectionReceiver);
            JSONObject data = new JSONObject();
            data.put("msg", "");
            data.put("success", Boolean.valueOf(true));
            uniJSCallback.invoke(data);
            JSONObject data2 = new JSONObject();
            data2.put("msg", "");
            data2.put("success", Boolean.valueOf(false));
            this.btConnectionCallback.invokeAndKeepAlive(data2);
        } catch (IOException e) {
            JSONObject data = new JSONObject();
            data.put("msg", e.toString());
            data.put("success", Boolean.valueOf(false));
            uniJSCallback.invoke(data);
        }
    }

    @UniJSMethod(uiThread = true)
    public void unpairDevice(String deviceAddress, UniJSCallback uniJSCallback) {
        try {
            if(this.bluetoothSocket != null && this.bluetoothSocket.isConnected()){
                this.bluetoothSocket.close();
            }

            BluetoothDevice device = this.bluetoothAdapter.getRemoteDevice(deviceAddress);
            Method method = device.getClass().getMethod("removeBond");
            boolean removeResult = (boolean) method.invoke(device);
            JSONObject data = new JSONObject();
            data.put("msg", "");
            data.put("success", Boolean.valueOf(removeResult));
            uniJSCallback.invoke(data);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 IOException e) {
            JSONObject data = new JSONObject();
            data.put("msg", e.toString());
            data.put("success", Boolean.valueOf(false));
            uniJSCallback.invoke(data);
        }
    }

    private final BroadcastReceiver bluetoothConnectionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.device.action.ACL_CONNECTED".equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                JSONObject data = new JSONObject();
                data.put("msg", "");
                data.put("address", device.getAddress());
                data.put("success", Boolean.valueOf(true));
                BluetoothModule.this.btConnectionCallback.invokeAndKeepAlive(data);
            } else if ("android.bluetooth.device.action.ACL_DISCONNECTED".equals(action)) {
                BluetoothModule.this.bluetoothSocket = null;
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                JSONObject data = new JSONObject();
                data.put("msg", "");
                data.put("address", device.getAddress());
                data.put("success", Boolean.valueOf(false));
                BluetoothModule.this.btConnectionCallback.invokeAndKeepAlive(data);
            }
        }
    };

    private final BroadcastReceiver btDiscoveryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.device.action.FOUND".equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                JSONObject data = new JSONObject();
                data.put("success", Boolean.valueOf(true));
                data.put("name", device.getName());
                data.put("address", device.getAddress());
                int rssi = intent.getIntExtra(BluetoothDevice.EXTRA_RSSI, Integer.MIN_VALUE);
                data.put("rssi", rssi);
                BluetoothModule.this.discoveryCallback.invokeAndKeepAlive(data);
            } else if ("android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(action)) {
                BluetoothModule.this.bluetoothAdapter.startDiscovery();
            }
        }
    };

    private void startListeningForData() {
        (new Thread(new Runnable() {
            public void run() {
                try {
                    InputStream inputStream = BluetoothModule.this.bluetoothSocket.getInputStream();
                    byte[] buffer = new byte[1024];
                    while (true) {
                        int bytesRead = inputStream.read(buffer);
                        String receivedMessage = BluetoothModule.this.bytesToHexString(Arrays.copyOf(buffer, bytesRead));
                        JSONObject data = new JSONObject();
                        data.put("msg", receivedMessage);
                        data.put("success", Boolean.valueOf(true));
                        BluetoothModule.this.readCallback.invokeAndKeepAlive(data);
                        Log.i(BluetoothModule.this.TAG, receivedMessage);
                    }
                } catch (IOException e) {
                    JSONObject data = new JSONObject();
                    data.put("msg", "");
                    data.put("success", Boolean.valueOf(false));
                    BluetoothModule.this.readCallback.invokeAndKeepAlive(data);
                    return;
                }
            }
        })).start();
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            stringBuilder.append(String.format("%02X ", new Object[]{Byte.valueOf(b)}));
        }
        return stringBuilder.toString().trim();
    }

    private byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            data[i / 2] =
                    (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
        return data;
    }


    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT)
            if (resultCode == -1) {
                Log.d(this.TAG, "onActivityResult: ");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("msg", "");
                jsonObject.put("success", Boolean.valueOf(true));
                this.permissionCallback.invoke(jsonObject);
            } else {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("msg", "");
                jsonObject.put("success", Boolean.valueOf(false));
                this.permissionCallback.invoke(jsonObject);
            }
    }
}
