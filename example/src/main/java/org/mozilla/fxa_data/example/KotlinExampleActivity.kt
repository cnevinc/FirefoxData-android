/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.container_explanation.*
import kotlinx.android.synthetic.main.container_loading.*
import kotlinx.android.synthetic.main.container_show_fx_data.*
import org.mozilla.fxa_data.FirefoxData
import org.mozilla.fxa_data.FirefoxDataException
import org.mozilla.fxa_data.download.FirefoxDataClient
import org.mozilla.fxa_data.download.FirefoxDataResult
import org.mozilla.fxa_data.download.HistoryRecord
import org.mozilla.fxa_data.login.FirefoxDataLoginManager
import java.lang.Exception
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

/**
 * An example Activity that which displays a user's history in a [RecyclerView] after they log in.
 * If they're already logged in, the data will just be shown. Once the data is shown, the user can click
 * "Sign out" to sign out.

 * This Activity will switch between four states:
 * - Prompt for sign in
 * - Loading
 * - Error logging in or fetching Firefox data
 * - Display user's history.

 * This is a fairly realistic use case. For a simpler example, see [SimpleExampleActivity].
 */
class KotlinExampleActivity : AppCompatActivity() {

    private enum class UIState {
        SIGN_IN_PROMPT, LOADING, ERROR, SHOW_DATA
    }

    // data store
    var exception: Exception? = null
    private var state: UIState by Delegates.observable(UIState.SIGN_IN_PROMPT) { _, _, newValue ->
        runOnUiThread {
            // Reset UI state by removing all mutations we make.
            for (container in containerViews) {
                container?.visibility = View.GONE // we'll unhide one of these.
            }

            explanation_button.isEnabled = true

            when (newValue) {
                UIState.SIGN_IN_PROMPT -> {
                    explanation_button.isEnabled = true
                    container_explanation.visibility = View.VISIBLE
                    setExplanationUIResources(R.string.sign_in_explanation.toResString(this),
                            R.string.sign_in_button.toResString(this),
                            signInAction)
                }

                UIState.ERROR -> {
                    explanation_button.isEnabled = true
                    container_explanation.visibility = View.VISIBLE
                    setExplanationUIResources(R.string.error_explanation.toResString(this, exception?.toString() ?: "Unknown"),
                            R.string.error_button.toResString(this),
                            signOutAction)
                }
                KotlinExampleActivity.UIState.LOADING -> {
                    container_loading.visibility = View.VISIBLE
                }
                KotlinExampleActivity.UIState.SHOW_DATA -> {
                    container_fx_data.visibility = View.VISIBLE
                }
            }
        }
    }

    private lateinit var loginManager: FirefoxDataLoginManager
    private lateinit var historyAdapter: FirefoxHistoryAdapter

    private var isWaitingForCallback = false

    private val containerViews by lazy {
        listOf(container_explanation,
                container_loading,
                container_fx_data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firefox_data_in_recycler_view_example)

        supportActionBar?.title = KotlinExampleActivity::class.simpleName

        initFirefoxDataView()

        // Keep a reference to the login manager - it's the entry point for Firefox data.
        loginManager = FirefoxData.getLoginManager(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data == null) {
            isWaitingForCallback = false
            updateUI(UIState.SIGN_IN_PROMPT)
            return
        }
        // Required callback.
        loginManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        if (isWaitingForCallback) {
            updateUI(UIState.LOADING)
        } else if (!loginManager.isSignedIn) {
            updateUI(UIState.SIGN_IN_PROMPT)
        } else {
            fetchFirefoxDataAndUpdateUI()
        }
    }

    private fun fetchFirefoxDataAndUpdateUI() {
        isWaitingForCallback = true
        updateUI(UIState.LOADING)
        loginManager.loadStoredAccount(LoginCallback(this))
    }

    // This Kotlin nested class without inner key
    // word doesn't keep a reference to outer class, which won't cause memory leaks.
    private class LoginCallback(activity: KotlinExampleActivity) : FirefoxDataLoginManager.LoginCallback {
        private val activityWeakReference = WeakReference(activity)

        override fun onSuccess(dataClient: FirefoxDataClient) {
            val activity = activityWeakReference.get() ?: return

            activity.isWaitingForCallback = false
            val result: FirefoxDataResult<List<HistoryRecord>>
            try {
                result = dataClient.allHistory
            } catch (e: FirefoxDataException) {
                activity.updateUI(UIState.ERROR, e)
                return
            }

            activity.historyAdapter.setHistoryRecords(result.result)
            activity.updateUI(UIState.SHOW_DATA)
        }

        override fun onFailure(e: FirefoxDataException) {
            val activity = activityWeakReference.get() ?: return

            activity.updateUI(UIState.ERROR, e)
        }

        override fun onUserCancel() {
            val activity = activityWeakReference.get() ?: return

            activity.isWaitingForCallback = false
            activity.updateUI(UIState.SIGN_IN_PROMPT)
        }
    }

    val signInAction = { v: View ->
        loginManager.promptLogin(this@KotlinExampleActivity,
                resources.getString(R.string.app_name),
                LoginCallback(this@KotlinExampleActivity))
        isWaitingForCallback = true
        updateUI(UIState.LOADING)
    }

    val signOutAction = { v: View ->
        loginManager.signOut()
        updateUI(UIState.SIGN_IN_PROMPT)
    }

    private fun updateUI(uiState: UIState, newException: Exception? = null) {
        exception = newException
        state = uiState
    }

    private fun setExplanationUIResources(explanation: String?, buttonText: String?, onButtonClick: (View) -> Unit) {
        explanation_text.text = explanation
        explanation_button.text = buttonText
        explanation_button.setOnClickListener(onButtonClick)
    }

    private fun initFirefoxDataView() {
        val orientation = RecyclerView.VERTICAL
        fx_data_list.layoutManager = LinearLayoutManager(this, orientation, false)
        fx_data_list.addItemDecoration(DividerItemDecoration(this, orientation))

        historyAdapter = FirefoxHistoryAdapter()
        fx_data_list.adapter = historyAdapter

        fx_data_sign_out_button.setOnClickListener(signOutAction)
    }

}

fun Int.toResString(context: Context): String? = context.getString(this)
fun Int.toResString(context: Context, vararg formatArgs: Any): String? = context.getString(this, formatArgs)

