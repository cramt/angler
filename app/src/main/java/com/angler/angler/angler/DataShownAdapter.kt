package com.angler.angler.angler

import android.bluetooth.BluetoothDevice
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.data_entry.view.*

class DataShownAdapter(val ac: DataDoneActivity, private val data: Double): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.data_entry.text = data.toString()
    }


    override fun getLayout(): Int {
        return R.layout.data_entry
    }

}