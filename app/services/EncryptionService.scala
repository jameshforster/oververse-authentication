package services

import java.security.MessageDigest

import com.google.inject.Inject
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import models.{EncryptedString, EncryptedUserModel, UserModel}
import org.apache.commons.codec.binary.Hex
import play.api.Configuration


class EncryptionService @Inject()(configuration: Configuration) {

  private lazy val secret: String = configuration.getOptional[String]("play.http.secret.key").get
  private val sha = MessageDigest.getInstance("SHA-1")
  private val cipher = Cipher.getInstance("AES")
  private val secretKey = new SecretKeySpec(java.util.Arrays.copyOf(sha.digest(secret.getBytes()), 16), "AES")
  private val encoder = new Hex

  def encryptUser(userModel: UserModel): EncryptedUserModel = {
    EncryptedUserModel(
      userModel.username,
      encrypt(userModel.email),
      encrypt(userModel.password),
      userModel.authLevel,
      userModel.authToken.map(encrypt)
    )
  }

  def decryptUser(encryptedUserModel: EncryptedUserModel): UserModel = {
    UserModel(
      encryptedUserModel.username,
      decrypt(encryptedUserModel.email),
      decrypt(encryptedUserModel.password),
      encryptedUserModel.authLevel,
      encryptedUserModel.authToken.map(decrypt)
    )
  }

  private[services] def encrypt(data: String): EncryptedString = {
    val result = {
      cipher.init(Cipher.ENCRYPT_MODE, secretKey)
      cipher.doFinal(data.getBytes())
    }
    EncryptedString(new String(encoder.encode(result)))
  }

  private[services] def decrypt(encryptedString: EncryptedString): String = {
    val encryptedData = encoder.decode(encryptedString.data.getBytes())
    val result = {
      cipher.init(Cipher.DECRYPT_MODE, secretKey)
      cipher.doFinal(encryptedData)
    }

    new String(result)
  }
}
