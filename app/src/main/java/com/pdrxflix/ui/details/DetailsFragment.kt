package com.pdrxflix.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.pdrxflix.R
import com.pdrxflix.databinding.FragmentDetailsBinding
import com.pdrxflix.ui.AppNavigator
import com.pdrxflix.ui.adapters.EpisodeAdapter
import com.pdrxflix.ui.common.SpacesItemDecoration

class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailsViewModel by viewModels()
    
    // O Adapter agora vai cuidar do clique no episódio
    private val episodeAdapter = EpisodeAdapter { episode ->
        (activity as? AppNavigator)?.openPlayer(
            collectionId = collectionId,
            videoPath = episode.filePath,
            startPositionMs = 0L,
            episodeIndex = episode.episodeIndex
        )
    }

    private val collectionId: Long by lazy {
        requireArguments().getLong(ARG_COLLECTION_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerEpisodes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = episodeAdapter
            // Mantive a decoração, mas talvez você queira ajustar os espaços depois
            addItemDecoration(SpacesItemDecoration(horizontal = 18, vertical = 18))
        }

        binding.btnBack.setOnClickListener { (activity as? AppNavigator)?.closeTransientScreen() }
        
        binding.btnPlayAll.setOnClickListener {
            // Play no primeiríssimo episódio disponível
            val first = viewModel.selectedCollection.value?.videos?.firstOrNull() ?: return@setOnClickListener
            (activity as? AppNavigator)?.openPlayer(collectionId, first.filePath, 0L, first.episodeIndex)
        }

        viewModel.load(collectionId)

        viewModel.selectedCollection.observe(viewLifecycleOwner) { collection ->
            if (collection == null) {
                binding.emptyState.visibility = View.VISIBLE
                return@observe
            }
            binding.emptyState.visibility = View.GONE
            binding.title.text = collection.title
            binding.episodeCount.text = getString(R.string.episodes_count, collection.itemCount)
            
            Glide.with(this)
                .load(collection.coverPath ?: R.drawable.ic_placeholder_cover)
                .placeholder(R.drawable.ic_placeholder_cover)
                .centerCrop()
                .into(binding.coverImage)

            // AQUI ESTÁ A MÁGICA:
            // Enviamos a coleção inteira para o adapter, e ele vai se organizar sozinho
            episodeAdapter.updateData(collection)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DetailsFragment"
        private const val ARG_COLLECTION_ID = "collection_id"

        fun newInstance(collectionId: Long): DetailsFragment {
            return DetailsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_COLLECTION_ID, collectionId)
                }
            }
        }
    }
}
