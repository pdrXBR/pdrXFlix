package com.pdrxflix.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.pdrxflix.databinding.FragmentHomeBinding
import com.pdrxflix.ui.AppNavigator
import com.pdrxflix.ui.adapters.ContinueAdapter
import com.pdrxflix.ui.adapters.MediaAdapter
import com.pdrxflix.ui.common.SpacesItemDecoration
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()
    private val continueAdapter = ContinueAdapter { record ->
        (activity as? AppNavigator)?.openPlayer(
            record.collectionId,
            record.videoPath,
            record.lastPositionMs,
            record.episodeIndex
        )
    }
    private val mediaAdapter = MediaAdapter { collection ->
        (activity as? AppNavigator)?.openDetails(collection.id)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerContinue.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = continueAdapter
            addItemDecoration(SpacesItemDecoration(horizontal = 24, vertical = 12))
            itemAnimator = null
        }

        binding.recyclerLibrary.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = mediaAdapter
            addItemDecoration(SpacesItemDecoration(horizontal = 20, vertical = 20))
            setHasFixedSize(true)
        }

        binding.searchInput.addTextChangedListener(SimpleTextWatcher { text ->
            viewModel.updateQuery(text)
        })

        binding.btnRefresh.setOnClickListener { refreshLibrary() }
        binding.btnSettings.setOnClickListener { (activity as? AppNavigator)?.openSettings() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progress.isVisible = state.loading
                binding.emptyState.isVisible = state.collections.isEmpty() && state.continueWatching.isEmpty() && !state.loading
                binding.continueHeader.isVisible = state.continueWatching.isNotEmpty()
                binding.recyclerContinue.isVisible = state.continueWatching.isNotEmpty()
                continueAdapter.submitList(state.continueWatching)
                mediaAdapter.submitList(state.collections)
            }
        }

        refreshLibrary()
    }

    fun refreshLibrary() {
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "HomeFragment"
    }
}
