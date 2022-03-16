package io.arcblock.walletkit.bip44;

import com.google.common.io.BaseEncoding;

import org.bitcoinj.core.Base58;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.wallet.DeterministicSeed;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

import java.security.SecureRandom;
import java.util.List;

import io.arcblock.walletkit.hash.ArcKeccakf1600Hasher;

/**
 * Author       :paperhuang
 * Time         :2019/1/3
 * Edited By    :
 * Edited Time  :
 **/
public class Bip44Utils {

  public static String ETH_JAXX_TYPE = "m/44'/60'/0'/0/0";
  public static String ETH_LEDGER_TYPE = "m/44'/60'/0'/0";
  public static String ETH_CUSTOM_TYPE = "m/44'/60'/1'/0/0";

  private static final SecureRandom secureRandom = SecureRandomUtils.secureRandom();

  /**
   * It takes the secret code,
   * recover code, and passphrase and uses them to create a deterministic seed
   *
   * @param secretCode The secret code that was used to generate the seed.
   * @param recoverCode The code that was used to generate the seed.
   * @param passphrase The passphrase you used to encrypt your wallet.
   * @return The DeterministicSeed object.
   */
  public static DeterministicSeed genSeed(String secretCode, String recoverCode,
                                          String passphrase) {
    long creationTimeSeconds = System.currentTimeMillis() / 1000;
    return new DeterministicSeed(getEntropy(secretCode, recoverCode), passphrase,
      creationTimeSeconds);
  }

  /**
   * Generate a deterministic seed from a list of mnemonics
   *
   * @param list The list of mnemonic words.
   * @return The DeterministicSeed object.
   */
  public static DeterministicSeed genSeedByInsertMnemonics(List<String> list) {
    String passphrase = "";
    long creationTimeSeconds = System.currentTimeMillis() / 1000;
    return new DeterministicSeed(list, null, passphrase, creationTimeSeconds);
  }

  /**
   * Generate a list of mnemonic codes from a seed
   *
   * @return A list of mnemonic codes.
   */
  public static List<String> genMnemonicCodes() {
    String passphrase = "";
    long creationTimeSeconds = System.currentTimeMillis() / 1000;
    DeterministicSeed ds = new DeterministicSeed(secureRandom, 128, passphrase);
    return ds.getMnemonicCode();
  }

  /**
   * It generates a random string of bytes and encodes them using Base58
   *
   * @return a string that is the base58 encoding of the random bytes.
   */
  public static String genRecoverCode() {
    byte[] seed = new byte[16];
    secureRandom.nextBytes(seed);
    return Base58.encode(seed);
  }

  /**
   * It takes the user's secret code and the recovery code, and returns the entropy
   *
   * @param secretCode The secret code that the user entered.
   * @param recoverCode The code that was entered by the user.
   * @return The first 32 bytes of the SHA3-256 hash of the SHA3-256 hash of the user's secret key and the seed.
   */
  private static byte[] getEntropy(String secretCode, String recoverCode) {
    // SHA3(SHA3(user_entered_key) + seed)
    String result = BaseEncoding.base16()
      .encode(ArcKeccakf1600Hasher.sha256(
        (BaseEncoding.base16().encode(ArcKeccakf1600Hasher.sha256(secretCode.getBytes(), 1))
          + recoverCode).getBytes(), 1));

    return result.substring(0, 32).getBytes();
  }

  /**
   * It takes the user's secret code and the recovery code, and returns the entropy
   *
   * @param seed The seed is the user's secret code.
   * @return The key pair.
   */
  public static ECKeyPair genKeyPair(DeterministicSeed seed) {
    return genKeyPair(seed.getSeedBytes());
  }


  /**
   * It takes a string of the form `m/44'/60'/0'/0/0` and generates a key pair from the seed
   *
   * @param seeds The seed is the root key of the HD wallet.
   * @return The private key in the form of a byte array.
   */
  /**
   * It takes a string of the form `m/44'/60'/0'/0/0` and generates a key pair from the seed
   *
   * @param seeds The seed is the root key of the wallet. It is a random string of bytes.
   * @return The private key in the form of a byte array.
   */
  public static ECKeyPair genKeyPair(byte[] seeds) {
    String[] pathArray = ETH_JAXX_TYPE.split("/");
    DeterministicKey dkKey = HDKeyDerivation.createMasterPrivateKey(seeds);
    for (int i = 1; i < pathArray.length; i++) {
      ChildNumber childNumber;
      if (pathArray[i].endsWith("'")) {
        int number = Integer.parseInt(pathArray[i].substring(0,
          pathArray[i].length() - 1));
        childNumber = new ChildNumber(number, true);
      } else {
        int number = Integer.parseInt(pathArray[i]);
        childNumber = new ChildNumber(number, false);
      }
      dkKey = HDKeyDerivation.deriveChildKey(dkKey, childNumber);
    }
    ECKeyPair keyPair = ECKeyPair.create(dkKey.getPrivKeyBytes());

    return keyPair;
  }


  /**
   * Convert a list of mnemonics into a single string
   *
   * @param mnemonics A list of mnemonics.
   * @return The string "a b c d e f g h i j k l m n o p q r s t u v w x y z".
   */
  public static String convertMnemonicList(List<String> mnemonics) {
    StringBuilder sb = new StringBuilder();
    for (String mnemonic : mnemonics
    ) {
      sb.append(mnemonic);
      sb.append(" ");
    }
    return sb.toString();
  }

  /**
   * get EcKeyPair use privateKey
   *
   * @param privateKey private key
   * @return todo
   */
  public static void loadWalletByPrivateKey(String privateKey) {
    Credentials credentials = null;
    ECKeyPair ecKeyPair = ECKeyPair.create(Numeric.toBigInt(privateKey));
  }

  /**
   * recovery eckey by mnemonics
   *
   * @param path m/44/60/0/0/0
   * @param list mnemonics
   */
  public static ECKeyPair importMnemonic(String path, List<String> list) {
    if (!path.startsWith("m") && !path.startsWith("M")) {
      return null;
    }
    String[] pathArray = path.split("/");
    if (pathArray.length <= 1) {
      return null;
    }
    String passphrase = "";
    long creationTimeSeconds = System.currentTimeMillis() / 1000;
    DeterministicSeed ds = new DeterministicSeed(list, null, passphrase, creationTimeSeconds);
    return genKeyPair(ds);
  }
}
