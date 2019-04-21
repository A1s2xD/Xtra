package com.github.exact7.xtra.ui.player.stream

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.exact7.xtra.R
import com.github.exact7.xtra.model.LoggedIn
import com.github.exact7.xtra.model.kraken.Channel
import com.github.exact7.xtra.model.kraken.stream.Stream
import com.github.exact7.xtra.ui.common.RadioButtonDialogFragment
import com.github.exact7.xtra.ui.main.MainViewModel
import com.github.exact7.xtra.ui.player.BasePlayerFragment
import com.github.exact7.xtra.util.C
import com.github.exact7.xtra.util.FragmentUtils
import kotlinx.android.synthetic.main.fragment_player_stream.*
import kotlinx.android.synthetic.main.player_stream.*

@Suppress("PLUGIN_WARNING")
class StreamPlayerFragment : BasePlayerFragment(), RadioButtonDialogFragment.OnSortOptionChanged {

    override lateinit var viewModel: StreamPlayerViewModel
    private lateinit var stream: Stream
    override val channel: Channel
        get() = stream.channel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stream = requireArguments().getParcelable(C.STREAM)!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_player_stream, container, false)
    }

    override fun initialize() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(StreamPlayerViewModel::class.java)
        ViewModelProviders.of(requireActivity(), viewModelFactory).get(MainViewModel::class.java).user.observe(viewLifecycleOwner, Observer {
            viewModel.startStream(stream, it)
            initializeViewModel(viewModel)
            if (it is LoggedIn) {
                chatView.messagingEnabled = true
                viewModel.emotes.observe(viewLifecycleOwner, Observer(chatView::addEmotes))
            }
        })
        viewModel.loaded.observe(this, Observer {
            settings.isEnabled = true
            settings.setColorFilter(Color.WHITE) //TODO
        })
        viewModel.chat.observe(viewLifecycleOwner, Observer(chatView::setCallback))
        settings.setOnClickListener {
            FragmentUtils.showRadioButtonDialogFragment(childFragmentManager, viewModel.qualities, viewModel.selectedQualityIndex)
        }
        resume.setOnClickListener { viewModel.player.seekToDefaultPosition() }
    }

    fun hideEmotesMenu() = chatView.hideEmotesMenu()

    override fun onMinimize() {
        super.onMinimize()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(chatView.windowToken, 0)
    }

//    override fun play(obj: Parcelable) {
//        val stream = obj as Stream
//        if (viewModel.stream != stream) {
//            viewModel.player.playWhenReady = false
//            chatView.adapter.submitList(null)
//        }
//        viewModel.stream = stream
//        draggableView?.maximize()
//    }

    override fun onChange(index: Int, text: CharSequence, tag: Int?) {
        viewModel.changeQuality(index)
//            if (index >= viewModel.helper.urls.value!!.lastIndex) {
//                TODO hide player
//            }
    }

    override fun onMovedToForeground() {
        if (this::viewModel.isInitialized && (!viewModel.isResumed || shouldHandleLifecycle)) {
//            viewModel.onResume()
        }
    }

    override fun onMovedToBackground() {
        super.onMovedToBackground()
        if (this::viewModel.isInitialized && shouldHandleLifecycle) {
//            viewModel.onPause()
        }
    }

    override fun onNetworkRestored() {
        if (this::viewModel.isInitialized) {
            viewModel.onResume()
        }
    }
}
