package com.angler.angler.angler

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.ClipboardManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_data_done.*


class DataDoneActivity : AppCompatActivity() {
    //when the thing is created
    override fun onCreate(savedInstanceState: Bundle?) {
        //this is required by anything that inherits AppCompatActivity, which means all activities
        super.onCreate(savedInstanceState)
        //here we define a layout, the xml for that can be found in res/layout/activity_data_done.xml
        setContentView(R.layout.activity_data_done)
        //get the parameter that the activity that called this activity gave us
        if (!intent.hasExtra("data")) {
            finish()
            return
        }
        val data = intent.getDoubleArrayExtra("data")
        //run this on the ui thread cause otherwise it might crash
        runOnUiThread {
            //create and adapter, this is basically a "holder" for the list
            val adapter = GroupAdapter<ViewHolder>()
            dataDoneList.adapter = adapter
            //make the content of the thing be linear
            dataDoneList.layoutManager = LinearLayoutManager(this@DataDoneActivity)
            //foreach bonded bluetooth device add it to the adapter using the DataShownAdapter class
            //DataShownAdapter can be found in DataShownAdapter.kt
            data.forEach {
                adapter.add(DataShownAdapter(it))
            }
        }
        //when the cope as json button is clicked
        btnCopyAsJson.setOnClickListener {
            //get the clipboard
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            //join all the floats with a ", " as the separator, and put it in []
            //set that to the clipboard
            clipboard.text = "[ " + data.joinToString(", ") + " ]"
        }
    }
}

