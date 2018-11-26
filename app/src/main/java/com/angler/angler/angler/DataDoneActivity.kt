package com.angler.angler.angler

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.ClipboardManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder

import kotlinx.android.synthetic.main.activity_data_done.*

class DataDoneActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_done)
        if (!intent.hasExtra("data")) {
            finish()
            return
        }
        val data = intent.getDoubleArrayExtra("data")
        runOnUiThread {
            val adapter = GroupAdapter<ViewHolder>()
            dataDoneList.adapter = adapter
            dataDoneList.layoutManager = LinearLayoutManager(this@DataDoneActivity)
            data.forEach {
                adapter.add(DataShownAdapter(this@DataDoneActivity, it))
            }
        }
        btnCopyAsJson.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.text = "[ " + data.joinToString(", ") + " ]"
        }
    }
}

