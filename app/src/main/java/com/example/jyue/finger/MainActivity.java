package com.example.jyue.finger;

import android.app.KeyguardManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    KeyguardManager mKeyguardManager;
    FingerprintManager mFingerprintManager;
    KeyStore mKeyStore;
    KeyGenerator mKeyGenerator;
    Cipher mCipher;
    FingerprintAuthenticationDialogFragment mFragment;

    public static final String KEY_NAME = "my_key";
    private static final String DIALOG_FRAGMENT_TAG = "myFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mKeyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        mFingerprintManager = (FingerprintManager)getSystemService(FINGERPRINT_SERVICE);//FingerprintManager.class

        if (!mKeyguardManager.isKeyguardSecure()) {
            // Show a message that the user hasn't set up a fingerprint or lock screen.
            Toast.makeText(this,
                    "Secure lock screen hasn't set up.\n"
                            + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!mFingerprintManager.hasEnrolledFingerprints()) {
            // This happens when no fingerprints are registered.
            Toast.makeText(this,
                    "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
                    Toast.LENGTH_LONG).show();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (createKey()) {

            try {
                if (mKeyStore == null) {
                    mKeyStore = KeyStore.getInstance("AndroidKeyStore");
                    mKeyStore.load(null);
                }
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            }

            SecretKey key = null;
            try {
                key = (SecretKey) mKeyStore.getKey(KEY_NAME, null);
                mCipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
                mCipher.init(Cipher.ENCRYPT_MODE, key);
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }


            // Show the fingerprint dialog. The user has the option to use the fingerprint with
            // crypto, or you can fall back to using a server-side verified password.
            mFragment = new FingerprintAuthenticationDialogFragment();
            mFragment.setFingerprintManager(mFingerprintManager);
            mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
            mFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        }
    }

    public boolean createKey() {

        try {
            // 创建KeyGenerator对象
            mKeyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            mKeyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // 设置需要用户验证
                    .setUserAuthenticationRequired(true)                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            // 生成key
            mKeyGenerator.generateKey();
            return true;
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }
}
