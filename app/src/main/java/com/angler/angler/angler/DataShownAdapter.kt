package com.angler.angler.angler

import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.data_entry.view.*

//this controls how each item in the list is displayed and what happens when you click it or otherwise interact with it
//data describes the data that we have to show
class DataShownAdapter(private val data: Double): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        //set the date_entry view's text to the data as a string
        viewHolder.itemView.data_entry.text = data.toString()
    }

    //this describes the standard ui we running with for this list
    //this can be found at res/layout/data_entry.xml
    override fun getLayout(): Int {
        return R.layout.data_entry
    }

}