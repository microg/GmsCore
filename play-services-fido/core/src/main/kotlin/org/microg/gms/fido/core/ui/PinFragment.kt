package org.microg.gms.fido.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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

        return binding.root
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