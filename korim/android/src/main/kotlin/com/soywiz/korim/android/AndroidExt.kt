package com.soywiz.korim.android

import android.app.AlertDialog
import android.app.Dialog
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korio.android.KorioAndroidContext
import com.soywiz.korio.async.async
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.coroutine.korioSuspendCoroutine

suspend fun androidShowImage(bitmap: Bitmap): Unit = korioSuspendCoroutine { c ->
	async(c.context) {
		executeInWorker {
			val ctx = KorioAndroidContext
			val androidBitmap = bitmap.toAndroidBitmap()
			ctx.runOnUiThread {
				val settingsDialog = Dialog(ctx)
				settingsDialog.window.requestFeature(Window.FEATURE_NO_TITLE)
				val rlmain = LinearLayout(ctx)
				val llp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT)
				val ll1 = LinearLayout(ctx)

				val iv = ImageView(ctx)
				iv.setBackgroundColor(Colors.BLACK)
				iv.setImageBitmap(androidBitmap)
				val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
				iv.layoutParams = lp

				ll1.addView(iv)
				rlmain.addView(ll1)
				settingsDialog.setContentView(rlmain, llp)

				settingsDialog.setOnDismissListener {
					c.resume(Unit)
				}
				settingsDialog.show()
			}
		}
	}
}

suspend fun androidQuestionAlert(message: String, title: String = "Warning"): Boolean = korioSuspendCoroutine { c ->
	KorioAndroidContext.runOnUiThread {
		val dialog = AlertDialog.Builder(KorioAndroidContext)
			.setTitle(title)
			.setMessage(message)
			.setPositiveButton(android.R.string.yes) { dialog, which ->
				c.resume(true)
			}
			.setNegativeButton(android.R.string.no, android.content.DialogInterface.OnClickListener { dialog, which ->
				c.resume(false)
			})
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(false)
			.show()

		dialog.show()
	}
}