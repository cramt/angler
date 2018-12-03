package com.angler.angler.angler


import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main.*

/**
 * this is the starting activity
 * this means that the whole app starts here
 */
class MainActivity : AppCompatActivity() {
    /**
     * when the thing is created
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        //this is required by anything that inherits AppCompatActivity, which means all activities
        super.onCreate(savedInstanceState)
        //here we define a layout, the xml for that can be found in res/layout/activity_main.xml
        setContentView(R.layout.activity_main)
        //define the bluetooth adapter
        val defaultBtAdapter = BluetoothAdapter.getDefaultAdapter()
        //run this on the ui thread cause otherwise it might crash
        runOnUiThread {
            //if there is no bluetooth tell the user
            if(defaultBtAdapter == null){
                Toast.makeText(this@MainActivity, "bluetooth isn't available", Toast.LENGTH_LONG).show()
            }
            //otherwise create a list of all the possible bluetooth devices
            else {
                //create and adapter, this is basically a "holder" for the list
                val adapter = GroupAdapter<ViewHolder>()
                avaibleBluetoothDevices.adapter = adapter
                //make the content of the thing be linear
                avaibleBluetoothDevices.layoutManager = LinearLayoutManager(this@MainActivity)
                //foreach bonded bluetooth device add it to the adapter using the BluetoothAvailableAdapter class
                //BluetoothAvailableAdapter can be found at BluetoothAvailableAdapter.kt
                for (bondedDevice in defaultBtAdapter.bondedDevices) {
                    adapter.add(BluetoothAvailableAdapter(this@MainActivity, bondedDevice))
                }
            }
        }
    }
}
