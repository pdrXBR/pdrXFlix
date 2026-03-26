package com.pdrxflix.ui.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpacesItemDecoration(
    private val horizontal: Int,
    private val vertical: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.set(horizontal / 2, vertical / 2, horizontal / 2, vertical / 2)
    }
}
