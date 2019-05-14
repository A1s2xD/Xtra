package com.github.exact7.xtra.ui.download

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.github.exact7.xtra.R
import com.github.exact7.xtra.model.VideoDownloadInfo
import com.github.exact7.xtra.model.kraken.video.Video
import com.github.exact7.xtra.util.C
import com.github.exact7.xtra.util.visible
import kotlinx.android.synthetic.main.dialog_video_download.*
import javax.inject.Inject


class VideoDownloadDialog : BaseDownloadDialog() {

    companion object {
        private const val KEY_VIDEO_INFO = "videoInfo"
        private const val KEY_VIDEO = "video"

        fun newInstance(videoInfo: VideoDownloadInfo? = null, video: Video? = null): VideoDownloadDialog {
            return VideoDownloadDialog().apply {
                arguments = bundleOf(KEY_VIDEO_INFO to videoInfo, KEY_VIDEO to video)
            }
        }
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: VideoDownloadViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?  =
            inflater.inflate(R.layout.dialog_video_download, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(VideoDownloadViewModel::class.java)
        viewModel.videoInfo.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                (requireView() as ConstraintLayout).children.forEach { v -> v.visible(v.id != R.id.progressBar && v.id != R.id.storageSelectionContainer) }
                init(it)
            } else {
                dismiss()
            }
        })
        requireArguments().getParcelable<VideoDownloadInfo?>(KEY_VIDEO_INFO).let {
            if (it == null) {
                viewModel.setVideo(requireArguments().getParcelable(KEY_VIDEO)!!)
            } else {
                viewModel.setVideoInfo(it)
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun init(videoInfo: VideoDownloadInfo) {
        val context = requireContext()
        init(context)
        with(videoInfo) {
            spinner.adapter = ArrayAdapter(context, R.layout.spinner_quality_item, qualities.keys.toTypedArray())
            with(DateUtils.formatElapsedTime(totalDuration / 1000L)) {
                duration.text = context.getString(R.string.duration, this)
                timeTo.hint = this.let { if (it.length != 5) it else "00:$it" }
            }
            timeFrom.hint = DateUtils.formatElapsedTime(currentPosition / 1000L).let { if (it.length == 5) "00:$it" else it }
            timeFrom.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    timeFrom.error = null
                }
            })
            timeTo.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    timeTo.error = null
                }
            })
            cancel.setOnClickListener { dismiss() }
            download.setOnClickListener {
                val from = parseTime(timeFrom) ?: return@setOnClickListener
                val to = parseTime(timeTo) ?: return@setOnClickListener
                when {
                    to > totalDuration -> {
                        timeTo.requestFocus()
                        timeTo.error = getString(R.string.to_is_longer)
                    }
                    from < to -> {
                        val fromIndex = if (from == 0L) {
                            0
                        } else {
                            val min = from - targetDuration
                            relativeStartTimes.binarySearch(comparison = { time ->
                                when {
                                    time > from -> 1
                                    time < min -> -1
                                    else -> 0
                                }
                            })
                        }
                        val toIndex = if (to in relativeStartTimes.last()..totalDuration) {
                            relativeStartTimes.lastIndex
                        } else {
                            val max = to + targetDuration
                            relativeStartTimes.binarySearch(comparison = { time ->
                                when {
                                    time > max -> 1
                                    time < to -> -1
                                    else -> 0
                                }
                            })
                        }

                        val preference = prefs.getString(C.DOWNLOAD_NETWORK_PREFERENCE, "3")
                        var wifiOnly = preference == "2"

                        fun download() {
                            val quality = spinner.selectedItem.toString()
                            val url = videoInfo.qualities.getValue(quality).substringBeforeLast('/') + "/"
                            viewModel.download(url, downloadPath, quality, fromIndex, toIndex, wifiOnly)
                            dismiss()
                        }

                        if (preference != "3") {
                            download()
                        } else {
                            wifiOnly = true
                            AlertDialog.Builder(context)
                                    .setMultiChoiceItems(arrayOf(getString(R.string.wifi_only)), BooleanArray(1) { true }) { _, _, isChecked -> wifiOnly = isChecked }
                                    .setPositiveButton(getString(R.string.always)) { _, _ ->
                                        prefs.edit { putString(C.DOWNLOAD_NETWORK_PREFERENCE, if (wifiOnly) "2" else "1") }
                                        download()
                                    }
                                    .setNegativeButton(getString(R.string.just_once)) { _, _ -> download() }
                                    .setNeutralButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                                    .setCustomTitle(LayoutInflater.from(context).inflate(R.layout.view_download_warning, null))
                                    .show()
                        }
                    }
                    from >= to -> {
                        timeFrom.requestFocus()
                        timeFrom.error = getString(R.string.from_is_greater)
                    }
                    else -> {
                        timeTo.requestFocus()
                        timeTo.error = getString(R.string.to_is_lesser)
                    }
                }
            }
        }
    }

    private fun parseTime(textView: TextView): Long? {
        with(textView) {
            val value = if (text.isEmpty()) hint else text
            val time = value.split(Regex("[:.]"))
            try {
                if (time.size != 3) throw IllegalArgumentException()
                val hours = time[0].toLong()
                val minutes = time[1].toLong().also { if (it > 59) throw IllegalArgumentException()}
                val seconds = time[2].toLong().also { if (it > 59) throw IllegalArgumentException()}
                return ((hours * 3600) + (minutes * 60) + seconds) * 1000
            } catch (ex: Exception) {
                requestFocus()
                error = getString(R.string.invalid_time)
            }
        }
        return null
    }
}
