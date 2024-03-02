package org.microg.gms.location.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import org.microg.gms.location.core.R

const val EXTRA_MESSENGER = "messenger"
private const val REQUEST_CODE_LOCATION = 120
class GoogleLocationSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 如果位置服务未启用，向用户显示一个对话框，提示用户打开位置设置
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.gps_open_hint)
            .setCancelable(false)
            .setPositiveButton(R.string.go_gps_settings) { dialog, id -> // 打开位置设置页面
                val intent =
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, REQUEST_CODE_LOCATION)
            }
            .setNegativeButton(
                R.string.gps_open_no
            ) { dialog, id ->
                sendReply()
                dialog.cancel()
                finish()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun sendReply(code: Int = RESULT_OK, extras: Bundle = Bundle.EMPTY) {
        intent?.getParcelableExtra<Messenger>(EXTRA_MESSENGER)?.let {
            runCatching {
                it.send(Message.obtain().apply {
                    what = code
                    data = extras
                })
            }
        }
        setResult(code, Intent().apply { putExtras(extras) })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_LOCATION) {
            sendReply()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
        finish()
    }
}