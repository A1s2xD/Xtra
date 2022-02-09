package com.github.andreyasadchy.xtra.ui.channel

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.viewpager.widget.ViewPager
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.NotLoggedIn
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.model.helix.stream.Stream
import com.github.andreyasadchy.xtra.ui.Utils
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.ui.common.follow.FollowFragment
import com.github.andreyasadchy.xtra.ui.common.pagers.MediaPagerFragment
import com.github.andreyasadchy.xtra.ui.login.LoginActivity
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.settings.SettingsActivity
import com.github.andreyasadchy.xtra.util.*
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.fragment_channel.*
import kotlinx.android.synthetic.main.fragment_channel.appBar
import kotlinx.android.synthetic.main.fragment_channel.collapsingToolbar
import kotlinx.android.synthetic.main.fragment_channel.follow
import kotlinx.android.synthetic.main.fragment_channel.menu
import kotlinx.android.synthetic.main.fragment_channel.search
import kotlinx.android.synthetic.main.fragment_channel.toolbar
import kotlinx.android.synthetic.main.fragment_channel.watchLive
import kotlinx.android.synthetic.main.fragment_channel_old.*
import kotlinx.android.synthetic.main.fragment_media_pager.*


class ChannelPagerFragment : MediaPagerFragment(), FollowFragment, Scrollable {

    companion object {
        fun newInstance(id: String?, login: String?, name: String?, channelLogo: String?, updateLocal: Boolean = false) = ChannelPagerFragment().apply {
            bundle.putString(C.CHANNEL_ID, id)
            bundle.putString(C.CHANNEL_LOGIN, login)
            bundle.putString(C.CHANNEL_DISPLAYNAME, name)
            bundle.putString(C.CHANNEL_PROFILEIMAGE, channelLogo)
            bundle.putBoolean(C.CHANNEL_UPDATELOCAL, updateLocal)
            arguments = bundle
        }
    }

    val bundle = Bundle()
    private val viewModel by viewModels<ChannelPagerViewModel> { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(if (requireContext().prefs().getString(C.USERNAME, "") != "") R.layout.fragment_channel else R.layout.fragment_channel_old, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val isLoggedIn = User.get(activity) !is NotLoggedIn
        setAdapter(ChannelPagerAdapter(activity, childFragmentManager, requireArguments()))
        if (activity.isInLandscapeOrientation) {
            appBar.setExpanded(false, false)
        }
        if (requireContext().prefs().getString(C.USERNAME, "") != "") {
            requireArguments().getString(C.CHANNEL_DISPLAYNAME).let {
                if (it != null) {
                    userLayout.visible()
                    userName.visible()
                    userName.text = it
                }
            }
            requireArguments().getString(C.CHANNEL_PROFILEIMAGE).let {
                if (it != null) {
                    userLayout.visible()
                    userImage.visible()
                    userImage.loadImage(this, it, circle = true)
                }
            }
        } else {
            collapsingToolbar.title = requireArguments().getString(C.CHANNEL_DISPLAYNAME)
            logo.loadImage(this, requireArguments().getString(C.CHANNEL_PROFILEIMAGE), circle = true)
        }
        toolbar.apply {
            navigationIcon = Utils.getNavigationIcon(activity)
            setNavigationOnClickListener { activity.popFragment() }
        }
        search.setOnClickListener { activity.openSearch() }
        menu.setOnClickListener { it ->
            PopupMenu(activity, it).apply {
                inflate(R.menu.top_menu)
                menu.findItem(R.id.login).title = if (isLoggedIn) getString(R.string.log_out) else getString(R.string.log_in)
                setOnMenuItemClickListener {
                    when(it.itemId) {
                        R.id.settings -> { activity.startActivityFromFragment(this@ChannelPagerFragment, Intent(activity, SettingsActivity::class.java), 3) }
                        R.id.login -> {
                            if (!isLoggedIn) {
                                activity.startActivityForResult(Intent(activity, LoginActivity::class.java), 1)
                            } else {
                                AlertDialog.Builder(activity)
                                    .setTitle(getString(R.string.logout_title))
                                    .setMessage(getString(R.string.logout_msg, context?.prefs()?.getString(C.USERNAME, "")))
                                    .setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
                                    .setPositiveButton(getString(R.string.yes)) { _, _ ->
                                        activity.startActivityForResult(Intent(activity, LoginActivity::class.java), 2) }
                                    .show()
                            }
                        }
                        else -> menu.close()
                    }
                    true
                }
                show()
            }
        }
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            private val layoutParams = collapsingToolbar.layoutParams as AppBarLayout.LayoutParams
            private val originalScrollFlags = layoutParams.scrollFlags

            override fun onPageSelected(position: Int) {
//                layoutParams.scrollFlags = if (position != 3) {
                layoutParams.scrollFlags = if (position != 2) {
                    originalScrollFlags
                } else {
                    appBar.setExpanded(false, isResumed)
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
        })
    }

    override fun initialize() {
        val activity = requireActivity() as MainActivity
        if (requireContext().prefs().getString(C.USERNAME, "") != "") {
            viewModel.loadStream(useHelix = true, clientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""), token = requireContext().prefs().getString(C.TOKEN, ""), channelId = requireArguments().getString(C.CHANNEL_ID), channelLogin = requireArguments().getString(C.CHANNEL_LOGIN), channelName = requireArguments().getString(C.CHANNEL_DISPLAYNAME), profileImageURL = requireArguments().getString(C.CHANNEL_PROFILEIMAGE))
            viewModel.stream.observe(viewLifecycleOwner) { stream ->
                if (stream?.type?.lowercase() == "rerun") {
                    watchLive.text = getString(R.string.watch_rerun)
                    watchLive.setOnClickListener { activity.startStream(stream) }
                } else {
                    if (stream?.viewer_count != null) {
                        watchLive.text = getString(R.string.watch_live)
                        watchLive.setOnClickListener { activity.startStream(stream) }
                    } else {
                        watchLive.setOnClickListener { activity.startStream(Stream(user_id = requireArguments().getString(C.CHANNEL_ID), user_login = requireArguments().getString(C.CHANNEL_LOGIN), user_name = requireArguments().getString(C.CHANNEL_DISPLAYNAME), profileImageURL = requireArguments().getString(C.CHANNEL_PROFILEIMAGE))) }
                    }
                }
                stream?.channelLogo.let {
                    if (it != null) {
                        userLayout.visible()
                        userImage.visible()
                        userImage.loadImage(this, it, circle = true)
                        bundle.putString(C.CHANNEL_PROFILEIMAGE, it)
                        arguments = bundle
                    }
                }
                stream?.user_name.let {
                    if (it != null && it != requireArguments().getString(C.CHANNEL_DISPLAYNAME)) {
                        userLayout.visible()
                        userName.visible()
                        userName.text = it
                        bundle.putString(C.CHANNEL_DISPLAYNAME, it)
                        arguments = bundle
                    }
                }
                stream?.user_login.let {
                    if (it != null && it != requireArguments().getString(C.CHANNEL_LOGIN)) {
                        bundle.putString(C.CHANNEL_LOGIN, it)
                        arguments = bundle
                    }
                }
                if (stream?.title != null) {
                    streamLayout.visible()
                    title.visible()
                    title.text = stream.title.trim()
                }
                if (stream?.game_name != null) {
                    streamLayout.visible()
                    gameName.visible()
                    gameName.text = stream.game_name
                    if (stream.game_id != null) {
                        gameName.setOnClickListener { activity.openGame(stream.game_id, stream.game_name) }
                    }
                }
                if (stream?.viewer_count != null) {
                    streamLayout.visible()
                    viewers.visible()
                    viewers.text = TwitchApiHelper.formatViewersCount(requireContext(), stream.viewer_count!!, requireContext().prefs().getBoolean(C.UI_TRUNCATEVIEWCOUNT, false))
                }
                if (requireContext().prefs().getBoolean(C.UI_UPTIME, true)) {
                    if (stream?.started_at != null) {
                        TwitchApiHelper.getUptime(requireContext(), stream.started_at).let {
                            if (it != null)  {
                                streamLayout.visible()
                                uptime.visible()
                                uptime.text = it
                            }
                        }
                    }
                }
                if (requireArguments().getBoolean(C.CHANNEL_UPDATELOCAL) && stream != null) {
                    viewModel.updateLocalUser(requireContext(), stream)
                }
            }
            viewModel.loadUser(useHelix = true, clientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""), token = requireContext().prefs().getString(C.TOKEN, ""), channelId = requireArguments().getString(C.CHANNEL_ID))
            viewModel.user.observe(viewLifecycleOwner) { user ->
                if (user != null) {
                    if (user.created_at != null) {
                        userCreated.visible()
                        userCreated.text = requireContext().getString(R.string.created_at, TwitchApiHelper.formatTimeString(requireContext(), user.created_at))
                    }
                    if (user.followers_count != null) {
                        userFollowers.visible()
                        userFollowers.text = requireContext().getString(R.string.followers, TwitchApiHelper.formatCount(user.followers_count, requireContext().prefs().getBoolean(C.UI_TRUNCATEVIEWCOUNT, false)))
                    }
                    if (user.view_count != null) {
                        userViews.visible()
                        userViews.text = TwitchApiHelper.formatViewsCount(requireContext(), user.view_count, requireContext().prefs().getBoolean(C.UI_TRUNCATEVIEWCOUNT, false))
                    }
                    val broadcasterType = if (user.broadcaster_type != null) { TwitchApiHelper.getUserType(requireContext(), user.broadcaster_type) } else null
                    val type = if (user.type != null) { TwitchApiHelper.getUserType(requireContext(), user.type) } else null
                    val typeString = if (broadcasterType != null && type != null) "$broadcasterType, $type" else broadcasterType ?: type
                    if (typeString != null) {
                        userType.visible()
                        userType.text = typeString
                    }
                }
            }
            if (requireContext().prefs().getBoolean(C.UI_FOLLOW, true)) {
                initializeFollow(this, viewModel, follow, User.get(activity), context?.prefs()?.getString(C.HELIX_CLIENT_ID, ""))
            }
        } else {
            collapsingToolbar.expandedTitleMarginBottom = activity.convertDpToPixels(50.5f)
            watchLive.setOnClickListener { activity.startStream(Stream(user_id = requireArguments().getString(C.CHANNEL_ID), user_login = requireArguments().getString(C.CHANNEL_LOGIN), user_name = requireArguments().getString(C.CHANNEL_DISPLAYNAME), profileImageURL = requireArguments().getString(C.CHANNEL_PROFILEIMAGE))) }
        }
    }

    override fun onNetworkRestored() {
        if (requireContext().prefs().getBoolean(C.API_USEHELIX, true) && requireContext().prefs().getString(C.USERNAME, "") != "") {
            viewModel.retry(useHelix = true, clientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, ""), token = requireContext().prefs().getString(C.TOKEN, ""))
        } else {
            viewModel.retry(useHelix = false, clientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, ""))
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            appBar.setExpanded(false, false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3 && resultCode == Activity.RESULT_OK) {
            requireActivity().recreate()
        }
    }

    override fun scrollToTop() {
        appBar?.setExpanded(true, true)
        (currentFragment as? Scrollable)?.scrollToTop()
    }
}