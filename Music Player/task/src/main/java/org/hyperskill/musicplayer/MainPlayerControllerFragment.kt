package org.hyperskill.musicplayer

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.hyperskill.musicplayer.model.TrackItem
import org.hyperskill.musicplayer.viewmodel.PlayerViewModel

class MainPlayerControllerFragment : Fragment() {

    private lateinit var controllerBtnPlayPause: Button
    private lateinit var controllerBtnStop: Button

    private lateinit var controllerTvCurrentTime: TextView
    private lateinit var controllerTvTotalTime: TextView

    private lateinit var controllerSeekBar: SeekBar

    private lateinit var listener: OnMainPlayerControllerFragmentButtonsClicksListener

    private lateinit var viewModel: PlayerViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main_player_controller, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(PlayerViewModel::class.java)

        initializeViews(view)
        setButtonsListeners()
        setOnControllerSeekBarChangeListener()
        setCurrentTrackObserver()

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnMainPlayerControllerFragmentButtonsClicksListener
        } catch (ex: ClassCastException) {
            throw ClassCastException(
                "${context.toString()} " +
                        "must implement MainPlayerControllerFragment.OnMainPlayerControllerFragmentButtonsClicksListener"
            )
        }
    }

    private fun initializeViews(view: View) {
        controllerBtnPlayPause = view.findViewById(R.id.controllerBtnPlayPause)
        controllerBtnStop = view.findViewById(R.id.controllerBtnStop)

        controllerSeekBar = view.findViewById(R.id.controllerSeekBar)

        controllerTvCurrentTime = view.findViewById(R.id.controllerTvCurrentTime)
        controllerTvTotalTime = view.findViewById(R.id.controllerTvTotalTime)
    }

    private fun setButtonsListeners() {
        controllerBtnPlayPause.setOnClickListener {
            listener.onControllerBtnPlayPauseButtonClick()
        }

        controllerBtnStop.setOnClickListener {
            controllerSeekBar.progress = 0
            controllerTvCurrentTime.text =
                requireActivity().resources.getString(R.string.song_duration_time, 0, 0)

            listener.onControllerBtnStopButtonClick()
        }
    }

    private fun setCurrentTrackObserver() {
        viewModel.currentTrackItem.observe(requireActivity(), Observer {
            if (it != null) {
//                Toast.makeText(requireActivity(), "RECEIVED", Toast.LENGTH_LONG).show()
                controllerTvTotalTime.text = formatTime(it.song.durationInMilliseconds.toInt())
                val seconds = (it.song.durationInMilliseconds / 1000).toInt()
                controllerSeekBar.max = seconds
//                controllerSeekBar.progress = 0
            }
        })
    }

    private fun setOnControllerSeekBarChangeListener() {

        viewModel.currentPosition.observe(viewLifecycleOwner) { position ->
            controllerSeekBar.progress = position / 1000 // Convert to seconds
            println("POSITION ${position / 1000}")
            controllerTvCurrentTime.text = formatTime(position)
        }

        controllerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    controllerTvCurrentTime.text =
                        formatTime(progress * 1000) // Convert to milliseconds
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                viewModel.stopUpdatingPosition()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                viewModel.mediaPlayer?.seekTo(controllerSeekBar.progress * 1000)
                viewModel.startUpdatingPosition()

                if (viewModel.isMediaPlayerCompleted) {
                    viewModel.mediaPlayer?.start()
                    viewModel.isMediaPlayerCompleted = false
                }
            }
        })

    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / 1000) / 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    interface OnMainPlayerControllerFragmentButtonsClicksListener {
        fun onControllerBtnPlayPauseButtonClick()
        fun onControllerBtnStopButtonClick()
    }
}