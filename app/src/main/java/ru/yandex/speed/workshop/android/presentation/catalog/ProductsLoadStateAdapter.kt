package ru.yandex.speed.workshop.android.presentation.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.yandex.speed.workshop.android.R

class ProductsLoadStateAdapter(
    private val onRetry: () -> Unit,
) : LoadStateAdapter<ProductsLoadStateAdapter.LoadStateViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState,
    ): LoadStateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_loading, parent, false)
        return LoadStateViewHolder(view, onRetry)
    }

    override fun onBindViewHolder(
        holder: LoadStateViewHolder,
        loadState: LoadState,
    ) {
        holder.bind(loadState)
    }

    class LoadStateViewHolder(itemView: View, onRetry: () -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val retryButton: Button = itemView.findViewById(R.id.retryButton)

        init {
            retryButton.setOnClickListener { onRetry() }
        }

        fun bind(loadState: LoadState) {
            when (loadState) {
                is LoadState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    messageText.visibility = View.VISIBLE
                    messageText.text = itemView.context.getString(R.string.loading)
                    retryButton.visibility = View.GONE
                }
                is LoadState.Error -> {
                    progressBar.visibility = View.GONE
                    messageText.visibility = View.VISIBLE
                    messageText.text = loadState.error.localizedMessage ?: itemView.context.getString(R.string.loading_error)
                    retryButton.visibility = View.VISIBLE
                }
                is LoadState.NotLoading -> {
                    progressBar.visibility = View.GONE
                    messageText.visibility = View.GONE
                    retryButton.visibility = View.GONE
                }
            }
        }
    }
}
