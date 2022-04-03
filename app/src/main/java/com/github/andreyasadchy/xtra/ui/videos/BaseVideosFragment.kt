package com.github.andreyasadchy.xtra.ui.videos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.helix.video.Video
import com.github.andreyasadchy.xtra.ui.common.PagedListFragment
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.ui.download.HasDownloadDialog
import com.github.andreyasadchy.xtra.ui.download.VideoDownloadDialog
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import kotlinx.android.synthetic.main.common_recycler_view_layout.*

abstract class BaseVideosFragment<VM : BaseVideosViewModel> : PagedListFragment<Video, VM, BaseVideosAdapter>(), Scrollable, HasDownloadDialog {

    interface OnVideoSelectedListener {
        fun startVideo(video: Video, offset: Double? = null)
    }

    override val adapter: BaseVideosAdapter by lazy {
        val activity = requireActivity() as MainActivity
        VideosAdapter(this, activity, activity, activity) {
            lastSelectedItem = it
            showDownloadDialog()
        }
    }

    var lastSelectedItem: Video? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_videos, container, false)
    }

    override fun initialize() {
        super.initialize()
        if (requireContext().prefs().getBoolean(C.PLAYER_USE_VIDEOPOSITIONS, true)) {
            viewModel.positions.observe(viewLifecycleOwner) {
                adapter.setVideoPositions(it)
            }
        }
    }

    override fun scrollToTop() {
        recyclerView?.scrollToPosition(0)
    }

    override fun showDownloadDialog() {
        lastSelectedItem?.let {
            VideoDownloadDialog.newInstance(it).show(childFragmentManager, null)
        }
    }
}