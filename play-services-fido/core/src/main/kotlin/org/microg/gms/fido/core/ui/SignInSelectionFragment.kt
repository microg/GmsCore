/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.images.ImageManager
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import org.microg.gms.fido.core.CredentialUserInfo
import org.microg.gms.fido.core.Database
import org.microg.gms.fido.core.R
import org.microg.gms.fido.core.databinding.FidoSignInSelectionFragmentBinding
import org.microg.gms.fido.core.rpId
import org.microg.gms.fido.core.transport.Transport
import androidx.core.view.isGone

class SignInSelectionFragment : AuthenticatorActivityFragment() {
    private lateinit var binding: FidoSignInSelectionFragmentBinding

    private val database by lazy { Database(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FidoSignInSelectionFragmentBinding.inflate(inflater, container, false)
        binding.data = data
        binding.signInKeyRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.signInKeyBack.setOnClickListener { requireActivity().finish() }
        return binding.root.apply { isGone }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rpId = options?.rpId
        if (rpId.isNullOrEmpty()) {
            authenticatorActivity?.finishWithError(ErrorCode.UNKNOWN_ERR, "Missing rpId")
            return
        }
        val knownRegistrationInfo = database.getKnownRegistrationInfo(rpId)
        if (knownRegistrationInfo.isEmpty()) {
            findNavController().navigate(R.id.openWelcomeFragment)
        } else if (knownRegistrationInfo.size == 1) {
            val info = knownRegistrationInfo.first()
            startTransportHandling(info.transport, info.userJson)
        } else {
            binding.root.apply { isVisible }
            binding.signInKeyRecycler.adapter = SignInKeyAdapter(knownRegistrationInfo) { user, transport ->
                startTransportHandling(transport, user)
            }
        }
    }
}

internal class SignInKeyAdapter(val data: List<CredentialUserInfo>, val onKeyClick: (String, Transport) -> Unit) :
    RecyclerView.Adapter<SignInKeyAdapter.SignInHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignInHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fido_sign_in_item_layout, parent, false)
        return SignInHolder(view)
    }

    override fun onBindViewHolder(holder: SignInHolder, position: Int) {
        val item = data[position]
        val user = PublicKeyCredentialUserEntity.parseJson(item.userJson)
        holder.signInKeyName.text = user.displayName
        holder.signInKeyEmail.text = user.name
        user.icon?.let { ImageManager.create(holder.itemView.context).loadImage(it, holder.signInKeyLogo) }
        holder.itemView.setOnClickListener { onKeyClick(item.userJson, item.transport) }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class SignInHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val signInKeyLogo: ImageView = itemView.findViewById(R.id.sign_in_key_logo)
        val signInKeyName: TextView = itemView.findViewById(R.id.sign_in_key_name)
        val signInKeyEmail: TextView = itemView.findViewById(R.id.sign_in_key_email)
    }
}
