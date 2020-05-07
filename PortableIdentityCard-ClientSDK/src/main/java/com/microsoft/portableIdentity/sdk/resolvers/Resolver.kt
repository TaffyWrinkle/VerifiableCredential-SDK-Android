/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package com.microsoft.portableIdentity.sdk.resolvers

import com.microsoft.portableIdentity.sdk.identifier.models.identifierdocument.IdentifierDocument
import com.microsoft.portableIdentity.sdk.repository.IdentifierRepository
import com.microsoft.portableIdentity.sdk.utilities.controlflow.ResolverException
import com.microsoft.portableIdentity.sdk.utilities.controlflow.Result
import com.microsoft.portableIdentity.sdk.utilities.controlflow.runResultTry
import javax.inject.Inject
import javax.inject.Named

class Resolver @Inject constructor(
    @Named("resolverUrl") private val baseUrl: String,
    private val identifierRepository: IdentifierRepository
) {
    suspend fun resolve(identifier: String): Result<IdentifierDocument> {
        return runResultTry {
            when (val id = identifierRepository.resolveIdentifier(baseUrl, identifier)) {
                is Result.Success -> {
                    Result.Success(id.payload.didDocument)
                }
                is Result.Failure -> Result.Failure(ResolverException("Unable to resolve identifier $identifier", id.payload))
            }
        }
    }
}