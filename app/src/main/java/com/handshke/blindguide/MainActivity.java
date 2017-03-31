package com.handshke.blindguide;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;
import java.io.InputStream;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity
{
    public BluetoothAdapter bth;
    public OutputStream out;
    public InputStream in;
    private static final int ENABLE_BT_REQUEST_CODE = 1;
    private static final int REQUEST_ENABLE_BT=1;
    public Button sel_device, disconnect;
    private final static UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ArrayAdapter<String>bt_array_adapter;
    private Set<BluetoothDevice>paired_devices;
    public ListView bt_list;
    int initial,middle,finish;
    public BluetoothDevice remote_device;
    public TextView temp_view, hum_view, temp_val, hum_val, distance;
    public String item_value;
    int temp,hum;
    byte readBuffer[] = new byte[1024];
    int readBufferPosition = 0;
    boolean stopWorker;
    String msg="@@@@@@@",tempstr,humstr;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        temp_view=(TextView)findViewById(R.id.temp_view);
        temp_val=(TextView)findViewById(R.id.temp_val);
        hum_val=(TextView)findViewById(R.id.hum_val);
        hum_view=(TextView)findViewById(R.id.hum_view);
        distance = (TextView)findViewById(R.id.distance);
        sel_device=(Button)findViewById(R.id.sel_device);
        bt_list=(ListView)findViewById(R.id.devices);
        disconnect=(Button)findViewById(R.id.disconnect);
        bth=BluetoothAdapter.getDefaultAdapter();
        bt_array_adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        bt_list.setAdapter(bt_array_adapter);
        if(bth==null)
        {
            Toaster("Bluetooth is not supported by your device");
        }

        SwitchOn(bth);
        sel_device.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                if(!bth.isEnabled())
                {
                    SwitchOn(bth);
                }
                if(bth.isEnabled())
                {
                    try
                    {
                        get_paired(v);
                        sel_device.setVisibility(View.GONE);
                        bt_list.setVisibility(View.VISIBLE);
                    }
                    catch(Exception e)
                    {
                        Toaster(e.toString());
                    }
                }
            }
        });
        bt_list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?>parent, View view, int position,long id)
            {
                item_value=(String)bt_list.getItemAtPosition(position);
                String MAC=item_value.substring(item_value.length()-17);
                remote_device=bth.getRemoteDevice(MAC);
                Toaster("Connecting to  :\n"+item_value);
                ConnectingThread t=new ConnectingThread(remote_device);
                t.start();
                bt_list.setVisibility(View.GONE);
            }
        });
        disconnect.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                Disconnect(remote_device);
                disconnect.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public void ConnectedView()
    {
        try
        {
            //bt_list.setVisibility(View.GONE);
            //temp_view.setVisibility(View.VISIBLE);
            //temp_val.setVisibility(View.VISIBLE);
            //hum_view.setVisibility(View.VISIBLE);
            //hum_val.setVisibility(View.VISIBLE);
            distance.setVisibility(View.VISIBLE);
            disconnect.setVisibility(View.VISIBLE);
        }
        catch(Exception e)
        {
            Toaster(e.toString());
        }
        //temp_val.setText(tempstr);
        //hum_val.setText(humstr);
    }

    public void Toaster(final String t)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(MainActivity.this, t, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void SwitchOn(BluetoothAdapter ba)
    {
        ba=bth;
        if(!ba.isEnabled())
        {
            Intent turnOn=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn,REQUEST_ENABLE_BT);
        }
        else
        {
            Toaster("Bluetooth already Enabled");
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == ENABLE_BT_REQUEST_CODE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                Toaster("Bluetooth successfully Enabled ");
            }
            if(resultCode==Activity.RESULT_CANCELED)
            {
                Toaster("Please Enable Bluetooth to use this app");
            }
        }
    }

    public class ConnectingThread extends Thread
    {
        public final BluetoothSocket bluetoothSocket;
        public final BluetoothDevice bluetoothDevice;
        public ConnectingThread(BluetoothDevice device)
        {
            BluetoothSocket temp=null;
            bluetoothDevice=device;
            try
            {
                temp=bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            }
            catch(IOException e)
            {
                MainActivity.this.runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        //Toast.makeText(MainActivity.this, "Unable to Inatialize Bluetooth connection", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            bluetoothSocket=temp;
        }
        public void run()
        {
            bth.cancelDiscovery();
            try
            {
                bluetoothSocket.connect();
                out=bluetoothSocket.getOutputStream();
                in=bluetoothSocket.getInputStream();
                MainActivity.this.runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        Toast.makeText(MainActivity.this, "Connection Successful", Toast.LENGTH_SHORT).show();
                        ConnectedView();
                        try
                        {
                            Thread.sleep(5000);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                        ReceivingThread r=new ReceivingThread(in);
                        r.start();
                    }
                });
            }
            catch(IOException e)
            {
                MainActivity.this.runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        Toast.makeText(MainActivity.this, "Unable to connect !\nCheck that the device is turned ON", Toast.LENGTH_SHORT).show();
                        ReInitialize();
                    }
                });
            }
        }
        public void cancel()
        {
            try
            {
                out.close();
                in.close();
                bluetoothSocket.close();
            }
            catch(IOException e)
            {
                //Toaster("Connection Termination Unsuccessful");
            }
        }
    }
    public class ReceivingThread extends Thread
    {
        byte buffer[]=new byte[1024];
        int bytes;
        public InputStream inputStream;
        public ReceivingThread(InputStream inp)
        {
            inputStream=inp;
        }
        public void run()
        {
            while(true)
            {
                if (inputStream!=null)
                {
                    try
                    {
                        bytes = inputStream.read(buffer);
                    }
                    catch(IOException e)
                    {}
                    msg = new String(buffer, 0, bytes);
                    msg=msg.trim();
                    /*int len=msg.length();
                    if(!msg.equals("@"))
                    {
                        initial=msg.indexOf("*");
                        middle=msg.indexOf(",");
                        finish=msg.indexOf("!");
                        tempstr=msg.substring((initial+1),(middle));
                        humstr=msg.substring((middle+1),(finish));
                    }*/
                    runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            //temp_val.setText(tempstr);
                            //hum_val.setText(humstr);
                            distance.setText(msg);
                            //distance.setText("HELLO");
                        }
                    });
                }
                else
                {
                    Toaster("Input Stream Not Initialized");
                }

                //hum=Integer.parseInt(humstr);
                try
                {
                    Thread.sleep(150);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
    public void get_paired(View view)
    {
        paired_devices=bth.getBondedDevices();
        for(BluetoothDevice device:paired_devices)
        {
            bt_array_adapter.add(device.getName()+"\n"+device.getAddress());
        }
        Toaster("List of Paired Devices");
    }
    public void Disconnect(BluetoothDevice dev)
    {
        try
        {
            ConnectingThread disc=new ConnectingThread(dev);
            disc.cancel();
            ReInitialize();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    public void ReInitialize()
    {
        try
        {
            disconnect.setVisibility(View.GONE);
            distance.setVisibility(View.GONE);
            //temp_view.setVisibility(View.GONE);
            //temp_val.setVisibility(View.GONE);
            //hum_view.setVisibility(View.GONE);
            //hum_val.setVisibility(View.GONE);
            sel_device.setVisibility(View.VISIBLE);
        }
        catch(Exception e)
        {
            Toaster(e.toString());
        }
    }
}
