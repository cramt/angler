/*package com.angler.angler.angler

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.lang.reflect.Method
import java.util.UUID

class Test2 : AppCompatActivity() {

    // GUI Components
    private var mBluetoothStatus: TextView? = null
    private var mReadBuffer: TextView? = null
    private var mScanBtn: Button? = null
    private var mOffBtn: Button? = null
    private var mListPairedDevicesBtn: Button? = null
    private var mDiscoverBtn: Button? = null
    private var mBTAdapter: BluetoothAdapter? = null
    private var mPairedDevices: Set<BluetoothDevice>? = null
    private var mBTArrayAdapter: ArrayAdapter<String>? = null
    private var mDevicesListView: ListView? = null
    private var mLED1: CheckBox? = null

    private val TAG = Test2::class.java.simpleName
    private var mHandler: Handler? = null // Our main handler that will receive callback notifications
    private var mConnectedThread: ConnectedThread? = null // bluetooth background worker thread to send and receive data
    private var mBTSocket: BluetoothSocket? = null // bi-directional client-to-client data path

    internal val blReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // add the name to the list
                mBTArrayAdapter!!.add(device.name + "\n" + device.address)
                mBTArrayAdapter!!.notifyDataSetChanged()
            }
        }
    }

    private val mDeviceClickListener = AdapterView.OnItemClickListener { av, v, arg2, arg3 ->
        if (!mBTAdapter!!.isEnabled) {
            Toast.makeText(baseContext, "Bluetooth not on", Toast.LENGTH_SHORT).show()
            return@OnItemClickListener
        }

        mBluetoothStatus!!.text = "Connecting..."
        // Get the device MAC address, which is the last 17 chars in the View
        val info = (v as TextView).text.toString()
        val address = info.substring(info.length - 17)
        val name = info.substring(0, info.length - 17)

        // Spawn a new thread to avoid blocking the GUI one
        object : Thread() {
            override fun run() {
                var fail = false

                val device = mBTAdapter!!.getRemoteDevice(address)

                try {
                    mBTSocket = createBluetoothSocket(device)
                } catch (e: IOException) {
                    fail = true
                    Toast.makeText(baseContext, "Socket creation failed", Toast.LENGTH_SHORT).show()
                }

                // Establish the Bluetooth socket connection.
                try {
                    mBTSocket!!.connect()
                } catch (e: IOException) {
                    try {
                        fail = true
                        mBTSocket!!.close()
                        mHandler!!.obtainMessage(CONNECTING_STATUS, -1, -1)
                                .sendToTarget()
                    } catch (e2: IOException) {
                        //insert code to deal with this
                        Toast.makeText(baseContext, "Socket creation failed", Toast.LENGTH_SHORT).show()
                    }

                }

                if (!fail) {
                    mConnectedThread = ConnectedThread(mBTSocket!!)
                    mConnectedThread!!.start()

                    mHandler!!.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                            .sendToTarget()
                }
            }
        }.start()
    }


    @SuppressLint("HandlerLeak")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBluetoothStatus = findViewById(R.id.bluetoothStatus)
        mReadBuffer = findViewById(R.id.readBuffer)
        mScanBtn = findViewById(R.id.scan)
        mOffBtn = findViewById(R.id.off)
        mDiscoverBtn = findViewById(R.id.discover)
        mListPairedDevicesBtn = findViewById(R.id.PairedBtn)
        mLED1 = findViewById(R.id.checkboxLED1)

        mBTArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        mBTAdapter = BluetoothAdapter.getDefaultAdapter() // get a handle on the bluetooth radio

        mDevicesListView = findViewById(R.id.devicesListView) as ListView
        mDevicesListView!!.adapter = mBTArrayAdapter // assign model to view
        mDevicesListView!!.onItemClickListener = mDeviceClickListener

        // Ask for location permission if not already allowed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)


        mHandler = object : Handler() {
            override fun handleMessage(msg: android.os.Message) {
                if (msg.what == MESSAGE_READ) {
                    var readMessage: String? = null
                    try {
                        readMessage = String(msg.obj as ByteArray, "UTF-8")
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }

                    mReadBuffer!!.text = readMessage
                }

                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1)
                        mBluetoothStatus!!.text = "Connected to Device: " + msg.obj as String
                    else
                        mBluetoothStatus!!.text = "Connection Failed"
                }
            }
        }

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus!!.text = "Status: Bluetooth not found"
            Toast.makeText(applicationContext, "Bluetooth device not found!", Toast.LENGTH_SHORT).show()
        } else {

            mLED1!!.setOnClickListener {
                if (mConnectedThread != null) {
                    if (mLED1!!.isChecked) {
                        mConnectedThread!!.write("1")
                    } else {
                        mConnectedThread!!.write("0")
                    }
                }
            }


            mScanBtn!!.setOnClickListener { bluetoothOn() }

            mOffBtn!!.setOnClickListener { bluetoothOff() }

            mListPairedDevicesBtn!!.setOnClickListener { v -> listPairedDevices(v) }

            mDiscoverBtn!!.setOnClickListener { discover() }
        }
    }

    private fun bluetoothOn() {
        if (!mBTAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            mBluetoothStatus!!.text = "Bluetooth enabled"
            Toast.makeText(applicationContext, "Bluetooth turned on", Toast.LENGTH_SHORT).show()

        } else {
            Toast.makeText(applicationContext, "Bluetooth is already on", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bluetoothOff() {
        mBTAdapter!!.disable() // turn off
        mBluetoothStatus!!.text = "Bluetooth disabled"
        Toast.makeText(applicationContext, "Bluetooth turned Off", Toast.LENGTH_SHORT).show()
    }

    private fun discover() {
        // Check if the device is already discovering
        if (mBTAdapter!!.isDiscovering) {
            mBTAdapter!!.cancelDiscovery()
            Toast.makeText(applicationContext, "Discovery stopped", Toast.LENGTH_SHORT).show()
        } else {
            if (mBTAdapter!!.isEnabled) {
                mBTArrayAdapter!!.clear() // clear items
                mBTAdapter!!.startDiscovery()
                Toast.makeText(applicationContext, "Discovery started", Toast.LENGTH_SHORT).show()
                registerReceiver(blReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            } else {
                Toast.makeText(applicationContext, "Bluetooth not on", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun listPairedDevices(view: View) {
        mPairedDevices = mBTAdapter!!.bondedDevices
        if (mBTAdapter!!.isEnabled) {
            // put it's one to the adapter
            for (device in mPairedDevices!!)
                mBTArrayAdapter!!.add(device.name + "\n" + device.address)

            Toast.makeText(applicationContext, "Show Paired Devices", Toast.LENGTH_SHORT).show()
        } else
            Toast.makeText(applicationContext, "Bluetooth not on", Toast.LENGTH_SHORT).show()
    }

    @Throws(IOException::class)
    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        try {
            val m = device.javaClass.getMethod("createInsecureRfcommSocketToServiceRecord", UUID::class.java)
            return m.invoke(device, BTMODULEUUID) as BluetoothSocket
        } catch (e: Exception) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e)
        }

        return device.createRfcommSocketToServiceRecord(BTMODULEUUID)
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            var buffer = ByteArray(1024)  // buffer store for the stream
            var bytes: Int // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream!!.available()
                    if (bytes != 0) {
                        buffer = ByteArray(1024)
                        SystemClock.sleep(100) //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available() // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes) // record how many bytes we actually read
                        mHandler!!.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget() // Send the obtained bytes to the UI activity
                    }
                } catch (e: IOException) {
                    e.printStackTrace()

                    break
                }

            }
        }

        /* Call this from the main activity to send data to the remote device */
        fun write(input: String) {
            val bytes = input.toByteArray()           //converts entered String into bytes
            try {
                mmOutStream!!.write(bytes)
            } catch (e: IOException) {
            }

        }

        /* Call this from the main activity to shutdown the connection */
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
            }

        }
    }

    companion object {

        private val BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // "random" unique identifier


        // #defines for identifying shared types between calling functions
        private val REQUEST_ENABLE_BT = 1 // used to identify adding bluetooth names
        private val MESSAGE_READ = 2 // used in bluetooth handler to identify message update
        private val CONNECTING_STATUS = 3 // used in bluetooth handler to identify message status
    }


}*/