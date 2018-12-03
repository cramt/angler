package com.angler.angler.angler

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_bluetooth_communication.*
import java.io.IOException
import java.util.*
import kotlin.math.pow


/**
 *
 */
class BluetoothCommunicationActivity : AppCompatActivity() {
    //everything in the companion object is static, meaning only defined once and global
    companion object {
        //"random" unique identifier used to communicate over bluetooth
        private val BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        //used in bluetooth handler to identify message update
        const val MESSAGE_READ = 2
        //used in bluetooth handler to identify message status
        const val CONNECTING_STATUS = 3
    }

    //the default adapter for bluetooth, used to tell the android os what to do with bluetooth
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    //our main handler that will receive callback notifications
    private var mHandler: Handler? = null
    //bluetooth background worker thread to send and receive data
    //we will get back to this later in the code
    private var mBluetoothConnectedThread: BluetoothConnectedThread? = null
    //to keep track of if we are connected
    var connected = false
    //the current input we are handing, that we got from bluetooth
    var currentInput: Double? = null
    //list of the data we have saved for the last screen
    private val savedDataList = mutableListOf<Double>()

    /**
     * when the activity is created
     */
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
        //this is documented in createBluetoothHandler.kt
        mHandler = createBluetoothHandler(this)
        //when the stop button is clicked
        btnStop.setOnClickListener {
            //stop bluetooth
            mBluetoothConnectedThread?.cancel()
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
            mBluetoothConnectedThread = BluetoothConnectedThread(socket!!, mHandler, this)
            //start the thread
            mBluetoothConnectedThread!!.start()
            //tell the handler that we succeeded
            (mHandler as Handler).obtainMessage(CONNECTING_STATUS, 1, -1, device.name)
                    .sendToTarget()
        }

    }

    /**
     * function for parsing the data from the arduino
     * @param str is the raw text from the arduino
     */
    fun parseBluetoothInput(str: String): Double {
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
            return 668.63*0.97029.pow(x)
        }
        //return the f of the average of the list
        return f(floatList.toFloatArray().average())
    }

    /**
     * this function create new bluetooth socket
     * @param device is a the device we wanna connect to
     */
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
}
