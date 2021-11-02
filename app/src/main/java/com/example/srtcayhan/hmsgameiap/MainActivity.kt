package com.example.srtcayhan.hmsgameiap

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.huawei.hmf.tasks.Task
import com.huawei.hms.common.ApiException
import com.huawei.hms.jos.JosApps
import com.huawei.hms.jos.games.Games
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import org.json.JSONException
import com.huawei.hms.support.hwid.result.HuaweiIdAuthResult
import android.text.TextUtils
import android.content.Intent
import android.util.Log
import android.view.View
import com.example.srtcayhan.hmsgameiap.databinding.ActivityMainBinding
import com.huawei.hms.jos.AppParams
import com.huawei.hms.support.account.request.AccountAuthParams
import org.json.JSONObject
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hms.jos.games.PlayersClient
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val TAG = "MainActivity"

    private var logTextView: TextView? = null

    lateinit var playersClient: PlayersClient

    lateinit var playerID: String

    lateinit var sessionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnInit.setOnClickListener { init() }
        binding.btnSignIn.setOnClickListener { signIn() }
        binding.btnGetPlayer.setOnClickListener { getGamePlayer() }
        binding.btnPlayerEventStart.setOnClickListener { timeReportStart() }
        binding.btnPlayerEventEnd.setOnClickListener { timeReportEnd() }
        binding.btnPlayerExtraInfo.setOnClickListener { getPlayerExtra() }
        logTextView = findViewById<View>(R.id.LogText) as TextView
        binding.btnIap.setOnClickListener {
            val intent = Intent(this, ProductActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * SDK initialization
     */
    private fun init() {
        val params = AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME
        val appsClient = JosApps.getJosAppsClient(this)
        val initTask: Task<Void> = appsClient.init(AppParams(params) {
            // Implement the game addiction prevention function, such as saving games and calling the account sign-out API.
        })
        initTask.addOnSuccessListener {
            showLog("init success")
        }
            .addOnFailureListener { e -> showLog("init failed, " + e.message) }
    }

    private fun signIn() {
        val authHuaweiIdTask =
            HuaweiIdAuthManager.getService(this, getHuaweiIdParams()).silentSignIn()
        authHuaweiIdTask.addOnSuccessListener { authHuaweiId ->
            Log.i(TAG, "silentsignIn success")
            showLog("sign in success")
            Log.i(TAG, "display:" + authHuaweiId.displayName)
            login()
        }.addOnFailureListener { e ->
            if (e is ApiException) {
                Log.i(TAG, "signIn failed:" + e.statusCode)
                Log.i(TAG, "start getSignInIntent")
                //                    Sign in explicitly. The sign-in result is obtained in onActivityResult.
                val service = HuaweiIdAuthManager.getService(this@MainActivity, getHuaweiIdParams())
                startActivityForResult(service.signInIntent, 6013)
            }
        }
    }


    private fun getHuaweiIdParams(): HuaweiIdAuthParams? {
        return HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME).setIdToken()
            .createParams()
    }

    fun login() {

        playersClient = Games.getPlayersClient(this)
        val playerTask = playersClient.currentPlayer
        playerTask.addOnSuccessListener { player ->
            playerID = player.getPlayerId()
            Log.i(TAG, "getPlayerInfo Success, player info: " + player.playerId)
        }.addOnFailureListener { e -> //  Failed to obtain player information.
            if (e is ApiException) {
                Log.e(
                    TAG,
                    "getPlayerInfo failed, status: " + e.statusCode
                )
            }
        }
    }

    fun getGamePlayer() {
        // Call the getPlayersClient method for initialization.
        val client = Games.getPlayersClient(this)
        // Obtain player information.
        val task = client.gamePlayer
        task.addOnSuccessListener { player ->
            val accessToken = player.accessToken
            val displayName = player.displayName
            val unionId = player.unionId
            val openId = player.openId
            showLog(
                "Access Token : $accessToken" +
                        " Display Name : $displayName" +
                        "Union Id : $unionId" +
                        "Open Id : $openId"
            )
            // The player information is successfully obtained. Your game is started after accessToken is verified.
        }.addOnFailureListener { e ->
            if (e is ApiException) {
                val result = "rtnCode:" + e.statusCode
                // Failed to obtain player information. Rectify the fault based on the result code.
            }
        }
    }

    // The game addiction prevention function is available only for users registered in the Chinese mainland.
    private fun timeReportStart() {
        val uid = UUID.randomUUID().toString()
        val task = playersClient.submitPlayerEvent(playerID, uid, "GAMEBEGIN")
        task.addOnSuccessListener(OnSuccessListener { jsonRequest ->
            sessionId = try {
                val data = JSONObject(jsonRequest)
                data.getString("transactionId")
            } catch (e: JSONException) {
                showLog("parse jsonArray meet json exception")
                return@OnSuccessListener
            }
            showLog("submitPlayerEvent traceId: $jsonRequest")
        }).addOnFailureListener { e ->
            if (e is ApiException) {
                val result = "rtnCode:" + e.statusCode
                showLog(result)
            }
        }
    }

    // The game addiction prevention function is available only for users registered in the Chinese mainland.
    private fun timeReportEnd() {
        val task = playersClient.submitPlayerEvent(playerID, sessionId, "GAMEEND")
        task.addOnSuccessListener { s -> showLog("submitPlayerEvent traceId: $s") }
            .addOnFailureListener { e ->
                if (e is ApiException) {
                    val result = "rtnCode:" + e.statusCode
                    showLog(result)
                }
            }
    }

    // The game addiction prevention function is available only for users registered in the Chinese mainland.
    private fun getPlayerExtra() {
        val task = playersClient.getPlayerExtraInfo(sessionId)
        task.addOnSuccessListener { extra ->
            if (extra != null) {
                showLog(
                    "IsRealName: " + extra.isRealName + ", IsAdult: " + extra.isAdult
                            + ", PlayerId: " + extra.playerId + ", PlayerDuration: " + extra.playerDuration
                )
            } else {
                showLog("Player extra info is empty.")
            }
        }.addOnFailureListener { e ->
            if (e is ApiException) {
                val result = "rtnCode:" + e.statusCode
                showLog(result)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 6013) {
            if (null == data) {
                showLog("signIn inetnt is null")
                return
            }
            val jsonSignInResult = data.getStringExtra("HUAWEIID_SIGNIN_RESULT")
            if (TextUtils.isEmpty(jsonSignInResult)) {
                showLog("signIn result is empty")
                return
            }
            try {
                val signInResult = HuaweiIdAuthResult().fromJson(jsonSignInResult)
                if (0 == signInResult.status.statusCode) {
                    showLog("signIn success.")
                    showLog("signIn result: " + signInResult.toJson())
                } else {
                    showLog("signIn failed: " + signInResult.status.statusCode)
                }
            } catch (var7: JSONException) {
                showLog("Failed to convert json from signInResult.")
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun showLog(log: String) {
        logTextView!!.text = "log:\n$log"
    }


}