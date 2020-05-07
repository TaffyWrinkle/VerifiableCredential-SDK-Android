/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package com.microsoft.portableIdentity.sdk.auth.protectors

import com.microsoft.portableIdentity.sdk.auth.models.oidc.AttestationResponse
import com.microsoft.portableIdentity.sdk.utilities.Constants
import com.microsoft.portableIdentity.sdk.auth.models.oidc.OidcResponseContent
import com.microsoft.portableIdentity.sdk.auth.responses.IssuanceResponse
import com.microsoft.portableIdentity.sdk.auth.responses.PresentationResponse
import com.microsoft.portableIdentity.sdk.auth.responses.Response
import com.microsoft.portableIdentity.sdk.cards.PortableIdentityCard
import com.microsoft.portableIdentity.sdk.cards.verifiableCredential.VerifiablePresentationContent
import com.microsoft.portableIdentity.sdk.cards.verifiableCredential.VerifiablePresentationDescriptor
import com.microsoft.portableIdentity.sdk.crypto.CryptoOperations
import com.microsoft.portableIdentity.sdk.crypto.models.Sha
import com.microsoft.portableIdentity.sdk.identifier.Identifier
import com.microsoft.portableIdentity.sdk.utilities.Constants.VERIFIABLE_PRESENTATION_TYPE
import com.microsoft.portableIdentity.sdk.utilities.Constants.VP_CONTEXT_URL
import com.microsoft.portableIdentity.sdk.utilities.Serializer
import com.microsoft.portableIdentity.sdk.utilities.controlflow.CryptoException
import com.microsoft.portableIdentity.sdk.utilities.controlflow.Result
import java.lang.Exception
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor

/**
 * Class that forms Response Contents Properly.
 */
@Singleton
class OidcResponseFormatter @Inject constructor(
    private val cryptoOperations: CryptoOperations,
    private val serializer: Serializer,
    private val signer: TokenSigner
) : Formatter {

    override fun formAndSignResponse(response: Response, responder: Identifier, expiresIn: Int): Result<String> {

        return try {
            val contents = formContents(response, responder, expiresIn)
            val signedToken = signContents(contents, responder)
            Result.Success(signedToken)
        } catch (exception: Exception) {
            Result.Failure(CryptoException("Unable to sign response contents", exception))
        }
    }

    private fun signContents(contents: OidcResponseContent, responder: Identifier): String {
        val serializedResponseContent = serializer.stringify(OidcResponseContent.serializer(), contents)
        return signer.signWithIdentifier(serializedResponseContent, responder)
    }

    private fun formContents(response: Response, responder: Identifier, expiresIn: Int = Constants.RESPONSE_EXPIRATION_IN_MINUTES): OidcResponseContent {
        val (iat, exp) = createIatAndExp(expiresIn)
        val key = cryptoOperations.keyStore.getPublicKey(responder.signatureKeyReference).getKey()
        val jti = UUID.randomUUID().toString()
        val did = responder.id

        var contract: String? = null
        var nonce: String? = null
        var state: String? = null

        when (response) {
            is IssuanceResponse -> {
                contract = response.request.contractUrl
            }
            is PresentationResponse -> {
                nonce = response.request.content.nonce
                state = response.request.content.state
            }
        }

        val attestationResponse = createAttestationResponse(response, responder, iat, exp)

        return OidcResponseContent(
            sub = key.getThumbprint(cryptoOperations, Sha.Sha256),
            aud = response.audience,
            nonce = nonce,
            did = did,
            subJwk = key.toJWK(),
            iat = iat,
            exp = exp,
            state = state,
            jti = jti,
            contract = contract,
            attestations = attestationResponse
        )
    }

    private fun createAttestationResponse(response: Response, responder: Identifier, iat: Long, exp: Long): AttestationResponse {
        var selfIssuedAttestations: Map<String, String>? = null
        var tokenAttestations: Map<String, String>? = null
        if (!response.getCollectedIdTokens().isNullOrEmpty()) {
            tokenAttestations = response.getCollectedIdTokens()
        }
        if (!response.getCollectedSelfIssuedClaims().isNullOrEmpty()) {
            selfIssuedAttestations = response.getCollectedSelfIssuedClaims()
        }
        val presentationAttestation = createPresentations(response.getCollectedCards(), response, responder, iat, exp)
        return AttestationResponse(selfIssuedAttestations, tokenAttestations, presentationAttestation)
    }

    private fun createPresentations(typeToCardsMapping: Map<String, PortableIdentityCard>, response: Response, responder: Identifier, iat: Long, exp: Long): Map<String, String>? {
        val presentations = mutableMapOf<String, String>()
        typeToCardsMapping.forEach {
            presentations[it.key] = createPresentation(it.value, response, responder, iat, exp)
        }
        if (presentations.isEmpty()) {
            return null
        }
        return presentations
    }

    // only support one VC per VP
    private fun createPresentation(card: PortableIdentityCard, response: Response, responder: Identifier, iat: Long, exp: Long): String {
        val vp = VerifiablePresentationDescriptor(verifiableCredential = listOf(card.verifiableCredential.raw),
                                                  context = listOf(VP_CONTEXT_URL),
                                                  type = listOf(VERIFIABLE_PRESENTATION_TYPE))
        val jti = UUID.randomUUID().toString()
        val did = responder.id
        val contents = VerifiablePresentationContent(
            jti = jti,
            vp = vp,
            iss = did,
            iat = iat,
            nbf = iat,
            exp = exp,
            aud = response.request.entityIdentifier
        )
        val serializedContents = serializer.stringify(VerifiablePresentationContent.serializer(), contents)
        return signer.signWithIdentifier(serializedContents, responder)
    }

    private fun createIatAndExp(expiresIn: Int = Constants.RESPONSE_EXPIRATION_IN_MINUTES): Pair<Long, Long> {
        val currentTime = Date().time
        val expiresInMilliseconds = 1000 * 60 * expiresIn
        val expiration = currentTime + expiresInMilliseconds.toLong()
        val exp = floor(expiration / 1000f).toLong()
        val iat = floor(currentTime / 1000f).toLong()
        return Pair(iat, exp)
    }
}