package io.github.sds100.keymapper.Utils

import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.Activities.SettingsActivity
import io.github.sds100.keymapper.R
import org.jetbrains.anko.alert
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.okButton

/**
 * Created by sds100 on 01/10/2018.
 */
object RootUtils {
    /**
     * @return whether the command was executed successfully
     */
    fun executeRootCommand(command: String): Boolean {
        return Shell.run("su", "-c", command)
    }

    fun checkAppHasRootPermission(ctx: Context): Boolean {
        return ctx.defaultSharedPreferences.getBoolean(ctx.str(R.string.key_pref_allow_root_features), false)
    }

    fun promptForRootPermission(ctx: Context) {
        ctx.alert {
            titleResource = R.string.dialog_title_root_prompt
            messageResource = R.string.dialog_message_root_prompt
            iconResource = R.drawable.ic_warning_black_24dp
            okButton {
                ctx.startActivity(Intent(ctx, SettingsActivity::class.java))
                Shell.run("su")
            }
            cancelButton { dialog -> dialog.cancel() }
        }.show()
    }

    fun changeSecureSetting(name: String, value: String) {
        RootUtils.executeRootCommand("settings put secure $name $value")
    }
}