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
import kotlin.math.ln
import kotlin.math.roundToInt

class BluetoothCommunicationActivity : AppCompatActivity() {
    //everything in the companion object is static, meaning only defined once and global
    companion object {
        //"random" unique identifier used to communicate over bluetooth
        private val BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        //used in bluetooth handler to identify message update
        private const val MESSAGE_READ = 2
        //used in bluetooth handler to identify message status
        private const val CONNECTING_STATUS = 3
    }

    //the default adapter for bluetooth, used to tell the android os what to do with bluetooth
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    //our main handler that will receive callback notifications
    private var mHandler: Handler? = null
    //bluetooth background worker thread to send and receive data
    //we will get back to this later in the code
    private var mConnectedThread: BluetoothCommunicationActivity.ConnectedThread? = null
    //to keep track of if we are connected
    private var connected = false
    //the current input we are handing, that we got from bluetooth
    private var currentInput: Double? = null
    //list of the data we have saved for the last screen
    private val savedDataList = mutableListOf<Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        //this is required by anything that inherits AppCompatActivity, which means all activities
        super.onCreate(savedInstanceState)
        //here we define a layout, the xml for that can be found in res/layout/activity_bluetooth_communication.xml
        setContentView(R.layout.activity_bluetooth_communication)
        //get the parameter that the activity that called this activity gave us
        val macAddress = intent.getStringExtra("mac_address")
        //if its not defined just go back the previous activity
        if (macAddress === "") {
            finishActivity(0)
            return
        }
        //create a new handler that handles the async nature of bluetooth
        mHandler = @SuppressLint("HandlerLeak")
        object : Handler() {
            //when we receive a message
            override fun handleMessage(msg: android.os.Message) {
                //check what the message is about
                if (msg.what == BluetoothCommunicationActivity.MESSAGE_READ) {
                    //try and parse the message as a string, we have never experience a catch with this
                    var readMessage: String? = null
                    try {
                        readMessage = String(msg.obj as ByteArray, Charset.defaultCharset())
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                    runOnUiThread {
                        //if the message is parsed currently
                        if (readMessage != null) {
                            //call the parsing function and set the currentInput to it
                            currentInput = parseBluetoothInput(readMessage)
                            //round the currentInput to an int, convert it to a string a display it on the txtAngleStatus view
                            txtAngleStatus.text = currentInput!!.roundToInt().toString() + "°"
                        }
                    }

                }
                if (msg.what == BluetoothCommunicationActivity.CONNECTING_STATUS) {
                    //if the message is a 1
                    if (msg.arg1 == 1) {
                        //tell the user a successful connection was made
                        runOnUiThread {
                            Toast.makeText(this@BluetoothCommunicationActivity, "Connected to Device: " + msg.obj as String, Toast.LENGTH_LONG).show()
                        }
                        //set connection to true
                        connected = true
                    } else {
                        //tell the user that the connection failed
                        runOnUiThread {
                            Toast.makeText(this@BluetoothCommunicationActivity, "Connection failed", Toast.LENGTH_LONG).show()
                        }
                        //go to previous activity
                        finish()
                    }

                }
            }
        }
        //when the stop button is clicked
        btnStop.setOnClickListener {
            //stop bluetooth
            mConnectedThread?.cancel()
            //create the new intent
            val intent = Intent(this, DataDoneActivity::class.java)
            //give it the data we have collected in the form an array of doubles
            intent.putExtra("data", savedDataList.toDoubleArray())
            //start the intent
            startActivity(intent)

        }
        //when the save button is clicked
        btnSaveCurrent.setOnClickListener {
            //if we have an input
            if (currentInput != null) {
                //save the input in the list
                savedDataList.add(currentInput!!)
            }
        }
        //(one of the variable to check) if the connection failed
        var fail = false
        //find the device based on the parameter of the previous activity
        val device = bluetoothAdapter!!.getRemoteDevice(macAddress)
        //try and create a new bluetooth socket
        var socket: BluetoothSocket? = null
        try {
            socket = createBluetoothSocket(device)
        } catch (e: IOException) {
            //if failed, set the fail variable and tell the user
            fail = true
            Toast.makeText(baseContext, "Socket creation failed", Toast.LENGTH_SHORT).show()
        }
        //try and connect to the socket
        try {
            socket!!.connect()
        } catch (e: IOException) {
            try {
                //set the fail variable
                fail = true
                //close the socket
                socket!!.close()
                //tell the handler that we failed
                mHandler!!.obtainMessage(CONNECTING_STATUS, -1, -1)
                        .sendToTarget()
            } catch (e2: IOException) {
                //tell the user that shit went wrong
                Toast.makeText(baseContext, "Socket creation failed", Toast.LENGTH_SHORT).show()
            }

        }
        if (!fail) {
            //if we did not fail
            //new thread, more a little later down the code
            mConnectedThread = ConnectedThread(socket!!)
            //start the thread
            mConnectedThread!!.start()
            //tell the handler that we succeeded
            (mHandler as Handler).obtainMessage(CONNECTING_STATUS, 1, -1, device.name)
                    .sendToTarget()
        }

    }

    //function for parsing the data from the arduino
    //str is the raw text from the arduino
    private fun parseBluetoothInput(str: String): Double {
        //strList is a list made from cutting the str at all the line breaks
        val strList = str.split("\n").toMutableList()
        //the first and last of all these lines are usually corrupted data
        //so we delete the first and last if the size of the list is over 0
        if (strList.count() > 0) strList.removeAt(0)
        if (strList.count() > 0) strList.removeAt(strList.count() - 1)
        //here we make the strList a floatList by passing all the strings to floating point numbers
        val floatList = mutableListOf<Float>()
        strList.forEach { x ->
            val n = x.toFloatOrNull()
            if (n != null) {
                floatList.add(n)
            }
        }
        //this is the function we discovered by our scientific test of the product
        fun f(x: Double): Double {
            return -125.4439201 * ln(0.007602250266 * x)
        }
        //return the f of the average of the list
        return f(floatList.toFloatArray().average())
    }

    //this function create new bluetooth socket
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

    //the class that handles the thread that reads data from bluetooth
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        //define the input and output stream
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        //init is the constructor of the class
        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            //get the input and output streams, using temp objects because
            //member streams are final
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            //buffer store for the stream
            var buffer = ByteArray(1024)
            //bytes returned from read()
            var bytes: Int
            //keep listening to the InputStream until an exception occurs
            //we are allowed to use while(true) here, since it happens on another thread
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
                }
                //an IO exception might happen once in a while and you should worry about it says the documentation
                catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
                //bluetooth has a tendency to send corrupted byte arrays, if this happens it fucks up everything and we go to the previous activity
                catch (e: ArrayIndexOutOfBoundsException) {
                    e.printStackTrace()
                    //stop bluetooth
                    this.cancel()
                    //tell the user things fucked up, but they can try again
                    runOnUiThread {
                        Toast.makeText(this@BluetoothCommunicationActivity,
                                "failed to connect because of interference, try again in like.... now???", Toast.LENGTH_LONG).show()
                    }
                    //go to previous activity
                    finish()
                    //stop the while loop
                    break
                }
            }
        }

        //call this from the main activity to send data to the remote device
        fun write(input: String) {
            //converts entered String into bytes
            val bytes = input.toByteArray()
            try {
                mmOutStream!!.write(bytes)
            } catch (e: IOException) {
            }

        }

        //call this from the main activity to shutdown the connection
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
            }

        }
    }
}
