package com.angler.angler.angler

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.bluetooth_entry.view.*

class BluetoothAvailableAdapter(val ac: MainActivity, private val bluetoothDevice: BluetoothDevice): Item<ViewHolder>() {
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.bluetooth_entry_layout.setOnClickListener {

        }
        viewHolder.itemView.bluetooth_name.text = bluetoothDevice.name
        viewHolder.itemView.setOnClickListener {
            val i = Intent(ac, BluetoothCommunicationActivity::class.java)
            i.putExtra("mac_address", bluetoothDevice.address)
            ac.startActivity(i)
        }
        viewHolder.itemView.mac_address.text = bluetoothDevice.address
    }


    override fun getLayout(): Int {
        return R.layout.bluetooth_entry
    }

}