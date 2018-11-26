package com.angler.angler.angler

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_bluetooth_communication.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*
import kotlin.math.roundToInt

class BluetoothCommunicationActivity : AppCompatActivity() {
    companion object {
        private val BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // "random" unique identifier
        // #defines for identifying shared types between calling functions
        private val REQUEST_ENABLE_BT = 1 // used to identify adding bluetooth names
        private val MESSAGE_READ = 2 // used in bluetooth handler to identify message update
        private val CONNECTING_STATUS = 3 // used in bluetooth handler to identify message status
    }

    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var mHandler: Handler? = null // Our main handler that will receive callback notifications
    private var mConnectedThread: BluetoothCommunicationActivity.ConnectedThread? = null // bluetooth background worker thread to send and receive data
    private var connected = false
    private var currentInput: Double? = null
    private val savedDataList = mutableListOf<Double>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_communication)
        val macAddress = intent.getStringExtra("mac_address")
        if (macAddress === "") {
            finishActivity(0)
            return
        }
        println(macAddress)

        mHandler = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: android.os.Message) {
                if (msg.what == BluetoothCommunicationActivity.MESSAGE_READ) {
                    var readMessage: String? = null
                    try {
                        readMessage = String(msg.obj as ByteArray, Charset.defaultCharset())
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                    runOnUiThread {
                        if (readMessage != null) {
                            currentInput = parseBluetoothInput(readMessage)
                            txtAngleStatus.text = currentInput!!.roundToInt().toString() + "°"
                        }
                    }

                }
                if (msg.what == BluetoothCommunicationActivity.CONNECTING_STATUS) {
                    if (msg.arg1 == 1) {
                        runOnUiThread {
                            Toast.makeText(this@BluetoothCommunicationActivity, "Connected to Device: " + msg.obj as String, Toast.LENGTH_LONG).show()
                        }
                        connected = true
                        mConnectedThread!!.write("1")
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@BluetoothCommunicationActivity, "Connection failed", Toast.LENGTH_LONG).show()
                        }
                        finish()
                    }

                }
            }
        }

        btnStop.setOnClickListener {
            mConnectedThread?.cancel()
            val intent = Intent(this, DataDoneActivity::class.java)
            intent.putExtra("data", savedDataList.toDoubleArray())
            startActivity(intent)

        }
        btnSaveCurrent.setOnClickListener {
            if (currentInput != null) {
                savedDataList.add(currentInput!!)
            }
        }


        var fail = false
        val device = bluetoothAdapter!!.getRemoteDevice(macAddress)
        var socket: BluetoothSocket? = null
        try {
            socket = createBluetoothSocket(device)
        } catch (e: IOException) {
            fail = true
            Toast.makeText(baseContext, "Socket creation failed", Toast.LENGTH_SHORT).show()
        }
        try {
            socket!!.connect()
        } catch (e: IOException) {
            try {
                fail = true
                socket!!.close()
                mHandler!!.obtainMessage(CONNECTING_STATUS, -1, -1)
                        .sendToTarget()
            } catch (e2: IOException) {
                //insert code to deal with this
                Toast.makeText(baseContext, "Socket creation failed", Toast.LENGTH_SHORT).show()
            }

        }
        if (!fail) {
            mConnectedThread = ConnectedThread(socket!!)
            mConnectedThread!!.start()

            (mHandler as Handler).obtainMessage(CONNECTING_STATUS, 1, -1, device.name)
                    .sendToTarget()
        }

    }

    private fun parseBluetoothInput(str: String): Double {
        val strList = str.split("\n").toMutableList()
        if(strList.count() > 0) strList.removeAt(0)
        if(strList.count() > 0) strList.removeAt(strList.count() - 1)
        val floatList = mutableListOf<Float>()
        strList.forEach { x ->
            val n = x.toFloatOrNull()
            if (n != null) {
                floatList.add(n)
            }
        }
        //988
        val R = 988
        return ((((1024 / (floatList.toFloatArray().sum() / floatList.count())) - 1) * R) / 81.624) + 19.96471626
    }

    @Throws(IOException::class)
    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        try {
            val m = device.javaClass.getMethod("createInsecureRfcommSocketToServiceRecord", UUID::class.java)
            return m.invoke(device, BTMODULEUUID) as BluetoothSocket
        } catch (e: Exception) {
            println("Could not create Insecure RFComm Connection")
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
                        mHandler!!.obtainMessage(BluetoothCommunicationActivity.MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget() // Send the obtained bytes to the UI activity
                    }
                } catch (e: IOException) {
                    e.printStackTrace()

                    break
                } catch (e: ArrayIndexOutOfBoundsException) {
                    e.printStackTrace()
                    this.cancel()
                    runOnUiThread {
                        Toast.makeText(this@BluetoothCommunicationActivity,
                                "failed to connect because of interference, try again in like.... now???", Toast.LENGTH_LONG).show()
                    }
                    finish()
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
}
