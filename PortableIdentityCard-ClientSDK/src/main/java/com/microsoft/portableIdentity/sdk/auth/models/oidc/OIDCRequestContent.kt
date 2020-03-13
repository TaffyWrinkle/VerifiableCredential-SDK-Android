package com.microsoft.portableIdentity.sdk.auth.models.oidc

import com.microsoft.portableIdentity.sdk.auth.credentialRequests.CredentialRequests
import com.microsoft.portableIdentity.sdk.auth.models.RequestContent
import com.microsoft.portableIdentity.sdk.credentials.deprecated.ClaimObject
import com.microsoft.portableIdentity.sdk.crypto.keys.PublicKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OIDCRequestContent(
    val iss: String? = null,
    val aud: String? = null,
    @SerialName("response_type")
    val responseType: String? = null,
    @SerialName("response_mode")
    val responseMode: String? = null,
    @SerialName("client_id")
    val clientId: String? = null,
    @SerialName("redirect_uri")
    override val responseUri: String = "",
    val scope: String? = null,
    val state: String? = null,
    val nonce: String? = null,
    @SerialName("max_age")
    val maxAge: Int? = null,
    val claims: RequestClaimParameter? = null,
    val registration: Registration? = null,
    // custom parameters
    @SerialName("offer")
    val claimsOffered: ClaimObject? = null
): RequestContent {
    override fun getCredentialRequests(): CredentialRequests {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isValid(): Boolean {
        TODO("need to check exp and such") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPublicKeys(): List<PublicKey> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}