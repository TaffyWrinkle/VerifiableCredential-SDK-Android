/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
import ISubtleCrypto from './ISubtleCrypto'
import IKeyStore, { CryptoAlgorithm } from '../keyStore/IKeyStore';
import CryptoFactory from './CryptoFactory';
import CryptoHelpers from '../utilities/CryptoHelpers';
import PublicKey from '../keys/PublicKey';
import PrivateKey from '../keys/PrivateKey';
import { KeyType } from '../keys/KeyTypeFactory';
import PairwiseKey from '../keys/PairwiseKey';
import { SubtleCrypto } from 'webcrypto-core';
const clone = require('clone');

// Named curves
const CURVE_P256K = 'P-256K';
const CURVE_K256 = 'K-256';

/**
 * The class extends the @class SubtleCrypto with addtional methods.
 *  Adds methods to work with key references.
 *  Extends SubtleCrypto to work with JWK keys.
 */
export default class SubtleCryptoExtension extends SubtleCrypto implements ISubtleCrypto {
  private keyStore: IKeyStore;
  private cryptoFactory: CryptoFactory;

  constructor(cryptoFactory: CryptoFactory) {
    super();
    this.keyStore = cryptoFactory.keyStore;
    this.cryptoFactory = cryptoFactory;
  }

  /**
   * Generate a pairwise key for the algorithm
   * @param algorithm for the key
   * @param seedReference Reference to the seed
   * @param personaId Id for the persona
   * @param peerId Id for the peer
   * @param extractable True if key is exportable
   * @param keyops Key operations
   */
   public async generatePairwiseKey(algorithm: EcKeyGenParams | RsaHashedKeyGenParams, seedReference: string, personaId: string, peerId: string): Promise<PrivateKey> {
    const pairwiseKey = new PairwiseKey(this.cryptoFactory);
    return pairwiseKey.generatePairwiseKey(algorithm, seedReference, personaId, peerId);
   } 

  /**
   * Sign with a key referenced in the key store
   * @param algorithm used for signature
   * @param keyReference points to key in the key store
   * @param data to sign
   * @returns The signature in the requested algorithm
   */
  public async signByKeyStore(algorithm: CryptoAlgorithm, keyReference: string, data: BufferSource): Promise<ArrayBuffer> {
    const jwk: PrivateKey = await <Promise<PrivateKey>>this.keyStore.get(keyReference, false);
    const crypto: SubtleCrypto = CryptoHelpers.getSubtleCryptoForAlgorithm(this.cryptoFactory, algorithm);
    const keyImportAlgorithm = SubtleCryptoExtension.normalizeAlgorithm(CryptoHelpers.getKeyImportAlgorithm(algorithm, jwk));
    
    const key = await crypto.importKey('jwk', SubtleCryptoExtension.normalizeJwk(jwk), keyImportAlgorithm, true, ['sign']);
    return <PromiseLike<ArrayBuffer>>crypto.sign(jwk.kty === KeyType.EC ? <EcdsaParams>SubtleCryptoExtension.normalizeAlgorithm(algorithm): <RsaPssParams>algorithm, 
      key, 
      <ArrayBuffer>data);
  }
          
  /**
   * Verify with JWK.
   * @param algorithm used for verification
   * @param jwk Json web key used to verify
   * @param signature to verify
   * @param payload which was signed
   */
   public async verifyByJwk(algorithm: CryptoAlgorithm, jwk: JsonWebKey, signature: BufferSource, payload: BufferSource): Promise<boolean> {
    const crypto: SubtleCrypto = CryptoHelpers.getSubtleCryptoForAlgorithm(this.cryptoFactory, algorithm);
    const keyImportAlgorithm = SubtleCryptoExtension.normalizeAlgorithm(CryptoHelpers.getKeyImportAlgorithm(algorithm, jwk));
    
    const key = await crypto.importKey('jwk', SubtleCryptoExtension.normalizeJwk(jwk), keyImportAlgorithm, true, ['verify']);
    return crypto.verify(jwk.kty === KeyType.EC ? 
      <EcdsaParams>SubtleCryptoExtension.normalizeAlgorithm(algorithm): 
      <RsaPssParams>algorithm, key, signature, payload);
   }  
          
  /**
   * Decrypt with a key referenced in the key store.
   * The referenced key must be a jwk key.
   * @param algorithm used for signature
   * @param keyReference points to key in the key store
   * @param cipher to decrypt
   */
   public async decryptByKeyStore(algorithm: CryptoAlgorithm, keyReference: string, cipher: BufferSource): Promise<ArrayBuffer> {
    const jwk: PrivateKey = <PrivateKey> await this.keyStore.get(keyReference, false);
    const crypto: SubtleCrypto = CryptoHelpers.getSubtleCryptoForAlgorithm(this.cryptoFactory, algorithm);
    const keyImportAlgorithm = SubtleCryptoExtension.normalizeAlgorithm(CryptoHelpers.getKeyImportAlgorithm(algorithm, jwk));
    
    const key = await crypto.importKey('jwk', SubtleCryptoExtension.normalizeJwk(jwk), SubtleCryptoExtension.normalizeAlgorithm(keyImportAlgorithm), true, ['decrypt']);
    return crypto.decrypt(algorithm, key, cipher);
   }  
          
  /**
   * Decrypt with JWK.
   * @param algorithm used for decryption
   * @param jwk Json web key to decrypt
   * @param cipher to decrypt
   */
   public async decryptByJwk(algorithm: CryptoAlgorithm, jwk: JsonWebKey, cipher: BufferSource): Promise<ArrayBuffer> {
    const crypto: SubtleCrypto = CryptoHelpers.getSubtleCryptoForAlgorithm(this.cryptoFactory, algorithm);
    const keyImportAlgorithm = SubtleCryptoExtension.normalizeAlgorithm(CryptoHelpers.getKeyImportAlgorithm(algorithm, jwk));
    
    const key = await crypto.importKey('jwk', SubtleCryptoExtension.normalizeJwk(jwk), SubtleCryptoExtension.normalizeAlgorithm(keyImportAlgorithm), true, ['decrypt']);
    return crypto.decrypt(algorithm, key, cipher);
   }  

  /**
   * Encrypt with a jwk key referenced in the key store
   * @param algorithm used for encryption
   * @param jwk Json web key public key
   * @param data to encrypt
   */
  public async encryptByJwk(algorithm: CryptoAlgorithm, jwk: PublicKey | JsonWebKey, data: BufferSource): Promise<ArrayBuffer> {
    const keyImportAlgorithm = CryptoHelpers.getKeyImportAlgorithm(algorithm, jwk);
    const crypto: SubtleCrypto = CryptoHelpers.getSubtleCryptoForAlgorithm(this.cryptoFactory, algorithm);
    const key = await crypto.importKey('jwk', SubtleCryptoExtension.normalizeJwk(jwk), SubtleCryptoExtension.normalizeAlgorithm(keyImportAlgorithm), true, ['encrypt']);
    return await <PromiseLike<ArrayBuffer>>crypto.encrypt(algorithm, key, <ArrayBuffer>data);
  }        
  
  /**
   * Normalize the algorithm so it can be used by underlying crypto.
   * @param algorithm Algorithm to be normalized
   */
  public static normalizeAlgorithm (algorithm: any) {
    if (algorithm.namedCurve) {
      if (algorithm.namedCurve === CURVE_P256K) {
        const alg = clone(algorithm);
        alg.namedCurve = CURVE_K256;
        return alg;
      }
    }

    return algorithm;
  }

  /**
   * Normalize the JWK parameters so it can be used by underlying crypto.
   * @param jwk Json web key to be normalized
   */
  public static normalizeJwk (jwk: any) {
    if (jwk.crv) {
      if (jwk.crv === CURVE_P256K) {
        const clonedKey = clone(jwk);
        clonedKey.crv = CURVE_K256;
        return clonedKey;
      }
    }

    return jwk;
  }
}

 