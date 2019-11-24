package com.maxfour.music.activities

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.maxfour.appthemehelper.ThemeStore
import com.maxfour.appthemehelper.util.ATHUtil
import com.maxfour.appthemehelper.util.ColorUtil
import com.maxfour.appthemehelper.util.MaterialValueHelper
import com.maxfour.music.R
import com.maxfour.music.activities.base.AbsMusicServiceActivity
import com.maxfour.music.adapter.song.PlayingQueueAdapter
import com.maxfour.music.extensions.applyToolbar
import com.maxfour.music.helper.MusicPlayerRemote
import com.maxfour.music.util.MusicUtil
import com.maxfour.music.util.ViewUtil
import kotlinx.android.synthetic.main.activity_playing_queue.*

open class PlayingQueueActivity : AbsMusicServiceActivity() {

	private var wrappedAdapter: RecyclerView.Adapter<*>? = null
	private var recyclerViewDragDropManager: RecyclerViewDragDropManager? = null
	private var playingQueueAdapter: PlayingQueueAdapter? = null
	private lateinit var linearLayoutManager: LinearLayoutManager

	private fun getUpNextAndQueueTime(): String {
		val duration = MusicPlayerRemote.getQueueDurationMillis(MusicPlayerRemote.position)

		return MusicUtil.buildInfoString(
				resources.getString(R.string.up_next), MusicUtil.getReadableDurationString(duration)
		)
	}

	override fun onCreate(
			savedInstanceState: Bundle?
	) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_playing_queue)

		setStatusbarColorAuto()
		setNavigationBarColorPrimary()
		setTaskDescriptionColorAuto()
		setLightNavigationBar(true)

		setupToolbar()
		setUpRecyclerView()

		clearQueue.setOnClickListener {
			MusicPlayerRemote.clearQueue()
		}
		checkForPadding()
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			android.R.id.home -> {
				onBackPressed()
				true
			}
			else              -> super.onOptionsItemSelected(item)
		}
	}

	private fun setUpRecyclerView() {
		recyclerViewDragDropManager = RecyclerViewDragDropManager()
		val animator = RefactoredDefaultItemAnimator()

		playingQueueAdapter = PlayingQueueAdapter(
				this,
				MusicPlayerRemote.playingQueue,
				MusicPlayerRemote.position,
				R.layout.item_queue
		)
		wrappedAdapter = recyclerViewDragDropManager?.createWrappedAdapter(playingQueueAdapter!!)

		linearLayoutManager = LinearLayoutManager(this)

		recyclerView.apply {
			layoutManager = linearLayoutManager
			adapter = wrappedAdapter
			itemAnimator = animator
			recyclerViewDragDropManager?.attachRecyclerView(this)
		}

		linearLayoutManager.scrollToPositionWithOffset(MusicPlayerRemote.position + 1, 0)

		recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				super.onScrolled(recyclerView, dx, dy)
				if (dy > 0) {
					clearQueue.shrink()
				} else if (dy < 0) {
					clearQueue.extend()
				}
			}
		})
		ViewUtil.setUpFastScrollRecyclerViewColor(this, recyclerView)
	}

	private fun checkForPadding() {

	}

	override fun onQueueChanged() {
		if (MusicPlayerRemote.playingQueue.isEmpty()) {
			finish()
			return
		}
		checkForPadding()
		updateQueue()
		updateCurrentSong()
	}

	override fun onMediaStoreChanged() {
		updateQueue()
		updateCurrentSong()
	}

	private fun updateCurrentSong() {
		playerQueueSubHeader.text = getUpNextAndQueueTime()
	}

	override fun onPlayingMetaChanged() {
		updateQueuePosition()
	}

	private fun updateQueuePosition() {
		playingQueueAdapter?.setCurrent(MusicPlayerRemote.position)
		resetToCurrentPosition()
		playerQueueSubHeader.text = getUpNextAndQueueTime()
	}

	private fun updateQueue() {
		playingQueueAdapter?.swapDataSet(MusicPlayerRemote.playingQueue, MusicPlayerRemote.position)
		resetToCurrentPosition()
	}

	private fun resetToCurrentPosition() {
		recyclerView.stopScroll()
		linearLayoutManager.scrollToPositionWithOffset(MusicPlayerRemote.position + 1, 0)
	}

	override fun onPause() {
		if (recyclerViewDragDropManager != null) {
			recyclerViewDragDropManager!!.cancelDrag()
		}
		super.onPause()
	}

	override fun onDestroy() {
		if (recyclerViewDragDropManager != null) {
			recyclerViewDragDropManager!!.release()
			recyclerViewDragDropManager = null
		}

		if (wrappedAdapter != null) {
			WrapperAdapterUtils.releaseAll(wrappedAdapter)
			wrappedAdapter = null
		}
		playingQueueAdapter = null
		super.onDestroy()
	}

	private fun setupToolbar() {
		playerQueueSubHeader.text = getUpNextAndQueueTime()
		playerQueueSubHeader.setTextColor(ThemeStore.accentColor(this))

		applyToolbar(toolbar)
		appBarLayout.setBackgroundColor(ATHUtil.resolveColor(this, R.attr.colorPrimary))

		clearQueue.backgroundTintList = ColorStateList.valueOf(ThemeStore.accentColor(this))
		ColorStateList.valueOf(
				MaterialValueHelper.getPrimaryTextColor(
						this,
						ColorUtil.isColorLight(
								ThemeStore.accentColor(
										this
								)
						)
				)
		).apply {
			clearQueue.setTextColor(this)
			clearQueue.iconTint = this
		}
	}
}