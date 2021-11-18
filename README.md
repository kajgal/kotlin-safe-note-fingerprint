# Information about the project

#### Project name: Safe Note
#### Technologies: Kotlin
#### Creation time: Fifth semester of computer science studies
#### Purpose of creation: security of mobile systems classes
#### Author(s): Kajetan Gałęziowski 

# Preview and description

## Preview:

![Main screen](./img/main.jpg)

![Change password](./img/changePassword.jpg)

![Auth](./img/auth.jpg)

![Device auth](./img/deviceAuth.jpg)

![Recaptcha auth](./img/recaptchaAuth.jpg)

![Password auth](./img/passwordAuth.jpg)

![Note](./img/note.jpg)

![Note hidden](./img/noteHidden.jpg)

## Description:

#### Architecture: Model - View - ViewModel

#### Hashing function: Bcrypt
- salted by default
- the salt space is large enough to mitigate precomputation attacks, such as rainbow tables
- it has an adaptable cost.

#### KeyStore:
- setBlockModes(KeyProperties.BLOCK_MODE_GCM) cipher mode GCM
- setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE) no padding
- setKeySize(256)
- setUserAuthenticationRequired(true)
- setUserAuthenticationValidityDurationSeconds(60)
- setIsStrongBoxBacked(true) - usage TEE is available (secured hardware)
- setUnlockedDeviceRequired(true)

#### Implemented security:
- password hashed by Bcrypt (used kotlin implementation https://github.com/patrickfav/bcrypt) and stored in SharedPreferences
- note encrypted with AES with usage of KeyStore key and stores in EncryptedSharedPreferences
- verification whether device is rooted or not (used koltin implementation https://github.com/scottyab/rootbeer)
- checking access to device by system password prompt (including biometric)
- recaptcha verifcation
- limited time for authorization (timeout to main screen if count down finishes)
- app is not allowed to work in background (minimazing app = getting app closed)
- app is straight forward which means that you can't go back (back press = redirection to main screen)
- all data is fully encrypted/hashed in sharedPreferences, no plainText
- strong password required to use application with verification of it's strength
- manual protection - you need to know how to display message in note screen
