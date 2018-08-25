package me.saket.expand

import me.saket.expand.page.ExpandablePageLayout

abstract class ItemExpandAnimator {

  lateinit var page: ExpandablePageLayout
  lateinit var recyclerView: InboxRecyclerView

  abstract fun onPageDetached(page: ExpandablePageLayout)

  abstract fun onPageAttached()
}

// TODO: Find a better name.
// TODO: Combine attach and detach into a single function.
class DefaultItemExpandAnimator : ItemExpandAnimator() {

  private val pagePreDrawListener = {
    onPageMove()
    true
  }

  private val pageLayoutChangeListener = {
    onPageMove()
  }

  override fun onPageDetached(page: ExpandablePageLayout) {
    page.viewTreeObserver.removeOnGlobalLayoutListener(pageLayoutChangeListener)
    page.viewTreeObserver.removeOnPreDrawListener(pagePreDrawListener)
  }

  override fun onPageAttached() {
    page.viewTreeObserver.addOnGlobalLayoutListener(pageLayoutChangeListener)
    page.viewTreeObserver.addOnPreDrawListener(pagePreDrawListener)
  }

  private fun onPageMove() {
    if (page.isCollapsed) {
      // Reset everything. This is also useful when the content size
      // changes, say as a result of the soft-keyboard getting dismissed.
      recyclerView.apply {
        for (childIndex in 0 until childCount) {
          val childView = getChildAt(childIndex)
          childView.translationY = 0F
          childView.alpha = 1F
        }
      }
      return
    }

    val (anchorPosition) = recyclerView.getExpandInfo()
    val anchorView = recyclerView.getChildAt(anchorPosition)

    val pageTop = page.translationY
    val pageBottom = page.translationY + page.clippedRect.height()

    val distanceExpandedTowardsTop = pageTop - anchorView.top
    val distanceExpandedTowardsBottom = pageBottom - anchorView.bottom

    // Move the RecyclerView rows with the page.
    recyclerView.apply {
      for (childIndex in 0 until childCount) {
        val childView = getChildAt(childIndex)

        if (anchorView == null) {
          // Anchor View can be null when the page was expanded from
          // an arbitrary location. See InboxRecyclerView#expandFromTop().
          childView.translationY = pageBottom

        } else {
          childView.translationY = when {
            childIndex <= anchorPosition -> distanceExpandedTowardsTop
            else -> distanceExpandedTowardsBottom
          }
        }
      }
    }

    // Fade in the anchor row with the expanding/collapsing page.
    val minPageHeight = anchorView.height
    val maxPageHeight = page.height
    val expandRatio = (page.clippedRect.height() - minPageHeight).toFloat() / (maxPageHeight - minPageHeight)
    anchorView.alpha = 1F - expandRatio
  }
}
