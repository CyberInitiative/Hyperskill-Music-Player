package org.hyperskill.musicplayer

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText

class MainAddPlaylistFragment : Fragment() {

    private lateinit var addPlaylistEtPlaylistName: EditText

    private lateinit var addPlaylistBtnCancel: Button
    private lateinit var addPlaylistBtnOk: Button

    private lateinit var listener: OnMainAddPlaylistFragmentButtonsClicksListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main_add_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addPlaylistEtPlaylistName = view.findViewById(R.id.addPlaylistEtPlaylistName)

        addPlaylistBtnCancel = view.findViewById(R.id.addPlaylistBtnCancel)
        addPlaylistBtnOk = view.findViewById(R.id.addPlaylistBtnOk)

        setButtonsListeners()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnMainAddPlaylistFragmentButtonsClicksListener
        } catch (ex: ClassCastException) {
            throw ClassCastException("${context.toString()} " +
                    "must implement MainAddPlaylistFragment.OnMainAddPlaylistFragmentButtonsClicksListener")
        }
    }

    private fun setButtonsListeners() {
        addPlaylistBtnCancel.setOnClickListener {
            listener.onAddPlaylistBtnCancelButtonClick()
        }

        addPlaylistBtnOk.setOnClickListener {
            listener.onAddPlaylistBtnOkButtonClick(addPlaylistEtPlaylistName.text.toString())
        }
    }

    interface OnMainAddPlaylistFragmentButtonsClicksListener {
        fun onAddPlaylistBtnCancelButtonClick()
        fun onAddPlaylistBtnOkButtonClick(playlistName: String)
    }
}