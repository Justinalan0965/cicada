package app.cicada.autofill

import android.R
import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.util.Log
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import app.cicada.CicadaApplication
import app.cicada.data.credential.CredentialEntity
import app.cicada.data.credential.CredentialRepository
import app.cicada.data.database.CicadaDB
import app.cicada.data.preferences.UserPreferencesRepository
import app.cicada.security.CryptoManager
import app.cicada.security.VaultSession
import app.cicada.util.AutofillDomainUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CicadaAutofillService : AutofillService() {

    companion object {
        private const val TAG = "CicadaAutofill"
    }

    private lateinit var db: CicadaDB
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    private lateinit var vaultSession: VaultSession

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        db = CicadaDB.getInstance(applicationContext)

        userPreferencesRepository = UserPreferencesRepository(applicationContext)

        vaultSession = (applicationContext as CicadaApplication).vaultSession

    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts
            .lastOrNull()
            ?.structure

        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        AutofillParser.dumpStructure(structure)

        val parsed = AutofillParser.parse(structure)

        Log.d(
            TAG,
            "Parsed request: package=${parsed.packageName}, " +
                    "activity=${parsed.activityName}, " +
                    "domain=${parsed.webDomain}, " +
                    "usernameId=${parsed.usernameId != null}, " +
                    "passwordId=${parsed.passwordId != null}"
        )

        Log.d(
            TAG,
            "Checking parsed fields: usernameId=${parsed.usernameId}, " +
                    "passwordId=${parsed.passwordId}"
        )

        Log.d(TAG, "Autofill request received")

        if (
            parsed.usernameId == null &&
            parsed.passwordId == null
        ) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                val userId =
                    userPreferencesRepository.lastUserId.first()

                if (userId == null) {
                    Log.d(TAG, "No remembered Cicada user")
                    callback.onSuccess(null)
                    return@launch
                }

                Log.d(TAG, "Remembered user found")

                val autosaveEnabled = userPreferencesRepository.autofillAutosaveEnabled.first()

                val domain = AutofillDomainUtils.normalizeWebsite(parsed.webDomain)

                Log.d(TAG, "Package: ${parsed.packageName}")
                Log.d(TAG, "Activity: ${parsed.activityName}")
                Log.d(TAG, "Web domain: ${parsed.webDomain}")
                Log.d(TAG, "Username field found: ${parsed.usernameId != null}")
                Log.d(TAG, "Password field found: ${parsed.passwordId != null}")

                if (domain == null) {
                    Log.d(TAG, "No web domain available")
                    callback.onSuccess(null)
                    return@launch
                }

                val credentials =
                    db.credentialDAO()
                        .getCredentialsForDomain(
                            userId = userId,
                            websiteDomain = domain
                        )

                Log.d(
                    TAG,
                    "Encrypted credentials found: ${credentials.size}"
                )

                if (credentials.isEmpty()) {
                    Log.d(TAG, "No matching credential")

                    val saveInfo = if (autosaveEnabled) {
                        createSaveInfo(parsed)
                    } else {
                        null
                    }

                    val response = saveInfo?.let {
                        FillResponse.Builder()
                            .setSaveInfo(it)
                            .build()
                    }

                    callback.onSuccess(response)
                    return@launch
                }


                sendAuthenticatedResponse(
                    parsed = parsed,
                    credentials = credentials,
                    domain = domain,
                    autosaveEnabled,
                    callback = callback
                )

            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Failed to load credentials",
                    e
                )

                callback.onSuccess(null)
            }
        }
    }

    private fun sendAuthenticatedResponse(
        parsed: ParsedAutofillRequest,
        credentials: List<CredentialEntity>,
        domain: String,
        autosaveEnabled: Boolean,
        callback: FillCallback
    ) {
        val responseBuilder = FillResponse.Builder()

        credentials.forEach { credential ->

            val presentation = RemoteViews(
                packageName,
                android.R.layout.simple_list_item_1
            )

            val accountLabel = credential.username
                ?: "Unknown account"

            presentation.setTextViewText(
                android.R.id.text1,
                "Cicada • $accountLabel"
            )

            val intent = Intent(
                this,
                AutofillAuthActivity::class.java
            ).apply {
                putExtra(
                    AutofillAuthActivity.EXTRA_CREDENTIAL_ID,
                    credential.id
                )

                putExtra(
                    AutofillAuthActivity.EXTRA_USER_ID,
                    credential.userId
                )

                putExtra(
                    AutofillAuthActivity.EXTRA_USERNAME_ID,
                    parsed.usernameId
                )

                putExtra(
                    AutofillAuthActivity.EXTRA_PASSWORD_ID,
                    parsed.passwordId
                )

                putExtra(
                    AutofillAuthActivity.EXTRA_PACKAGE_NAME,
                    parsed.packageName
                )

                putExtra(
                    AutofillAuthActivity.EXTRA_WEB_DOMAIN,
                    domain
                )
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                credential.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

            val datasetBuilder =
                Dataset.Builder(presentation)

            // The initial authenticated dataset must contain at least one field.
            parsed.usernameId?.let { id ->
                datasetBuilder.setValue(
                    id,
                    AutofillValue.forText(""),
                    presentation
                )
            }

            parsed.passwordId?.let { id ->
                datasetBuilder.setValue(
                    id,
                    AutofillValue.forText(""),
                    presentation
                )
            }

            datasetBuilder.setAuthentication(
                pendingIntent.intentSender
            )

            responseBuilder.addDataset(
                datasetBuilder.build()
            )
        }

        if (autosaveEnabled) {
            createSaveInfo(parsed)?.let { saveInfo ->
                responseBuilder.setSaveInfo(saveInfo)
            }
        }

        callback.onSuccess(
            responseBuilder.build()
        )
    }

    private fun sendTestResponse(
        parsed: ParsedAutofillRequest,
        callback: FillCallback
    ) {
        val presentation = RemoteViews(
            packageName,
            R.layout.simple_list_item_1
        )

        presentation.setTextViewText(
            R.id.text1,
            "Cicada Test Credential"
        )

        val datasetBuilder =
            Dataset.Builder(presentation)

        parsed.usernameId?.let { id ->
            datasetBuilder.setValue(
                id,
                AutofillValue.forText("test_user"),
                presentation
            )
        }

        parsed.passwordId?.let { id ->
            datasetBuilder.setValue(
                id,
                AutofillValue.forText("test_password"),
                presentation
            )
        }

        val response =
            FillResponse.Builder()
                .addDataset(datasetBuilder.build())
                .build()

        callback.onSuccess(response)
    }

    private fun createSaveInfo(
        parsed: ParsedAutofillRequest
    ): SaveInfo? {
        // Don't offer to save unless a password field was identified.
        val passwordId = parsed.passwordId ?: return null

        val usernameId = parsed.usernameId

        val saveType = if (usernameId != null) {
            SaveInfo.SAVE_DATA_TYPE_USERNAME or
                    SaveInfo.SAVE_DATA_TYPE_PASSWORD
        } else {
            SaveInfo.SAVE_DATA_TYPE_PASSWORD
        }

        val requiredIds = if (usernameId != null) {
            arrayOf(usernameId, passwordId)
        } else {
            arrayOf(passwordId)
        }

        return SaveInfo.Builder(saveType, requiredIds).build()
    }


    override fun onSaveRequest(
        request: SaveRequest,
        callback: SaveCallback
    ) {
        serviceScope.launch {
            try {
                val autosaveEnabled =
                    userPreferencesRepository.autofillAutosaveEnabled.first()

                if (!autosaveEnabled) {
                    callback.onFailure(
                        "Credential saving is disabled in Cicada settings."
                    )
                    return@launch
                }

                val structure = request.fillContexts
                    .lastOrNull()
                    ?.structure

                if (structure == null) {
                    callback.onFailure(
                        "Could not read the submitted login form."
                    )
                    return@launch
                }

                val parsed = AutofillParser.parse(structure)

                val domain =
                    AutofillDomainUtils.normalizeWebsite(parsed.webDomain)

                if (domain == null) {
                    callback.onFailure(
                        "Cicada couldn't verify the website domain."
                    )
                    return@launch
                }

                val passwordId = parsed.passwordId
                if (passwordId == null) {
                    callback.onFailure(
                        "Could not identify the password field."
                    )
                    return@launch
                }

                val password = findTextValue(structure, passwordId)
                if (password.isNullOrBlank()) {
                    callback.onFailure(
                        "Could not read the submitted password."
                    )
                    return@launch
                }

                val username = parsed.usernameId
                    ?.let { findTextValue(structure, it) }
                    .orEmpty()

                val userId =
                    userPreferencesRepository.lastUserId.first()

                if (userId == null) {
                    callback.onFailure(
                        "Open Cicada and unlock the account before saving."
                    )
                    return@launch
                }

                // Expire stale sessions before deciding whether to authenticate.
                if (vaultSession.shouldAutoLock()) {
                    vaultSession.lock()
                }

                val correctVaultAlreadyUnlocked =
                    vaultSession.isUnlocked &&
                            runCatching {
                                vaultSession.getUserId() == userId
                            }.getOrDefault(false)

                if (
                    vaultSession.isUnlocked &&
                    !correctVaultAlreadyUnlocked
                ) {
                    vaultSession.lock()
                }

                AutofillSaveCoordinator.begin(
                    userId = userId,
                    domain = domain,
                    username = username,
                    password = password,
                    callback = callback
                )

                if (correctVaultAlreadyUnlocked) {
                    AutofillSaveCoordinator.complete(
                        context = applicationContext,
                        vaultSession = vaultSession
                    )
                } else {
                    try {
                        startActivity(
                            Intent(
                                this@CicadaAutofillService,
                                AutofillSaveAuthActivity::class.java
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra(
                                    AutofillSaveAuthActivity.EXTRA_USER_ID,
                                    userId
                                )
                            }
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not open save authentication", e)
                        AutofillSaveCoordinator.fail(
                            "Cicada couldn't open authentication. Please unlock the app and try again."
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Autofill save request failed", e)
                callback.onFailure(
                    "Cicada couldn't save this login. Please try again."
                )
            }
        }
    }

    private fun findTextValue(
        structure: AssistStructure,
        targetId: AutofillId
    ): String? {
        for (windowIndex in 0 until structure.windowNodeCount) {
            val root = structure
                .getWindowNodeAt(windowIndex)
                .rootViewNode

            val value = findTextValue(root, targetId)

            if (value != null) {
                return value
            }
        }

        return null
    }

    private fun findTextValue(
        node: AssistStructure.ViewNode,
        targetId: AutofillId
    ): String? {
        if (node.autofillId == targetId) {
            val value = node.autofillValue

            return if (value?.isText == true) {
                value.textValue?.toString()
            } else {
                null
            }
        }

        for (index in 0 until node.childCount) {
            val value = findTextValue(
                node.getChildAt(index),
                targetId
            )

            if (value != null) {
                return value
            }
        }

        return null
    }
}