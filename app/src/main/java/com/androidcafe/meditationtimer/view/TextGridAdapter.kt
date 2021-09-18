package com.androidcafe.meditationtimer.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.androidcafe.meditationtimer.databinding.GridViewItemBinding
import com.androidcafe.meditationtimer.viewmodel.GridViewItemData
import com.androidcafe.meditationtimer.viewmodel.MainFragmentViewModel

class TextGridAdapter(
    private val viewModel: MainFragmentViewModel,
    private val savePreferenceCallback: ()-> Unit)
    : ListAdapter<GridViewItemData, TextGridAdapter.TextViewHolder>(DiffCallback){

    private val gridViewItemBindingList = mutableListOf<GridViewItemBinding>()

    // grid view holder
    class TextViewHolder(private val binding: GridViewItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val meditateMinutesText = binding.meditateMinutesText

        fun bindViewItemData(viewItemData: GridViewItemData) {
            binding.viewItemData = viewItemData
            binding.executePendingBindings()
        }
    }

    // grid view item data diff
    companion object DiffCallback : DiffUtil.ItemCallback<GridViewItemData>() {
        override fun areItemsTheSame(oldItem: GridViewItemData, newItem: GridViewItemData): Boolean {
            return oldItem.meditateMinutes == newItem.meditateMinutes
        }

        override fun areContentsTheSame(oldItem: GridViewItemData, newItem: GridViewItemData): Boolean {
            return oldItem.meditateMinutes == newItem.meditateMinutes
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {

        val gridViewItemBinding = GridViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        gridViewItemBindingList.add(gridViewItemBinding)

        return TextViewHolder(gridViewItemBinding)
    }

    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {

        val viewItemData = getItem(position)

        holder.bindViewItemData(viewItemData)
        holder.meditateMinutesText.setOnClickListener {
            viewModel.updateSelectedMinutes(viewItemData.meditateMinutes)
            refresh()
            savePreferenceCallback()
        }
    }

    fun refresh() {

        for(binding in gridViewItemBindingList) {
            binding.invalidateAll()
        }
    }
}
