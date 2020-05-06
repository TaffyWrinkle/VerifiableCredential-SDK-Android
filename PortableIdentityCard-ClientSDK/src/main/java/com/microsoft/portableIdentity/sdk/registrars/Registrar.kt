/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package com.microsoft.portableIdentity.sdk.registrars

import com.microsoft.portableIdentity.sdk.crypto.CryptoOperations
import com.microsoft.portableIdentity.sdk.identifier.Identifier
import com.microsoft.portableIdentity.sdk.repository.IdentifierRepository
import com.microsoft.portableIdentity.sdk.utilities.Serializer
import com.microsoft.portableIdentity.sdk.utilities.controlflow.Result

/**
 * @interface defining methods and properties
 * to be implemented by specific registration methods.
 */
abstract class Registrar {

    /**
     * @return Identifier that was created.
     * @throws Exception if unable to create an Identifier.
     */
    abstract suspend fun register(identifier: Identifier): Result<Identifier>
}