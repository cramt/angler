package com.angler.angler.angler


import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intent = Intent(this, DataDoneActivity::class.java)
        val list = mutableListOf<Double>()
        list.add(1321.432423)
        list.add(235.5)
        list.add(543.54)
        list.add(321.3)
        intent.putExtra("data", list.toDoubleArray())
        startActivity(intent)
        /*
        runOnUiThread {


            val adapter = GroupAdapter<ViewHolder>()
            avaibleBluetoothDevices.adapter = adapter
            avaibleBluetoothDevices.layoutManager = LinearLayoutManager(this@MainActivity)
            for (bondedDevice in BluetoothAdapter.getDefaultAdapter().bondedDevices) {
                adapter.add(BluetoothAvailableAdapter(this@MainActivity, bondedDevice));
            }

        }
        */
    }
}
