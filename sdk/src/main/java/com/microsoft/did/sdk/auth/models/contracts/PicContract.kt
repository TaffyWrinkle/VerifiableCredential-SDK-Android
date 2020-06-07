/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package com.microsoft.did.sdk.auth.models.contracts

import com.microsoft.did.sdk.auth.models.contracts.display.DisplayContract
import kotlinx.serialization.Serializable

/**
 * A logical grouping of documents created by an issuer to enable the creation of a Portable Identity Card.
 * In the Portable Identity Card Service, there are four files that make up a contract:
 * schema, display, and input.
 */
@Serializable
data class PicContract(
    // unique identifier of the contract
    val id: String,

    // A subset of the model in the Rules file for client consumption. The input file must describe the set of inputs,
    // where to obtain the inputs and the endpoint to call to obtain a Verifiable Credential.
    val input: InputContract,

    // A user experience data file that describes how information in a Verifiable Credential may be displayed.
    val display: DisplayContract,

    // An optional structured data model used to describe the set of claims in a Verifiable Credential.
    val schema: SchemaContract? = null
)