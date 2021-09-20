package com.androidcafe.meditationtimer.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkInfo
import com.androidcafe.meditationtimer.databinding.MainFragmentBinding
import com.androidcafe.meditationtimer.viewmodel.CountDownWorker
import com.androidcafe.meditationtimer.viewmodel.MainFragmentViewModel

const val SHARED_PREF_SELECTED_MINUTES_KEY = "SelectedMinutes"

class MainFragment : Fragment() {

    private val mainFragmentBinding: MainFragmentBinding by lazy {
        MainFragmentBinding.inflate(layoutInflater)
    }
    private val textGridAdapter: TextGridAdapter by lazy {
        TextGridAdapter(viewModel, ::savePreference)
    }
    private val viewModel: MainFragmentViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
    }

    private val countDownWorkProgressObserver = Observer<WorkInfo> { workInfo: WorkInfo?  ->
        if (workInfo != null) {
            val progress = workInfo.progress
            val timeInMS = progress.getLong(CountDownWorker.PROGRESS, -1L)
            if(timeInMS >= 0L) {
                viewModel.updateTimerDIsplayText(timeInMS)
            }

            if(workInfo.state == WorkInfo.State.CANCELLED) {
                viewModel.onCountDownWorkCancelled()
                textGridAdapter.refresh()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadPreference()
    }

    private fun loadPreference() {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val savedSelectedMinutes = sharedPref.getInt(SHARED_PREF_SELECTED_MINUTES_KEY, viewModel.selectedMinutes)
        viewModel.updateSelectedMinutes(savedSelectedMinutes)
    }

    private fun savePreference() {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt(SHARED_PREF_SELECTED_MINUTES_KEY, viewModel.selectedMinutes)
        editor.apply()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        mainFragmentBinding.lifecycleOwner = this
        mainFragmentBinding.viewModel = viewModel
        mainFragmentBinding.textGridRecyclerView.adapter = textGridAdapter
        registerButtonClickCallback()

        return mainFragmentBinding.root
    }

    private fun registerButtonClickCallback() {
        mainFragmentBinding.timerButton.setOnClickListener() {

            viewModel.countDownWorkInfo?.removeObserver(countDownWorkProgressObserver)

            viewModel.toggleButton()

            viewModel.countDownWorkInfo?.observe(viewLifecycleOwner, countDownWorkProgressObserver)

            textGridAdapter.refresh()
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.countDownWorkInfo?.removeObserver(countDownWorkProgressObserver)
        viewModel.countDownWorkInfo?.observe(viewLifecycleOwner, countDownWorkProgressObserver)
    }
}
