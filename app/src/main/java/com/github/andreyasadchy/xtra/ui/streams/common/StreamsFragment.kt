package com.github.andreyasadchy.xtra.ui.streams.common

import android.os.Bundle
import androidx.fragment.app.viewModels
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.ui.common.BasePagedListAdapter
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.streams.BaseStreamsFragment
import com.github.andreyasadchy.xtra.ui.streams.StreamsCompactAdapter
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import kotlinx.android.synthetic.main.fragment_streams.*

class StreamsFragment : BaseStreamsFragment<StreamsViewModel>() {

    interface OnTagStreams {
        fun openTagStreams(tags: List<String>?, gameId: String?, gameName: String?)
    }

    companion object {
        fun newInstance(tags: List<String>?, gameId: String?, gameName: String?) = StreamsFragment().apply {
            bundle.putStringArray(C.TAGS, tags?.toTypedArray())
            bundle.putString(C.GAME_ID, gameId)
            bundle.putString(C.GAME_NAME, gameName)
            arguments = bundle
        }
    }

    val bundle = Bundle()
    override val viewModel by viewModels<StreamsViewModel> { viewModelFactory }

    override val adapter: BasePagedListAdapter<Stream> by lazy {
        if (!compactStreams) {
            super.adapter
        } else {
            val activity = requireActivity() as MainActivity
            StreamsCompactAdapter(this, activity, activity, activity)
        }
    }

    private var compactStreams = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        compactStreams = requireContext().prefs().getBoolean(C.COMPACT_STREAMS, false)
    }

    override fun initialize() {
        super.initialize()
        if (arguments?.getStringArray(C.TAGS) == null && requireContext().prefs().getString(C.USERNAME, "") != "" && !requireContext().prefs().getBoolean(C.UI_TAGS, true)) {
            viewModel.loadStreams(useHelix = true, showTags = requireContext().prefs().getBoolean(C.UI_TAGS, true), clientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""), token = requireContext().prefs().getString(C.TOKEN, ""), gameId = arguments?.getString(C.GAME_ID), thumbnailsEnabled = !compactStreams)
        } else {
            viewModel.loadStreams(useHelix = false, showTags = requireContext().prefs().getBoolean(C.UI_TAGS, true), clientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""), gameId = arguments?.getString(C.GAME_ID), gameName = arguments?.getString(C.GAME_NAME), tags = arguments?.getStringArray(C.TAGS)?.toList(), thumbnailsEnabled = !compactStreams)
        }
        val activity = requireActivity() as MainActivity
        sortBar.visible()
        if (arguments?.getString(C.GAME_ID) != null && arguments?.getString(C.GAME_NAME) != null) {
            sortBar.setOnClickListener { activity.openTagSearch(gameId = arguments?.getString(C.GAME_ID), gameName = arguments?.getString(C.GAME_NAME)) }
        } else {
            sortBar.setOnClickListener { activity.openTagSearch() }
        }
    }
}