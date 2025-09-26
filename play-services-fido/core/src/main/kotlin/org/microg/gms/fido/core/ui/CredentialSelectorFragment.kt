package org.microg.gms.fido.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.gms.fido.fido2.api.common.AuthenticatorResponse
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import kotlinx.coroutines.launch
import org.microg.gms.fido.core.AuthenticatorResponseWrapper
import org.microg.gms.fido.core.UserInfo
import org.microg.gms.fido.core.R
import org.microg.gms.fido.core.databinding.FidoCredentialSelectorFragmentBinding
import org.microg.gms.fido.core.databinding.FidoCredentialSelectorListItemBinding
import org.microg.gms.fido.core.transport.Transport

class CredentialListAdapter(
    private val responseWrapper: AuthenticatorResponseWrapper,
    private val listSelectionFunction: (suspend () -> AuthenticatorResponse) -> Unit,
    private val deleteCredentialFunction: (suspend () -> Boolean) -> Unit
) : BaseAdapter() {
    override fun getCount(): Int {
        return responseWrapper.responseChoices.size
    }

    override fun getItem(position: Int): Pair<UserInfo?, suspend () -> AuthenticatorResponse> {
        return responseWrapper.responseChoices[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val inflater = LayoutInflater.from(parent?.context)
        if (convertView == null) {
            view = inflater.inflate(R.layout.fido_credential_selector_list_item, parent, false)
        }

        val (userInfo, function) = getItem(position)
        // TODO: Set icons
        if (userInfo != null) {
            view?.findViewById<TextView>(R.id.credentialNameTextView)?.setText(userInfo.name)
            if (userInfo.displayName != null) view?.findViewById<TextView>(R.id.credentialDisplayNameTextView)?.setText(userInfo.displayName)
        }

        val binding = FidoCredentialSelectorListItemBinding.bind(view!!)
        binding.onCredentialSelection = View.OnClickListener {
            view.setBackgroundColor(ContextCompat.getColor(it.context, androidx.appcompat.R.color.abc_color_highlight_material))
            listSelectionFunction(function)
        }

        if (responseWrapper.deleteFunctions.size == responseWrapper.responseChoices.size) {
            val deleteButton = view.findViewById<Button>(R.id.deleteCredentialButton)
            deleteButton.visibility = View.VISIBLE

            binding.onDeleteCredential = View.OnClickListener {
                deleteCredentialFunction(responseWrapper.deleteFunctions[position])
            }
        }

        return view
    }
}

class CredentialSelectorFragment : AuthenticatorActivityFragment() {
    private lateinit var binding: FidoCredentialSelectorFragmentBinding
    private lateinit var transport: Transport

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FidoCredentialSelectorFragmentBinding.inflate(inflater, container, false)

        transport = arguments?.get("transport") as Transport
        val responseWrapper = arguments?.get("responseWrapper") as AuthenticatorResponseWrapper
        val adapter = CredentialListAdapter(responseWrapper, this::onListSelection, this::credentialDeletion)
        binding.fidoCredentialListView.adapter = adapter

        return binding.root
    }

    fun onListSelection(function: suspend () -> AuthenticatorResponse) {
        val authenticator = authenticatorActivity
        authenticator?.lifecycleScope?.launch {
            try {
                authenticator.finishWithSuccessResponse(AuthenticatorResponseWrapper(listOf(null to function)), transport)
            } catch (e: Exception) {
                authenticator.finishWithError(ErrorCode.UNKNOWN_ERR, e.message ?: e.javaClass.simpleName)
            }
        }
        if (!findNavController().navigateUp()) {
            findNavController().navigate(
                R.id.transportSelectionFragment,
                arguments,
                navOptions { popUpTo(R.id.usbFragment) { inclusive = true } })
        }
    }

    fun credentialDeletion(deleteFunction: suspend () -> Boolean) {
        binding.fidoCredentialListView.isEnabled = false
        authenticatorActivity?.lifecycleScope?.launch {
            val deletionSucceeded = deleteFunction.invoke()

            // If credential is deleted, make leave the fragment, since the list is no longer valid
            // There is probably some way to update the list, but it doesn't seem to work from inside
            // the authenticatorActivity's lifecycleScope
            if (deletionSucceeded && findNavController().currentDestination?.id == R.id.credentialSelectorFragment) {
                findNavController().navigateUp()
            }
        }
    }
}