package com.github.exact7.xtra.ui.videos.channel

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.exact7.xtra.R
import com.github.exact7.xtra.ui.fragment.RadioButtonDialogFragment
import com.github.exact7.xtra.ui.videos.BaseVideosFragment
import com.github.exact7.xtra.ui.videos.Sort
import com.github.exact7.xtra.util.FragmentUtils
import kotlinx.android.synthetic.main.fragment_videos.*

class ChannelVideosFragment : BaseVideosFragment(), RadioButtonDialogFragment.OnSortOptionChanged {

    private lateinit var viewModel: ChannelVideosViewModel

    override fun initialize() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ChannelVideosViewModel::class.java)
        binding.viewModel = viewModel
        binding.sortText = viewModel.sortText
        viewModel.list.observe(this, Observer {
            adapter.submitList(it)
        })
        viewModel.setChannelId(arguments?.get("channelId")!!)
        sortBar.setOnClickListener { FragmentUtils.showRadioButtonDialogFragment(requireContext(), childFragmentManager, viewModel.sortOptions, viewModel.selectedIndex) }
    }

    override fun onNetworkRestored() {
        viewModel.retry()
    }

    override fun onChange(index: Int, text: CharSequence, tag: Int?) {
        adapter.submitList(null)
        viewModel.setSort(if (tag == R.string.upload_date) Sort.TIME else Sort.VIEWS, index, text)
    }
}