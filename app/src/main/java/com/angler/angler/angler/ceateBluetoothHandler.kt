package com.angler.angler.angler

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_bluetooth_communication.*
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import kotlin.math.roundToInt

/**
 * creates that handler that does the logic for the bluetooth input
 *
 * @param activity the bluetooth communication activity which is the main thread that keeps track of all the other threads
 */
fun createBluetoothHandler(activity: BluetoothCommunicationActivity): Handler {
    return (@SuppressLint("HandlerLeak") object : Handler() {
        //when we receive a message
        override fun handleMessage(msg: Message) {
            //check what the message is about
            if (msg.what == BluetoothCommunicationActivity.MESSAGE_READ) {
                //try and parse the message as a string, we have never experience a catch with this
                var readMessage: String? = null
                try {
                    readMessage = String(msg.obj as ByteArray, Charset.defaultCharset())
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                activity.runOnUiThread {
                    //if the message is parsed currently
                    if (readMessage != null) {
                        //call the parsing function and set the currentInput to it
                        activity.currentInput = activity.parseBluetoothInput(readMessage)
                        //round the currentInput to an int, convert it to a string a display it on the txtAngleStatus view
                        activity.txtAngleStatus.text = activity.currentInput!!.roundToInt().toString() + "°"
                    }
                }

            }
            if (msg.what == BluetoothCommunicationActivity.CONNECTING_STATUS) {
                //if the message is a 1
                if (msg.arg1 == 1) {
                    //tell the user a successful connection was made
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Connected to Device: " + msg.obj as String, Toast.LENGTH_LONG).show()
                    }
                    //set connection to true
                    activity.connected = true
                } else {
                    //tell the user that the connection failed
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Connection failed", Toast.LENGTH_LONG).show()
                    }
                    //go to previous activity
                    activity.finish()
                }

            }
        }
    })
}