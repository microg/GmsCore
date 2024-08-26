package org.microg.gms.fido.core.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.core.content.getSystemService
import androidx.databinding.adapters.TextViewBindingAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import org.microg.gms.fido.core.R
import org.microg.gms.fido.core.databinding.FidoPinFragmentBinding

class AuthenticatorPinViewModel : ViewModel() {
    var pinRequest: Boolean = false
    var pin: String? = null
}

class PinFragment: AuthenticatorActivityFragment() {
    private lateinit var binding: FidoPinFragmentBinding
    private val pinViewModel: AuthenticatorPinViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FidoPinFragmentBinding.inflate(inflater, container, false)
        binding.onCancel = View.OnClickListener {
            leaveFragment()
        }
        binding.onEnterPin = View.OnClickListener {
            enterPin()
        }
        binding.onInputChange = TextViewBindingAdapter.AfterTextChanged {
            view?.findViewById<Button>(R.id.pin_fragment_ok)?.isEnabled = it.toString().encodeToByteArray().size in 4..63
        }
        binding.root.findViewById<EditText>(R.id.pin_editor)?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE &&
                (event == null || event.action == KeyEvent.ACTION_DOWN) &&
                v.text.toString().encodeToByteArray().size in 4 ..63) {
                enterPin()
                true
            } else {
                false
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<EditText>(R.id.pin_editor)?.let { editText ->
            requireContext().getSystemService<InputMethodManager>()?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun enterPin () {
        val textEditor = view?.findViewById<EditText>(R.id.pin_editor)
        if (textEditor != null) {
            pinViewModel.pin = textEditor.text.toString()
        }
        leaveFragment()
    }

    fun leaveFragment() {
        pinViewModel.pinRequest = true
        if (!findNavController().navigateUp()) {
            findNavController().navigate(
                R.id.transportSelectionFragment,
                arguments,
                navOptions { popUpTo(R.id.usbFragment) { inclusive = true } })
        }
    }
}