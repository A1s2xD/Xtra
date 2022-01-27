package com.github.andreyasadchy.xtra.ui.common

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.repository.LoadingState
import com.github.andreyasadchy.xtra.util.gone
import kotlinx.android.synthetic.main.common_recycler_view_layout.*

abstract class PagedListFragment<T, VM : PagedListViewModel<T>, Adapter : BasePagedListAdapter<T>> : BaseNetworkFragment() {

    protected abstract val viewModel: VM
    protected abstract val adapter: Adapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                adapter.unregisterAdapterDataObserver(this)
                adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                        try {
                            if (positionStart == 0) {
                                recyclerView?.scrollToPosition(0)
                            }
                        } catch (e: Exception) {

                        }
                    }
                })
            }
        })
        recyclerView.adapter = adapter
    }

    override fun initialize() {
        viewModel.list.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
            nothingHere.isVisible = it.isEmpty()
        })
        viewModel.loadingState.observe(viewLifecycleOwner, Observer {
            val isLoading = it == LoadingState.LOADING
            val isListEmpty = adapter.currentList.isNullOrEmpty()
            if (isLoading) {
                nothingHere.gone()
            }
            progressBar.isVisible = isLoading && isListEmpty
            if (swipeRefresh.isEnabled) {
                swipeRefresh.isRefreshing = isLoading && !isListEmpty
            }
        })
        viewModel.pagingState.observe(viewLifecycleOwner, Observer(adapter::setPagingState))
        if (swipeRefresh.isEnabled) {
            swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        }
    }

    override fun onNetworkRestored() {
        viewModel.retry()
    }
}