package com.androidcafe.meditationtimer.ui

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.androidcafe.meditationtimer.viewmodel.GridViewItemData

@BindingAdapter("gridViewItemListData")
fun bindRecyclerView(recyclerView: RecyclerView, data: List<GridViewItemData>?) {
    val adapter = recyclerView.adapter as TextGridAdapter
    adapter.submitList(data)
}
