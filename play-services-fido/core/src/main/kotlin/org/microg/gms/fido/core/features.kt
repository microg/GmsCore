package org.microg.gms.fido.core

import com.google.android.gms.common.Feature

val FEATURES = arrayOf(
    Feature("cancel_target_direct_transfer", 1),
    Feature("delete_credential", 1),
    Feature("delete_device_public_key", 1),
    Feature("get_or_generate_device_public_key", 1),
    Feature("get_passkeys", 1),
    Feature("update_passkey", 1),
    Feature("is_user_verifying_platform_authenticator_available_for_credential", 1),
    Feature("is_user_verifying_platform_authenticator_available", 1),
    Feature("privileged_api_list_credentials", 2),
    Feature("start_target_direct_transfer", 1),
    Feature("first_party_api_get_link_info", 1),
    Feature("get_browser_hybrid_client_sign_pending_intent", 1),
    Feature("get_browser_hybrid_client_registration_pending_intent", 1),
    Feature("privileged_authenticate_passkey", 1),
    Feature("privileged_register_passkey_with_sync_account", 1)
)