package com.angler.angler.angler

import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.SystemClock
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


/**
 * this creates an async thread to keep track of the input communicated over bluetooth
 *
 * @property mmSocket describes the current bluetooth socket being sued
 * @property mHandler is the async handler that keeps track of the logic needed to be applied to data received
 * @property activity this is the bluetooth communication activity that is the main thread and keeps track of creating all the other threads and the bluetooth connection
 *
 */
class BluetoothConnectedThread(private val mmSocket: BluetoothSocket, private val mHandler: Handler?, private val activity: BluetoothCommunicationActivity) : Thread() {
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
                    mHandler?.obtainMessage(BluetoothCommunicationActivity.MESSAGE_READ, bytes, -1, buffer)?.sendToTarget() // Send the obtained bytes to the UI activity
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
                activity.runOnUiThread {
                    Toast.makeText(activity,
                            "failed to connect because of interference, try again in like.... now???", Toast.LENGTH_LONG).show()
                }
                //go to previous activity
                activity.finish()
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