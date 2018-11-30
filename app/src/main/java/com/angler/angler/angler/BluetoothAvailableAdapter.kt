package com.angler.angler.angler

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.bluetooth_entry.view.*

//this controls how each item in the list is displayed and what happens when you click it or otherwise interact with it
//ac describes the activity that created it, and makes sure it can communicate with it
class BluetoothAvailableAdapter(private val ac: MainActivity, private val bluetoothDevice: BluetoothDevice): Item<ViewHolder>() {
    //we require a specific version of android for this
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    //when an item is created
    //viewHolder, surprisingly, holds and keeps track of the view that we are going to create
    //position, surprisingly, is the position of the view in the list
    override fun bind(viewHolder: ViewHolder, position: Int) {
        //this sets the text of the view called bluetooth_name to the name of the specific bluetooth device we are displaying
        viewHolder.itemView.bluetooth_name.text = bluetoothDevice.name
        //and the same with the mac address
        viewHolder.itemView.mac_address.text = bluetoothDevice.address
        //when either of the 2 previous views are clicked
        viewHolder.itemView.setOnClickListener {
            //define a new intent which is a thing that creates a new activity based on one of our classes.
            //that class here is BluetoothCommunicationActivity, for more information check BluetoothCommunicationActivity.kt
            val i = Intent(ac, BluetoothCommunicationActivity::class.java)
            //give the intent an argument, BluetoothCommunicationActivity will be able to read this once its started
            i.putExtra("mac_address", bluetoothDevice.address)
            //start the activity based on the intent
            ac.startActivity(i)
        }

    }

    //this describes the standard ui we running with for this list
    //this can be found at res/layout/bluetooth_entry.xml
    override fun getLayout(): Int {
        return R.layout.bluetooth_entry
    }

}