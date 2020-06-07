package com.microsoft.did.sdk.auth.protectors

import com.microsoft.did.sdk.auth.models.verifiablePresentation.VerifiablePresentationContent
import com.microsoft.did.sdk.auth.models.verifiablePresentation.VerifiablePresentationDescriptor
import com.microsoft.did.sdk.cards.verifiableCredential.VerifiableCredential
import com.microsoft.did.sdk.identifier.Identifier
import com.microsoft.did.sdk.utilities.Constants
import com.microsoft.did.sdk.utilities.serializer.Serializer
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerifiablePresentationFormatter @Inject constructor(
    private val serializer: Serializer,
    private val signer: TokenSigner
) {

    // only support one VC per VP
    fun createPresentation(
        verifiableCredentials: List<VerifiableCredential>,
        audience: String,
        responder: Identifier,
        expiresIn: Int
    ): String {
        val vp = VerifiablePresentationDescriptor(
            verifiableCredential = verifiableCredentials.map { it.raw },
            context = listOf(Constants.VP_CONTEXT_URL),
            type = listOf(Constants.VERIFIABLE_PRESENTATION_TYPE)
        )

        val (iat, exp) = createIatAndExp(expiresIn)
        val jti = UUID.randomUUID().toString()
        val did = responder.id
        val contents =
            VerifiablePresentationContent(
                jti = jti,
                vp = vp,
                iss = did,
                iat = iat,
                nbf = iat,
                exp = exp,
                aud = audience
            )
        val serializedContents = serializer.stringify(VerifiablePresentationContent.serializer(), contents)
        return signer.signWithIdentifier(serializedContents, responder)
    }
}