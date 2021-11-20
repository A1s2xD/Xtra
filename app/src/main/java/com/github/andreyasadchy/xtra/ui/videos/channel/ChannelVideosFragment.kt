package com.github.andreyasadchy.xtra.ui.videos.channel

import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.helix.video.Sort
import com.github.andreyasadchy.xtra.ui.common.RadioButtonDialogFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosAdapter
import com.github.andreyasadchy.xtra.ui.videos.BaseVideosFragment
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.FragmentUtils
import com.github.andreyasadchy.xtra.util.prefs
import kotlinx.android.synthetic.main.fragment_videos.*
import kotlinx.android.synthetic.main.sort_bar.*

class ChannelVideosFragment : BaseVideosFragment<ChannelVideosViewModel>(), RadioButtonDialogFragment.OnSortOptionChanged {

    override val viewModel by viewModels<ChannelVideosViewModel> { viewModelFactory }
    override val adapter: BaseVideosAdapter by lazy {
        ChannelVideosAdapter(this, requireActivity() as MainActivity) {
            lastSelectedItem = it
            showDownloadDialog()
        }
    }

    override fun initialize() {
        super.initialize()
        viewModel.sortText.observe(viewLifecycleOwner, Observer {
            sortText.text = it
        })
        viewModel.setChannelId(requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""), requireContext().prefs().getString(C.TOKEN, "") ?: "", requireArguments().getString(C.CHANNEL)!!)
        sortBar.setOnClickListener { FragmentUtils.showRadioButtonDialogFragment(requireContext(), childFragmentManager, viewModel.sortOptions, viewModel.selectedIndex) }
    }

    override fun onChange(requestCode: Int, index: Int, text: CharSequence, tag: Int?) {
        adapter.submitList(null)
        viewModel.setSort(requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""), requireContext().prefs().getString(C.TOKEN, "") ?: "", if (tag == R.string.upload_date) Sort.TIME else Sort.VIEWS, index, text)
    }
}